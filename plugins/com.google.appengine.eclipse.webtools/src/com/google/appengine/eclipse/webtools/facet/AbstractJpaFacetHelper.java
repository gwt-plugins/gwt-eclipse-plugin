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
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jst.common.project.facet.core.internal.ClasspathUtil;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.osgi.service.prefs.BackingStoreException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public abstract class AbstractJpaFacetHelper {

  public interface PersistenceXmlUpdater {
    public void updateConnection(IJavaProject javaProject) throws IOException;
  }

  public static final String JDBC_DRIVER = "javax.persistence.jdbc.driver";
  public static final String JDBC_URL = "javax.persistence.jdbc.url";
  public static final String JDBC_USER = "javax.persistence.jdbc.user";
  public static final String JDBC_PASSWORD = "javax.persistence.jdbc.password";
  private static final String FACET_JPT_JPA = "jpt.jpa";
  private static final
      String GAE_CLOUD_SQL_DRIVER_CLASS = "com.google.cloud.sql.jdbc.internal.googleapi.GoogleApiDriver";
  private static final
      String GAE_CLOUD_SQL_DRIVER_CLASS_FIXED = "com.google.appengine.api.rdbms.AppEngineDriver";
  private static final String
      URL_REFRESH_TOKEN_REGEX = "oauth2RefreshToken=[^&]*&?";
  private static final String
      URL_ACCESS_TOKEN_REGEX = "oauth2AccessToken=[^&]*&?";
  private static final String URL_CLIENT_ID_REGEX = "oauth2ClientId=[^&]*&?";
  private static final String
      URL_CLIENT_SECRET_REGEX = "oauth2ClientSecret=[^&]*&?";

  public static String getFixedDriverClassName(String driverClassName) {
    // We can't use the driver used in the App Engine DTP connection.
    // This driver requires that google_sql.jar be present in WEB-INF/lib,
    // which in turn causes a bunch of warnings about App Engine restricted
    // stuff being used.
    // So check if the driver is set to the App Engine DTP driver, and if so,
    // use AppEngineDriver, which is part of the App Engine SDK.
    if (driverClassName.equals(GAE_CLOUD_SQL_DRIVER_CLASS))
      return GAE_CLOUD_SQL_DRIVER_CLASS_FIXED;
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
          BuilderUtilities.removeBuilderFromProject(
              javaProject.getProject(), GaeNature.CLASS_ENHANCER_BUILDER);
          GaeProjectProperties.setGaeDatanucleusEnabled(
              javaProject.getProject(), false);
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
          return StatusUtilities.newErrorStatus(
              e, AppEngineWtpPlugin.PLUGIN_ID);
        } catch (FileNotFoundException e) {
          // Log and continue
          AppEngineWtpPlugin.getLogger().logError(e);
          return StatusUtilities.newErrorStatus(
              e, AppEngineWtpPlugin.PLUGIN_ID);
        }
      }
    };
    // Lock on workspace root
    job.setRule(javaProject.getProject().getWorkspace().getRoot());
    job.schedule();
  }

  public static void jobUpdatePersistenceAndWebInf(
      final IJavaProject javaProject, final PersistenceXmlUpdater updater) {
    Job job = new WorkspaceJob("") {
        @Override
      public IStatus runInWorkspace(IProgressMonitor monitor) {
        try {
          updater.updateConnection(javaProject);
          copyJpaLibraryToWebInf(javaProject);
          return Status.OK_STATUS;
        } catch (CoreException e) {
          // Log and continue
          AppEngineWtpPlugin.getLogger().logError(e);
          return e.getStatus();
        } catch (IOException e) {
          // Log and continue
          AppEngineWtpPlugin.getLogger().logError(e);
          return StatusUtilities.newErrorStatus(
              e, AppEngineWtpPlugin.PLUGIN_ID);
        }
      }
    };
    // Lock on project
    job.setRule(javaProject.getProject());
    job.schedule();
  }

  private static void copyJpaLibraryToWebInf(IJavaProject javaProject)
      throws CoreException, FileNotFoundException {
    IProject project = javaProject.getProject();
    if (!WebAppUtilities.hasManagedWarOut(project)) {
      // Nothing to do if project war directory is not managed
      return;
    }

    // Get WEB-INF/lib folder (create if it doesn't exist)
    IFolder webInfLibFolder = WebAppUtilities.getWebInfLib(project);
    ResourceUtils.createFolderStructure(
        project, webInfLibFolder.getProjectRelativePath());

    // Copy jars to WEB-INF/lib
    List<IClasspathEntry> cpes = ClasspathUtil.getClasspathEntries(
        project, ProjectFacetsManager.getProjectFacet(FACET_JPT_JPA));
    for (IClasspathEntry cpe : cpes) {
      for (IPackageFragmentRoot fragment :
          javaProject.findPackageFragmentRoots(cpe)) {
        File srcFile = fragment.getPath().toFile();
        IFile destFile = webInfLibFolder.getFile(srcFile.getName());
        if (!destFile.exists()) {
          destFile.create(new FileInputStream(srcFile), true, null);
        }
      }
    }
  }
}
