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
package com.google.gdt.eclipse.suite;

import com.google.appengine.eclipse.core.markers.AppEngineProblemType;
import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.gdt.eclipse.core.AbstractGooglePlugin;
import com.google.gdt.eclipse.core.BuilderUtilities;
import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.Logger;
import com.google.gdt.eclipse.core.markers.GdtProblemSeverities;
import com.google.gdt.eclipse.core.markers.ProjectStructureOrSdkProblemType;
import com.google.gdt.eclipse.core.natures.NatureUtils;
import com.google.gdt.eclipse.core.projects.ProjectUtilities;
import com.google.gdt.eclipse.suite.launch.processors.LaunchConfigAffectingChangesListener;
import com.google.gdt.eclipse.suite.preferences.GdtPreferences;
import com.google.gdt.eclipse.suite.resources.GdtImages;
import com.google.gdt.eclipse.suite.wizards.WebAppProjectCreator;
import com.google.gwt.eclipse.core.markers.ClientBundleProblemType;
import com.google.gwt.eclipse.core.markers.GWTProblemType;
import com.google.gwt.eclipse.core.nature.GWTNature;
import com.google.gwt.eclipse.core.uibinder.problems.UiBinderTemplateProblemType;
import com.google.gwt.eclipse.core.uibinder.problems.java.UiBinderJavaProblemType;
import com.google.gwt.eclipse.core.validators.rpc.RemoteServiceProblemType;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PerspectiveAdapter;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.internal.WorkbenchPage;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Activator for the com.google.eclipse.gdt plugin.
 */
@SuppressWarnings({"deprecation", "restriction"})
public class GdtPlugin extends AbstractGooglePlugin {

  public static final String PLUGIN_ID = GdtPlugin.class.getPackage().getName();

  private static Logger logger;
  /**
   * Perspectives that we monitor and optionally add our new wizards to.
   */
  private static final List<String> PERSPECTIVES_TO_ADD_WIZARDS_TO = new ArrayList<String>(
      Arrays.asList(new String[]{
          "org.eclipse.jdt.ui.JavaPerspective",
          "org.eclipse.jdt.ui.JavaBrowsingPerspective",
          "org.eclipse.jdt.ui.JavaHierarchyPerspective",
          "org.eclipse.jst.j2ee.J2EEPerspective"}));

  private static GdtPlugin plugin;

  /**
   * Wizards that we add to perspectives.
   * 
   * TODO: If we add more wizards, this list should grow. Alternatively, we
   * could query the extension registry and add all GDT contributed wizards.
   */
  private static final List<String> WIZARDS_TO_ADD_TO_PERSPECTIVES = new ArrayList<String>(
      Arrays.asList(new String[]{
          "com.google.gdt.eclipse.suite.wizards.newProjectWizard",
          "com.google.gwt.eclipse.core.newModuleWizard",
          "com.google.gwt.eclipse.core.newHostPageWizard",
          "com.google.gwt.eclipse.core.newEntryPointWizard",
          "com.google.gwt.eclipse.core.newClientBundleWizard",
          "com.google.gwt.eclipse.core.newUiBinderWizard"}));

  public static GdtPlugin getDefault() {
    return plugin;
  }

  /**
   * Return the current eclipse version as a string.
   * 
   * @return current eclipse version as a string.
   */
  public static String getEclipseVersion() {
    return (String) ResourcesPlugin.getPlugin().getBundle().getHeaders().get(
        Constants.BUNDLE_VERSION);
  }

  public static String getInstallationId() {
    String id = GdtPreferences.getInstallationId();
    if (id == null) {
      // Use the current time in millis as the installation id and store it
      // back to the prefs.
      id = Long.toString(System.currentTimeMillis());
      GdtPreferences.setInstallationId(id);
    }

    return id;
  }

  public static Logger getLogger() {
    return logger;
  }

  public static Version getVersion() {
    return new Version((String) getDefault().getBundle().getHeaders().get(
        Constants.BUNDLE_VERSION));
  }

  private static void rebuildGoogleProjectIfPluginVersionChanged(
      IProject project) {
    try {
      // We're only worried about Google projects
      if (NatureUtils.hasNature(project, GWTNature.NATURE_ID)
          || NatureUtils.hasNature(project, GaeNature.NATURE_ID)) {
        // Find the last plugin version that know the project was built with
        Version lastForcedRebuildAt = GdtPreferences.getVersionForLastForcedRebuild(project);
        Version currentPluginVersion = GdtPlugin.getVersion();

        if (!lastForcedRebuildAt.equals(currentPluginVersion)) {
          GdtPreferences.setVersionForLastForcedRebuild(project,
              currentPluginVersion);

          BuilderUtilities.scheduleRebuild(project);
          CorePluginLog.logInfo("Scheduled rebuild of project "
              + project.getName()
              + " because of plugin update (current version: "
              + currentPluginVersion.toString() + ")");
        }
      }
    } catch (CoreException e) {
      CorePluginLog.logError(e);
    }
  }

  private static void rebuildGoogleProjectsIfPluginVersionChanged() {
    boolean closedProjectsInWorkspace = false;

    // Rebuild all (open) Google projects in the workspace
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    for (IProject project : workspace.getRoot().getProjects()) {
      if (project.isOpen()) {
        rebuildGoogleProjectIfPluginVersionChanged(project);
      } else {
        closedProjectsInWorkspace = true;
      }
    }

    // Add listeners for all closed projects, so we can rebuild them, too,
    // when they're opened (but only if they are Google projects).
    if (closedProjectsInWorkspace) {
      workspace.addResourceChangeListener(new IResourceChangeListener() {
        public void resourceChanged(IResourceChangeEvent event) {
          IResourceDelta delta = event.getDelta();
          if (delta != null) {
            // Find any project-level changes
            IResourceDelta[] projectDeltas = delta.getAffectedChildren(
                IResourceDelta.CHANGED, IResource.PROJECT);

            // The master delta may include more than one project delta
            for (IResourceDelta projectDelta : projectDeltas) {
              // Find any deltas for projects being opened/closed
              if ((projectDelta.getFlags() & IResourceDelta.OPEN) > 0) {
                IProject project = (IProject) projectDelta.getResource();
                if (project.isOpen()) {
                  rebuildGoogleProjectIfPluginVersionChanged(project);
                }
              }
            }
          }
        }
      });
    }
  }

  private final PerspectiveAdapter perspectiveListener = new PerspectiveAdapter() {
    @Override
    public void perspectiveActivated(IWorkbenchPage page,
        IPerspectiveDescriptor perspectiveDesc) {
      maybeAddNewWizardActionsToPerspective((WorkbenchPage) page,
          perspectiveDesc);
    }
  };

  private final IWindowListener windowListener = new IWindowListener() {
    public void windowActivated(IWorkbenchWindow window) {
    }

    public void windowClosed(IWorkbenchWindow window) {
    }

    public void windowDeactivated(IWorkbenchWindow window) {
    }

    public void windowOpened(IWorkbenchWindow window) {
      maybeAddNewWizardActionsToWindow(window);
    }
  };

  public GdtPlugin() {
  }

  @Override
  protected void initializeImageRegistry(ImageRegistry reg) {
    super.initializeImageRegistry(reg);

    reg.put(GdtImages.GDT_ICON, imageDescriptorFromPath("icons/gdt_16x16.png"));
    reg.put(GdtImages.GDT_NEW_PROJECT_ICON,
        imageDescriptorFromPath("icons/gdt-new-project_16x16.png"));
    reg.put(GdtImages.GDT_NEW_PROJECT_LARGE,
        imageDescriptorFromPath("icons/gdt-new-project_75x66.png"));
    reg.put(GdtImages.GAE_ICON, imageDescriptorFromPath("icons/ae_16x16.png"));
  }

  /**
   * If we haven't added them in the past, add the new wizard actions that this
   * to the perspective which is being displayed on the workbench page.
   * 
   * Note: This method can only be called once the workbench has been started.
   */
  private void maybeAddNewWizardActionsToPerspective(WorkbenchPage page,
      IPerspectiveDescriptor desc) {
    if (page == null || desc == null) {
      return;
    }
    
    if (PERSPECTIVES_TO_ADD_WIZARDS_TO.contains(desc.getId())) {
      // Perspective perspective = page.findPerspective(desc);
      // if (perspective != null) {
      // List<String> wizardsToAdd = new ArrayList<String>(
      // WIZARDS_TO_ADD_TO_PERSPECTIVES);
      //
      // // Ignore any wizards we've already tried to add to this perspective.
      // // That way we don't re-add any wizards that the user explicitly
      // // removed from the New shortcut menu.
      // List<String> wizardsAlreadyAdded = GdtPreferences
      // .getAddedNewWizardActionsForPerspective(desc.getId());
      // wizardsToAdd.removeAll(wizardsAlreadyAdded);
      //
      // // Get the current set of wizard shortcuts
      // List<String> currentWizardShortcuts = new ArrayList<String>(
      // Arrays.asList(perspective.getNewWizardShortcuts()));
      //
      // // Ignore wizards that already have shortcuts in this perspective
      // wizardsToAdd.removeAll(currentWizardShortcuts);
      //
      // // Only update the perspective if there are new wizards to add
      // if (!wizardsToAdd.isEmpty()) {
      // currentWizardShortcuts.addAll(wizardsToAdd);
      //
      // // Update the perspective
      // perspective.setNewWizardActionIds(new ArrayList<String>(
      // currentWizardShortcuts));
      // }
      //
      // // Remember the wizards that we've attempted to add to this perspective
      // GdtPreferences.setAddedNewWizardActionsForPerspective(desc.getId(),
      // WIZARDS_TO_ADD_TO_PERSPECTIVES);
      // } else {
      // assert false : "Perspective was activated but not found";
      // }
    }
  }

  /**
   * Adds the new wizards to the current perspective displayed in
   * <code>activeWorkbenchWindow</code>, if they've not been added already. Adds
   * listeners on the window so that the same is done whenever the user switches
   * perspectives in the window.
   * 
   * Note: This method can only be called once the workbench has been started.
   */
  private void maybeAddNewWizardActionsToWindow(
      IWorkbenchWindow activeWorkbenchWindow) {
    if (activeWorkbenchWindow == null) {
      return;
    }

    activeWorkbenchWindow.addPerspectiveListener(perspectiveListener);

    WorkbenchPage activePage = (WorkbenchPage) activeWorkbenchWindow.getActivePage();
    if (activePage == null) {
      return;
    }

    IPerspectiveDescriptor perspectiveDesc = activePage.getPerspective();
    maybeAddNewWizardActionsToPerspective(activePage, perspectiveDesc);
  }

  /**
   * Adds the new wizards to the current perspective displayed in the
   * workbench's active window, if they've not been added already. Adds
   * listeners on the workbench so that the same is done for any new workbench
   * windows that are created.
   * 
   * Note: This method can only be called once the workbench has been started.
   */
  private void maybeAddNewWizardActionsToWorkbench() {
    IWorkbench workbench = Workbench.getInstance();
    if (workbench != null) {
      workbench.addWindowListener(windowListener);
      maybeAddNewWizardActionsToWindow(workbench.getActiveWorkbenchWindow());
    } else {
      // This should never happen; the workbench must be started by the time
      // this code is executed
    }
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    plugin = this;
    logger = new Logger(this);

    // Force the installation id initialization before SDK registration.
    getInstallationId();

    GdtPreferences.registerSdks();
    ProjectUtilities.setWebAppProjectCreatorFactory(WebAppProjectCreator.FACTORY);

    /*
     * Execute this on the UI thread. This has the effect of delaying the
     * execution until the Workbench is running and the UI is available. This is
     * necessary because the code in this method manipulates the Workbench UI.
     */
    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        maybeAddNewWizardActionsToWorkbench();
      }
    });

    LaunchConfigAffectingChangesListener.INSTANCE.start();

    // Load problem severities
    GdtProblemSeverities.initializeInstance(new Class<?>[]{
        AppEngineProblemType.class, GWTProblemType.class,
        RemoteServiceProblemType.class, UiBinderJavaProblemType.class,
        ClientBundleProblemType.class, ProjectStructureOrSdkProblemType.class,
        UiBinderTemplateProblemType.class},
        GdtPreferences.getEncodedProblemSeverities());

    rebuildGoogleProjectsIfPluginVersionChanged();

    new ProjectMigrator().migrate();
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    LaunchConfigAffectingChangesListener.INSTANCE.stop();

    plugin = null;
    logger = null;

    super.stop(context);
  }

}
