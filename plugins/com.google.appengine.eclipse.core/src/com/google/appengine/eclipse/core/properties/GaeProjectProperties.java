/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.appengine.eclipse.core.properties;

import com.google.appengine.eclipse.core.AppEngineCorePlugin;
import com.google.appengine.eclipse.core.AppEngineCorePluginLog;
import com.google.gdt.eclipse.core.PropertiesUtilities;
import com.google.gdt.eclipse.core.StatusUtilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.osgi.service.prefs.BackingStoreException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Gets and sets GAE project properties.
 */
public final class GaeProjectProperties {

  public static final String PREFERENCES_JOB_NAME = "preferences job";

  private static final String ORM_ENHANCEMENT_INCLUSIONS = "ormEnhancementInclusions";

  private static final String VALIDATION_EXCLUSIONS = "validationExclusions";

  private static final String FILES_COPIED_TO_WEB_INF_LIB = "filesCopiedToWebInfLib";

  private static final String GAE_DEPLOY_DIALOG_SETTINGS = "gaeDeployDialogSettings";

  private static final String GAE_HRD_ENABLED = "gaeHrdEnabled";

  private static final String GAE_IS_USE_SDK_FROM_DEFAULT = "gaeIsEclipseDefaultInstPath";

  private static final String GAE_DATANUCLEUS_ENABLED = "gaeDatanucleusEnabled";

  private static final String GAE_DATANUCLEUS_VERSION = "gaeDatanucleusVersion";

  public static List<String> getFileNamesCopiedToWebInfLib(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    String rawPropVal = prefs.get(FILES_COPIED_TO_WEB_INF_LIB, null);
    if (rawPropVal == null || rawPropVal.length() == 0) {
      return Collections.emptyList();
    }

    return Arrays.asList(rawPropVal.split("\\|"));
  }

  public static boolean getGaeDatanucleusEnabled(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    // If the property is not present, default to true.
    return prefs.getBoolean(GAE_DATANUCLEUS_ENABLED, true);
  }

  /**
   * If the property is not present, this is an old project. Though the version of the libs is v1,
   * they were not copied from /lib/opt.
   *
   * @return Returns "" if the property is not set.
   */
  public static String getGaeDatanucleusVersion(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    return prefs.get(GAE_DATANUCLEUS_VERSION, "");
  }

  public static String getGaeDeployDialogSettings(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    return prefs.get(GAE_DEPLOY_DIALOG_SETTINGS, "");
  }

  public static boolean getGaeHrdEnabled(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    // If the property is not present, it means we're dealing with an
    // old/existing project - hence we default to false. New projects will
    // enable HRD upon creation.
    return prefs.getBoolean(GAE_HRD_ENABLED, false);
  }

  public static boolean getIsUseSdkFromDefault(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    Boolean isUseSdkFromDefault = prefs.getBoolean(GAE_IS_USE_SDK_FROM_DEFAULT, true);
    return isUseSdkFromDefault;
  }

  public static List<IPath> getOrmEnhancementInclusionPatterns(IProject project) {
    List<IPath> patterns = new ArrayList<IPath>();

    IEclipsePreferences prefs = getProjectProperties(project);
    String rawPropVal = prefs.get(ORM_ENHANCEMENT_INCLUSIONS, null);
    if (rawPropVal == null) {
      // If we haven't set this property yet, default to including all Java src
      IJavaProject javaProject = JavaCore.create(project);
      try {
        for (IPackageFragmentRoot pkgRoot : javaProject.getAllPackageFragmentRoots()) {
          if (pkgRoot.getKind() == IPackageFragmentRoot.K_SOURCE) {
            // Only include src roots in this project
            if (javaProject.equals(pkgRoot.getAncestor(IJavaElement.JAVA_PROJECT))) {
              // Get project-relative path to source root
              IPath pattern =
                  pkgRoot.getPath().removeFirstSegments(1).makeRelative().addTrailingSeparator();
              patterns.add(pattern);
            }
          }
        }
      } catch (JavaModelException e) {
        AppEngineCorePluginLog.logError(e);
      }
    } else {
      patterns = PropertiesUtilities.deserializePaths(rawPropVal);
    }

    return patterns;
  }

  public static List<IPath> getValidationExclusionPatterns(IProject project) {
    String rawPatterns = getProjectProperties(project).get(VALIDATION_EXCLUSIONS, null);
    return PropertiesUtilities.deserializePaths(rawPatterns);
  }

  /**
   * All functions which begin with "job" prefix starts a new Job with schedule IProject.
   * This solves the deadlock/hanging problem with Eclipse 3.7 synchronized flush. Work around till
   * 3.7 releases a new updated version with synchronized.
   * @param project
   * @param datanucleusEnabled
   */
  public static void jobSetGaeDatanucleusEnabled(final IProject project,
      final boolean datanucleusEnabled) {
    Job job = new WorkspaceJob(PREFERENCES_JOB_NAME) {
      @Override
      public IStatus runInWorkspace(IProgressMonitor monitor) {
        try {
          setGaeDatanucleusEnabled(project, datanucleusEnabled);
          return Status.OK_STATUS;
        } catch (BackingStoreException e) {
          return StatusUtilities.newErrorStatus(e, AppEngineCorePlugin.PLUGIN_ID);
        }
      }
    };
    startWorkspaceJob(job, project);
  }

  public static void jobSetGaeDatanucleusVersion(
      final IProject project, final String datanucleusVersion) {
    Job job = new WorkspaceJob(PREFERENCES_JOB_NAME) {
      @Override
      public IStatus runInWorkspace(IProgressMonitor monitor) {
        try {
          setGaeDatanucleusVersion(project, datanucleusVersion);
          return Status.OK_STATUS;
        } catch (BackingStoreException e) {
          return StatusUtilities.newErrorStatus(e, AppEngineCorePlugin.PLUGIN_ID);
        }
      }
    };
    startWorkspaceJob(job, project);
  }

  public static void jobSetGaeHrdEnabled(final IProject project, final boolean hrdEnabled) {
    Job job = new WorkspaceJob(PREFERENCES_JOB_NAME) {
      @Override
      public IStatus runInWorkspace(IProgressMonitor monitor) {
        try {
          setGaeHrdEnabled(project, hrdEnabled);
          return Status.OK_STATUS;
        } catch (BackingStoreException e) {
          return StatusUtilities.newErrorStatus(e, AppEngineCorePlugin.PLUGIN_ID);
        }
      }
    };
    startWorkspaceJob(job, project);
  }

  public static void jobSetIsUseSdkFromDefault(final IProject project,
      final boolean isUseSdkFromDefault) {
    Job job = new WorkspaceJob(PREFERENCES_JOB_NAME) {
      @Override
      public IStatus runInWorkspace(IProgressMonitor monitor) {
        try {
          setIsUseSdkFromDefault(project, isUseSdkFromDefault);
          return Status.OK_STATUS;
        } catch (BackingStoreException e) {
          return StatusUtilities.newErrorStatus(e, AppEngineCorePlugin.PLUGIN_ID);
        }
      }
    };
    startWorkspaceJob(job, project);
  }

  public static void setFileNamesCopiedToWebInfLib(IProject project,
      List<String> fileNamesCopiedToWebInfLib) throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    StringBuilder sb = new StringBuilder();
    boolean addPipe = false;
    Collections.sort(fileNamesCopiedToWebInfLib);
    for (String fileNameCopiedToWebInfLib : fileNamesCopiedToWebInfLib) {
      if (addPipe) {
        sb.append("|");
      } else {
        addPipe = true;
      }

      sb.append(fileNameCopiedToWebInfLib);
    }

    prefs.put(FILES_COPIED_TO_WEB_INF_LIB, sb.toString());
    prefs.flush();
  }

  public static void setFilesCopiedToWebInfLib(IProject project, List<File> filesCopiedToWebInfLib)
      throws BackingStoreException {
    setFileNamesCopiedToWebInfLib(project, toFileNames(filesCopiedToWebInfLib));
  }

  public static void setGaeDatanucleusEnabled(IProject project, boolean datanucleusEnabled)
      throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.putBoolean(GAE_DATANUCLEUS_ENABLED, datanucleusEnabled);
    prefs.flush();
  }

  public static void setGaeDatanucleusVersion(IProject project, String datanucleusVersion)
      throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.put(GAE_DATANUCLEUS_VERSION, datanucleusVersion);
    prefs.flush();
  }

  public static void setGaeDeployDialogSettings(IProject project, String settings)
      throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.put(GAE_DEPLOY_DIALOG_SETTINGS, settings);
    prefs.flush();
  }

  public static void setGaeHrdEnabled(IProject project, boolean hrdEnabled)
      throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.putBoolean(GAE_HRD_ENABLED, hrdEnabled);
    prefs.flush();
  }

  public static void setIsUseSdkFromDefault(IProject project, boolean isUseSdkFromDefault)
      throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.putBoolean(GAE_IS_USE_SDK_FROM_DEFAULT, isUseSdkFromDefault);
    prefs.flush();
  }

  public static void setOrmEnhancementInclusionPatterns(IProject project, List<IPath> patterns)
      throws BackingStoreException {
    assert (patterns != null);

    IEclipsePreferences prefs = getProjectProperties(project);
    String rawPropVal = PropertiesUtilities.serializePaths(patterns);
    prefs.put(ORM_ENHANCEMENT_INCLUSIONS, rawPropVal);
    prefs.flush();
  }

  public static void setValidationExclusionPatterns(IProject project, List<IPath> patterns)
      throws BackingStoreException {
    assert (patterns != null);

    IEclipsePreferences prefs = getProjectProperties(project);
    String rawPropVal = PropertiesUtilities.serializePaths(patterns);
    prefs.put(VALIDATION_EXCLUSIONS, rawPropVal);
    prefs.flush();
  }

  /**
   * Starts a workspace job with lock on the project.
   *
   * @param job
   * @param project
   */
  public static void startWorkspaceJob(Job job, IProject project) {
    job.setRule(project);
    job.schedule();
    try {
      job.join();
    } catch (InterruptedException e) {
      AppEngineCorePluginLog.logError(e);
    }
  }

  private static IEclipsePreferences getProjectProperties(IProject project) {
    IScopeContext projectScope = new ProjectScope(project);
    return projectScope.getNode(AppEngineCorePlugin.PLUGIN_ID);
  }

  private static List<String> toFileNames(List<File> files) {
    List<String> fileNames = new ArrayList<String>();
    for (File file : files) {
      fileNames.add(file.getName());
    }
    return fileNames;
  }

  private GaeProjectProperties() {
    // Not instantiable
  }
}
