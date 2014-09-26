/*******************************************************************************
 * Copyright 2012 Google Inc. All Rights Reserved.
 *
 *  All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.appengine.eclipse.webtools.facet;

import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.appengine.eclipse.core.properties.GaeProjectProperties;
import com.google.appengine.eclipse.core.sdk.AppEngineUpdateWebInfFolderCommand;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.appengine.eclipse.webtools.AppEngineWtpPlugin;
import com.google.gdt.eclipse.core.BuilderUtilities;
import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gdt.eclipse.core.WebAppUtilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jpt.jpa.core.JpaProject;
import org.eclipse.jpt.jpa.core.context.persistence.Persistence;
import org.eclipse.jpt.jpa.core.context.persistence.PersistenceUnit;
import org.eclipse.jpt.jpa.core.context.persistence.PersistenceUnitTransactionType;
import org.eclipse.jpt.jpa.db.ConnectionProfile;
import org.eclipse.jst.common.project.facet.core.internal.ClasspathUtil;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.prefs.BackingStoreException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * A helper class for working with JPA (Java Persistence API) facets.
 */
public class JpaFacetHelper {
  /**
   * The method of retrieving the {@link Persistence} object for has {@link JpaProject} has been
   * changing every release, so isolate the changes behind this interface.
   */
  interface PersistenceRetriever {
    public Persistence getPersistence(JpaProject jpaProject);
  }

  public static final String JDBC_DRIVER = "javax.persistence.jdbc.driver";
  public static final String JDBC_URL = "javax.persistence.jdbc.url";
  public static final String JDBC_USER = "javax.persistence.jdbc.user";
  public static final String JDBC_PASSWORD = "javax.persistence.jdbc.password";
  private static final String FACET_JPT_JPA = "jpt.jpa";
  private static final String GAE_CLOUD_SQL_DRIVER_CLASS =
      "com.google.cloud.sql.jdbc.internal.googleapi.GoogleApiDriver";
  private static final String GAE_CLOUD_SQL_DRIVER_CLASS_FIXED =
      "com.google.appengine.api.rdbms.AppEngineDriver";
  private static final String URL_REFRESH_TOKEN_REGEX = "oauth2RefreshToken=[^&]*&?";
  private static final String URL_ACCESS_TOKEN_REGEX = "oauth2AccessToken=[^&]*&?";
  private static final String URL_CLIENT_ID_REGEX = "oauth2ClientId=[^&]*&?";
  private static final String URL_CLIENT_SECRET_REGEX = "oauth2ClientSecret=[^&]*&?";
  private static final String EMPTY_PROVIDER = "";

  /**
   * Attempts to activate the JPA plug-ins needed for our JPA integration, returning {@code true} if
   * the loading and activation succeeded.
   */
  public static boolean activateJpaSupport() {
    /*
     * Initially this function only attempted to check that the declared dependencies of the
     * o.e.jpt.jpa.core plug-in were available. But in Eclipse 3.7 the o.e.jst.j2ee.web was required
     * at runtime but not declared in the dependencies. Since the WTP has a long history of
     * mis-specifying dependencies and there is no penalty for loading the bundle here (we will
     * immediately retrieve the JpaProjectManager class from this bundle if the bundle can be
     * loaded), just attempt to load and activate the plug-in rather than checking dependencies.
     */
    Bundle jptJpaCore = Platform.getBundle("org.eclipse.jpt.jpa.core");
    if (jptJpaCore.getState() != Bundle.ACTIVE) {
      try {
        Platform.getBundle("org.eclipse.jpt.jpa.core").start();
      } catch (BundleException e) {
        return false;
      }
    }
    // The o.e.jpt.jpa.core plug-in is active, so all of its dependencies were met.
    return true;
  }

  /**
   * Updates the JPA project's persistence connection.
   */
  public static void updateConnection(IJavaProject javaProject) throws IOException, CoreException {
    JpaProject jpaProject = (JpaProject) javaProject.getProject().getAdapter(JpaProject.class);
    if (jpaProject == null) {
      return;
    }
    ConnectionProfile conn = jpaProject.getConnectionProfile();
    if (conn == null) {
      // Nothing to do if there is no associated connection
      return;
    }
    Persistence persistence = getPersistence(jpaProject);
    PersistenceUnit pUnit;
    // Create a persistence unit if there isn't one
    if (persistence.getPersistenceUnitsSize() == 0) {
      pUnit = persistence.addPersistenceUnit();
      pUnit.setName(jpaProject.getName());
    } else {
      // Only one persistence unit
      pUnit = persistence.getPersistenceUnits().iterator().next();
    }

    // Use default persistence provider (This might have earlier been set some DataNucleus value).
    if (pUnit.getProvider() != null) {
      pUnit.setProvider(EMPTY_PROVIDER);
    }
    pUnit.setSpecifiedTransactionType(PersistenceUnitTransactionType.RESOURCE_LOCAL);
    if (conn.getDriverClassName() != null) {
      pUnit.setProperty(JDBC_DRIVER, getFixedDriverClassName(conn.getDriverClassName()));
    }
    if (conn.getURL() != null) {
      pUnit.setProperty(JDBC_URL, getFixedUrl(conn.getURL()));
    }
    if (conn.getUserName() != null) {
      pUnit.setProperty(JDBC_USER, conn.getUserName());
    }
    if (conn.getUserPassword() != null) {
      pUnit.setProperty(JDBC_PASSWORD, conn.getUserPassword());
    }

    jpaProject.getPersistenceXmlResource().save(Collections.EMPTY_MAP);
  }

  /**
   * Returns the {@link Persistence} object for the given {@link JpaProject}. API breakage in recent
   * WTP-JPA releases requires the use of reflection (blech!).
   */
  private static Persistence getPersistence(JpaProject jpaProject) throws CoreException {
    // On Eclipse 4.4+ retrieve the Persistence object via JpaProject.getContextRoot()
    Persistence persistence = (Persistence) invokeMethodSafe(jpaProject, "getContextRoot");
    if (persistence != null) {
      return persistence;
    }

    // On Eclipse 4.3 retrieve the Persistence object via JpaProject.getContextModelRoot()
    persistence = (Persistence) invokeMethodSafe(jpaProject, "getContextModelRoot");
    if (persistence != null) {
      return persistence;
    }

    // On Eclipse 4.2, retrieve the Persistence object via
    // JpaProject.getRootContextNode().getPersistenceXml().getPersistence()
    // Any failures to invoke this method chain will result in a CoreException being thrown.
    Object rootContextNode = invokeMethod(jpaProject, "getRootContextNode");
    Object persistenceXml = invokeMethod(rootContextNode, "getPersistenceXml");
    if (persistenceXml != null) {
      persistence = (Persistence) invokeMethod(persistenceXml, "getPersistence");
      if (persistence != null) {
        return persistence;
      }
    }

    // At this point we saw the API for retrieving a persistence object for Eclipse 4.2, but it was
    // not hooked up to an actual persistence object, so report the failure to retrieve it.
    throw new CoreException(AppEngineWtpPlugin.createErrorStatus(
        "Unable to retrieve a JPA project persistence object.", null));
  }

  private static Object invokeMethodSafe(Object object, String methodName) {
    try {
      Method method = object.getClass().getDeclaredMethod(methodName);
      if (method != null) {
        return method.invoke(object);
      }
    } catch (NoSuchMethodException | SecurityException | IllegalAccessException
        | IllegalArgumentException | InvocationTargetException e) {
      // fall through to return null
    }
    return null;
  }

  private static Object invokeMethod(Object object, String methodName) throws CoreException {
    try {
      Method method = object.getClass().getDeclaredMethod(methodName);
      if (method != null) {
        return method.invoke(object);
      }
    } catch (NoSuchMethodException | SecurityException | IllegalAccessException
        | IllegalArgumentException | InvocationTargetException e) {
      throw createCoreException(e);
    }
    throw createCoreException(null);
  }

  private static CoreException createCoreException(Throwable t) {
    return new CoreException(AppEngineWtpPlugin.createErrorStatus(
        "Unable to retrieve a JPA project persistence object. "
        + "Incompatible Eclipse/WTP-JPA version?", t));
  }

  public static String getFixedDriverClassName(String driverClassName) {
    // We can't use the driver used in the App Engine DTP connection.
    // This driver requires that google_sql.jar be present in WEB-INF/lib,
    // which in turn causes a bunch of warnings about App Engine restricted
    // stuff being used.
    // So check if the driver is set to the App Engine DTP driver, and if so,
    // use AppEngineDriver, which is part of the App Engine SDK.
    if (driverClassName.equals(GAE_CLOUD_SQL_DRIVER_CLASS)) {
      return GAE_CLOUD_SQL_DRIVER_CLASS_FIXED;
    }
    return driverClassName;
  }

  public static String getFixedUrl(String url) {
    // Remove refresh/access tokens, client id/secret
    return url.replaceAll(URL_REFRESH_TOKEN_REGEX, "")
        .replaceAll(URL_ACCESS_TOKEN_REGEX, "")
        .replaceAll(URL_CLIENT_ID_REGEX, "")
        .replaceAll(URL_CLIENT_SECRET_REGEX, "")
        // Remove trailing "?&" / "?"
        .replaceAll("\\?&?$", "");
  }

  public static void jobDisableDataNucleus(final IJavaProject javaProject) {
    Job job = new WorkspaceJob("") {
        @Override
      public IStatus runInWorkspace(IProgressMonitor monitor) {
        try {
          // RemoveBuilder internally runs a job locked on the workspace root
          // Locking our DisableDataNucleus job on this project results in:
          // java.lang.IllegalArgumentException: Attempted to beginRule: R/,
          // does not match outer scope rule: P/foo
          // Hence our job should also run locked on the workspace root
          BuilderUtilities.removeBuilderFromProject(javaProject.getProject(),
              GaeNature.CLASS_ENHANCER_BUILDER);
          GaeProjectProperties.setGaeDatanucleusEnabled(javaProject.getProject(), false);
          GaeSdk sdk = GaeSdk.findSdkFor(javaProject);
          (new AppEngineUpdateWebInfFolderCommand(javaProject, sdk)).execute();
          return Status.OK_STATUS;
        } catch (CoreException e) {
          // Log and continue
          AppEngineWtpPlugin.getLogger().logError(e);
          return e.getStatus();
        } catch (BackingStoreException e) {
          // Log and continue
          AppEngineWtpPlugin.getLogger().logError(e);
          return StatusUtilities.newErrorStatus(e, AppEngineWtpPlugin.PLUGIN_ID);
        } catch (FileNotFoundException e) {
          // Log and continue
          AppEngineWtpPlugin.getLogger().logError(e);
          return StatusUtilities.newErrorStatus(e, AppEngineWtpPlugin.PLUGIN_ID);
        }
      }
    };
    // Lock on workspace root
    job.setRule(javaProject.getProject().getWorkspace().getRoot());
    job.schedule();
  }

  public static void jobUpdatePersistenceAndWebInf(final IJavaProject javaProject) {
    Job job = new WorkspaceJob("") {
      @Override
      public IStatus runInWorkspace(IProgressMonitor monitor) {
        try {
          updateConnection(javaProject);
          copyJpaLibraryToWebInf(javaProject);
          return Status.OK_STATUS;
        } catch (CoreException e) {
          // Log and continue
          AppEngineWtpPlugin.getLogger().logError(e);
          return e.getStatus();
        } catch (IOException e) {
          // Log and continue
          AppEngineWtpPlugin.getLogger().logError(e);
          return StatusUtilities.newErrorStatus(e, AppEngineWtpPlugin.PLUGIN_ID);
        }
      }
    };
    // Lock on project
    job.setRule(javaProject.getProject());
    job.schedule();
  }

  private static void copyJpaLibraryToWebInf(IJavaProject javaProject) throws CoreException,
      FileNotFoundException {
    IProject project = javaProject.getProject();
    if (!WebAppUtilities.hasManagedWarOut(project)) {
      // Nothing to do if project war directory is not managed
      return;
    }

    // Get WEB-INF/lib folder (create if it doesn't exist)
    IFolder webInfLibFolder = WebAppUtilities.getWebInfLib(project);
    ResourceUtils.createFolderStructure(project, webInfLibFolder.getProjectRelativePath());

    // Copy jars to WEB-INF/lib
    List<IClasspathEntry> classpathEntries = ClasspathUtil.getClasspathEntries(project,
        ProjectFacetsManager.getProjectFacet(FACET_JPT_JPA));
    for (IClasspathEntry classpathEntry : classpathEntries) {
      for (IPackageFragmentRoot fragment : javaProject.findPackageFragmentRoots(classpathEntry)) {
        File srcFile = fragment.getPath().toFile();
        IFile destFile = webInfLibFolder.getFile(srcFile.getName());
        if (!destFile.exists()) {
          destFile.create(new FileInputStream(srcFile), true, null);
        }
      }
    }
  }
}
