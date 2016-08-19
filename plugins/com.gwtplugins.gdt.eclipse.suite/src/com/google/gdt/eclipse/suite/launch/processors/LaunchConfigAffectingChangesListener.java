/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.google.gdt.eclipse.suite.launch.processors;

import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.JavaProjectUtilities;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationUtilities;
import com.google.gdt.eclipse.core.properties.WebAppProjectProperties;
import com.google.gdt.eclipse.core.properties.WebAppProjectProperties.IWarOutLocationChangedListener;
import com.google.gdt.eclipse.suite.GdtPlugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.INodeChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.NodeChangeEvent;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.IPreferenceNodeVisitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Updates relevant launch configurations when a project's classpath or natures
 * change.
 */
public enum LaunchConfigAffectingChangesListener implements
    IElementChangedListener, IResourceChangeListener,
    ILaunchConfigurationListener, IWarOutLocationChangedListener,
    IPreferenceChangeListener, INodeChangeListener {
  INSTANCE;

  private static void asyncUpdate(
      final List<LaunchConfigurationUpdater> launchConfigurationUpdaters) {
    WorkspaceJob job = new WorkspaceJob("Updating GPE launch configurations") {

      @Override
      public IStatus runInWorkspace(IProgressMonitor monitor)
          throws CoreException {
        syncUpdate(launchConfigurationUpdaters);
        return Status.OK_STATUS;
      }
    };

    job.setSystem(true);
    // Use the build rule, since it locks the whole workspace
    job.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
    job.schedule();
  }

  private static void syncUpdate(
      final List<LaunchConfigurationUpdater> launchConfigurationUpdaters) {
    for (LaunchConfigurationUpdater updater : launchConfigurationUpdaters) {
      try {
        updater.update();
      } catch (CoreException ce) {
        CorePluginLog.logError(ce, "Could not update launch configuration "
            + updater.getLaunchConfiguration().getName() + " .");
      }
    }
  }

  public void added(NodeChangeEvent event) {
    ((IEclipsePreferences) event.getChild()).addPreferenceChangeListener(this);
    ((IEclipsePreferences) event.getChild()).addNodeChangeListener(this);
  }

  public void elementChanged(ElementChangedEvent event) {
    for (IJavaElementDelta delta : event.getDelta().getAffectedChildren()) {
      int flags = delta.getFlags();
      if ((flags & IJavaElementDelta.F_CLASSPATH_CHANGED) != 0) {
        IJavaElement element = delta.getElement();
        if (element.getElementType() != IJavaElement.JAVA_PROJECT) {
          continue;
        }

        updateLaunchConfigs((IJavaProject) element);
      }
    }
  }

  public void launchConfigurationAdded(ILaunchConfiguration configuration) {
    try {
      String id = configuration.getType().getIdentifier();
      if (id == null
          || !LaunchConfigurationUpdater.APPLICABLE_LAUNCH_CONFIGURATION_TYPE_IDS.contains(id)) {
        return;
      }
    } catch (CoreException e) {
      // Could not get the type
      GdtPlugin.getLogger().logWarning(
          e,
          "Not updating launch configuration after possible SDK/classpath change due to failure fetching its type");
      return;
    }

    try {
      IJavaProject javaProject = JavaRuntime.getJavaProject(configuration);
      if (javaProject == null || !javaProject.exists()) {
        return;
      }

      /*
       * Assumes that we have a lock on the launch configuration itself; this
       * should be called in response to saving a launch config working copy. We
       * need to do this synchronously, otherwise there's a chance that the
       * launch config could actually execute before the updaters have a chance
       * to run.
       */
      syncUpdate(Collections.singletonList(new LaunchConfigurationUpdater(
          configuration, javaProject)));
    } catch (CoreException e) {
      GdtPlugin.getLogger().logError(e,
          "Could not update newly added launch configuration");
    }
  }

  public void launchConfigurationChanged(ILaunchConfiguration configuration) {
  }

  public void launchConfigurationRemoved(ILaunchConfiguration configuration) {
  }

  public void preferenceChange(PreferenceChangeEvent event) {
    try {
      Preferences preferences = event.getNode().parent();
      if (preferences.parent().name() == ProjectScope.SCOPE) {
        IJavaProject javaProject = JavaProjectUtilities.findJavaProject(preferences.name());
        if (javaProject != null) {
          updateLaunchConfigs(javaProject);
        }
      }
    } catch (IllegalStateException e) {
      // Can happen if a project is deleted; ignore this.
    }
  }

  public void removed(NodeChangeEvent event) {
  }

  public void resourceChanged(IResourceChangeEvent event) {
    IResourceDelta rootDelta = event.getDelta();
    if (rootDelta == null) {
      return;
    }

    for (IResourceDelta delta : rootDelta.getAffectedChildren()) {
      // The description includes natures (see IProjectDescription)
      if (delta.getFlags() == IResourceDelta.DESCRIPTION) {
        IResource resource = delta.getResource();
        if (resource.getType() != IResource.PROJECT) {
          continue;
        }

        IJavaProject javaProject = JavaCore.create((IProject) resource);
        if (!javaProject.exists()) {
          continue;
        }

        updateLaunchConfigs(javaProject);
      }
    }
  }

  public void start() {
    ResourcesPlugin.getWorkspace().addResourceChangeListener(this,
        IResourceChangeEvent.POST_CHANGE);
    JavaCore.addElementChangedListener(this, ElementChangedEvent.POST_CHANGE);

    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    manager.addLaunchConfigurationListener(this);
    
    IEclipsePreferences projectScopeNode = ((IEclipsePreferences)
        Platform.getPreferencesService().getRootNode().node(ProjectScope.SCOPE));
    IPreferenceNodeVisitor visitor = new IPreferenceNodeVisitor() {
      public boolean visit(IEclipsePreferences node) {
        node.addNodeChangeListener(LaunchConfigAffectingChangesListener.this);
        node.addPreferenceChangeListener(LaunchConfigAffectingChangesListener.this);
        return true;
      }
    };
    try {
      projectScopeNode.accept(visitor);
    } catch (BackingStoreException e) {
      CorePluginLog.logError(e);
    }

    WebAppProjectProperties.addWarOutLocationChangedListener(this);
  }

  public void stop() {
    WebAppProjectProperties.removeWarOutLocationChangedListener(this);

    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    manager.removeLaunchConfigurationListener(this);

    JavaCore.removeElementChangedListener(this);
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
  }

  public void updateLaunchConfigurations(IProject project) {
    IJavaProject javaProject = JavaCore.create(project);
    if (javaProject.exists()) {
      updateLaunchConfigs(javaProject);
    }
  }

  public void warOutLocationChanged(IProject project) {
    updateLaunchConfigurations(project);
  }

  private void updateLaunchConfigs(IJavaProject javaProject) {
    try {
      List<ILaunchConfiguration> launchConfigs = LaunchConfigurationUtilities.getLaunchConfigurations(
          javaProject.getProject(),
          LaunchConfigurationUpdater.APPLICABLE_LAUNCH_CONFIGURATION_TYPE_IDS.toArray(new String[0]));
      ArrayList<LaunchConfigurationUpdater> launchConfigurationUpdaters = new ArrayList<LaunchConfigurationUpdater>();
      for (ILaunchConfiguration launchConfig : launchConfigs) {
        launchConfigurationUpdaters.add(new LaunchConfigurationUpdater(
            launchConfig, javaProject));
      }

      asyncUpdate(launchConfigurationUpdaters);
    } catch (CoreException e) {
      CorePluginLog.logError(
          e,
          "Could not update launch configuration after project nature or classpath change");
    }
  }

}
