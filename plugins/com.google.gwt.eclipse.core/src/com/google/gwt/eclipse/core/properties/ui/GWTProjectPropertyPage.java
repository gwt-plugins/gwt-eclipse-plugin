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
package com.google.gwt.eclipse.core.properties.ui;

import com.google.gdt.eclipse.core.BuilderUtilities;
import com.google.gdt.eclipse.core.ClasspathUtilities;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gdt.eclipse.core.extensions.ExtensionQuery;
import com.google.gdt.eclipse.core.properties.WebAppProjectProperties;
import com.google.gdt.eclipse.core.sdk.SdkManager;
import com.google.gdt.eclipse.core.sdk.UpdateProjectSdkCommand.UpdateType;
import com.google.gdt.eclipse.core.ui.AbstractProjectPropertyPage;
import com.google.gdt.eclipse.core.ui.ProjectSdkSelectionBlock;
import com.google.gdt.eclipse.core.ui.SdkSelectionBlock.SdkSelection;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.editors.java.GWTJavaEditor;
import com.google.gwt.eclipse.core.launch.ui.EntryPointModulesSelectionBlock;
import com.google.gwt.eclipse.core.nature.GWTNature;
import com.google.gwt.eclipse.core.preferences.GWTPreferences;
import com.google.gwt.eclipse.core.preferences.ui.GwtPreferencePage;
import com.google.gwt.eclipse.core.properties.GWTProjectProperties;
import com.google.gwt.eclipse.core.runtime.GWTProjectsRuntime;
import com.google.gwt.eclipse.core.runtime.GWTRuntime;
import com.google.gwt.eclipse.core.runtime.GWTRuntimeContainer;
import com.google.gwt.eclipse.core.sdk.GWTUpdateProjectSdkCommand;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.debug.ui.SWTFactory;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.osgi.service.prefs.BackingStoreException;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Property page for setting GWT project properties (SDK selection and the
 * default set of entry point modules).
 */
@SuppressWarnings("restriction")
public class GWTProjectPropertyPage extends AbstractProjectPropertyPage {

  /**
   * Interface to allow extension points to determine whether this project is
   * eligible for GWT SDK selection.
   * 
   */
  public interface GwtSdkSelectionEnablementFinder {
    boolean shouldEnableGwtSdkSelection(IProject project);
  }

  public static final String ID = GWTPlugin.PLUGIN_ID
      + ".properties.ui.gwtProjectPropertyPage";

  private EntryPointModulesSelectionBlock entryPointModulesBlock;

  private List<String> initialEntryPointModules;

  private boolean initialUseGWT;

  private ProjectSdkSelectionBlock<GWTRuntime> sdkSelectionBlock;

  private boolean useGWT;

  private Button useGWTCheckbox;

  public GWTProjectPropertyPage() {
    noDefaultAndApplyButton();
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite panel = new Composite(parent, SWT.NONE);
    panel.setLayout(new GridLayout());

    useGWTCheckbox = new Button(panel, SWT.CHECK);
    useGWTCheckbox.setText("Use Google Web Toolkit");

    createSdkComponent(panel);
    createEntryPointModuleComponent(panel);
    recordInitialSettings();
    initializeControls();
    addEventHandlers();
    fieldChanged();

    return panel;
  }

  @Override
  protected void saveProjectProperties() throws BackingStoreException,
      CoreException, FileNotFoundException {

    // Only add or remove GWT if the nature or SDK actually changed
    if (hasNatureChanged() || hasSdkChanged()) {
      if (useGWT) {
        addGWT();
      } else {
        removeGWT();
      }
    }

    if (useGWT) {
      saveChangesToEntryPointModules();
    }
  }

  private void addEventHandlers() {
    useGWTCheckbox.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fieldChanged();
      }
    });

    /*
     * TODO: Add event handler on the ProjectSdkSelectionBlock to detect when
     * the user has selected a GWT SDK that would lead to an incompatible
     * project configuration (i.e. using pre-GWT 1.6 with a project that has the
     * WAR Nature).
     */
  }

  /**
   * Removes all GWT SDK jars and replaces it with a single container entry,
   * adds GWT nature and optionally the web app nature.
   * 
   * @throws BackingStoreException
   * @throws FileNotFoundException
   * @throws FileNotFoundException
   */
  private void addGWT() throws CoreException, BackingStoreException,
      FileNotFoundException {

    IProject project = getProject();

    IJavaProject javaProject = JavaCore.create(project);

    /*
     * Set the appropriate web app project properties if this is a J2EE project.
     * 
     * There can be a collision between different property pages manipulating
     * the same web app properties, but the collision actually works itself out.
     * 
     * Both the GWT and GAE property pages make a call to this method. So, there
     * are no conflicting/differing settings of the web app project properties
     * in this case.
     * 
     * In the event that the GAE/GWT natures are enabled and the Web App
     * property page does not have the "This Project Has a War Directory"
     * setting selected, and that setting is enabled, then the settings on the
     * Web App project page will take precedence (over those settings that are
     * set by this method call).
     * 
     * The gory details as to why have to do with the order of application of
     * the properties for each page (App Engine, Web App, then GWT), and the
     * fact that this method will not make any changes to Web App properties if
     * the project is already a Web App.
     */
    WebAppProjectProperties.maybeSetWebAppPropertiesForDynamicWebProject(project);

    if (sdkSelectionBlock.hasSdkChanged()
        && !GWTProjectsRuntime.isGWTRuntimeProject(javaProject)) {
      SdkSelection<GWTRuntime> sdkSelection = sdkSelectionBlock.getSdkSelection();
      boolean isDefault = false;
      GWTRuntime newSdk = null;
      if (sdkSelection != null) {
        newSdk = sdkSelection.getSelectedSdk();
        isDefault = sdkSelection.isDefault();
      }

      GWTRuntime oldSdk = sdkSelectionBlock.getInitialSdk();

      UpdateType updateType = GWTUpdateProjectSdkCommand.computeUpdateType(
          oldSdk, newSdk, isDefault);

      GWTUpdateProjectSdkCommand updateProjectSdkCommand = new GWTUpdateProjectSdkCommand(
          javaProject, oldSdk, newSdk, updateType, null);

      /*
       * Update the project classpath which will trigger the <WAR>/WEB-INF/lib
       * jars to be updated.
       */
      updateProjectSdkCommand.execute();
    }

    GWTNature.addNatureToProject(project);

    // Need to rebuild to get GWT errors to appear
    BuilderUtilities.scheduleRebuild(project);

    // only prompt to reopen editors if the transition from disabled -> enabled
    if (!initialUseGWT && useGWT) {
      // Get the list of Java editors opened on files in this project
      IEditorReference[] openEditors = getOpenJavaEditors(project);
      if (openEditors.length > 0) {
        MessageDialog dlg = new MessageDialog(
            GWTPlugin.getActiveWorkbenchShell(),
            GWTPlugin.getName(),
            null,
            "GWT editing functionality, such as syntax-colored JSNI blocks, "
                + "will only be enabled after you re-open your Java editors.\n\nDo "
                + "you want to re-open your editors now?",
            MessageDialog.QUESTION,
            new String[] {"Re-open Java editors", "No"}, 0);
        if (dlg.open() == IDialogConstants.OK_ID) {
          reopenWithGWTJavaEditor(openEditors);
        }
      }
    }
  }

  private void createEntryPointModuleComponent(Composite parent) {
    Group group = SWTFactory.createGroup(parent, "Entry Point Modules", 3, 1,
        GridData.FILL_BOTH);
    GridLayout groupLayout = (GridLayout) group.getLayout();
    groupLayout.marginBottom = 8;
    group.setLayout(groupLayout);

    entryPointModulesBlock = new EntryPointModulesSelectionBlock(null);
    entryPointModulesBlock.doFillIntoGrid(group, 3);
  }

  private void createSdkComponent(Composite parent) {
    Group group = SWTFactory.createGroup(parent, "GWT SDK", 1, 1,
        GridData.FILL_HORIZONTAL);

    sdkSelectionBlock = new ProjectSdkSelectionBlock<GWTRuntime>(group,
        SWT.NONE, getJavaProject()) {
      @Override
      protected void doConfigure() {
        if (Window.OK == PreferencesUtil.createPreferenceDialogOn(getShell(),
            GwtPreferencePage.ID, new String[] {GwtPreferencePage.ID}, null).open()) {
          GWTProjectPropertyPage.this.fieldChanged();
        }
      }

      @Override
      protected GWTRuntime doFindSdkFor(IJavaProject javaProject) {
        try {
          return GWTRuntime.findSdkFor(javaProject);
        } catch (JavaModelException e) {
          GWTPluginLog.logError(e);
          return null;
        }
      }

      @Override
      protected String doGetContainerId() {
        return GWTRuntimeContainer.CONTAINER_ID;
      }

      @Override
      protected SdkManager<GWTRuntime> doGetSdkManager() {
        return GWTPreferences.getSdkManager();
      }
    };
  }

  private void fieldChanged() {
    validateFields();
    updateControls();
  }

  private IEditorReference[] getOpenJavaEditors(IProject project) {
    List<IEditorReference> projectOpenJavaEditors = new ArrayList<IEditorReference>();
    try {
      IWorkbenchPage page = JavaPlugin.getActivePage();
      if (page != null) {
        // Iterate through all the open editors
        IEditorReference[] openEditors = page.getEditorReferences();
        for (IEditorReference openEditor : openEditors) {
          IEditorPart editor = openEditor.getEditor(false);

          // Only look for Java Editor and subclasses
          if (editor instanceof CompilationUnitEditor) {
            IEditorInput input = openEditor.getEditorInput();
            IJavaProject inputProject = EditorUtility.getJavaProject(input);

            // See if the editor is editing a file in this project
            if (inputProject != null
                && inputProject.getProject().equals(project)) {
              projectOpenJavaEditors.add(openEditor);
            }
          }
        }
      }
    } catch (PartInitException e) {
      GWTPluginLog.logError(e);
    }

    return projectOpenJavaEditors.toArray(new IEditorReference[0]);
  }

  private boolean hasNatureChanged() {
    return useGWT ^ initialUseGWT;
  }

  private boolean hasSdkChanged() {
    return sdkSelectionBlock.hasSdkChanged();
  }

  private void initializeControls() {
    useGWTCheckbox.setSelection(initialUseGWT);

    // Set the project for the block (needed for adding a module)
    entryPointModulesBlock.setJavaProject(getJavaProject());

    // Set the default and selected modules for the block
    entryPointModulesBlock.setDefaultModules(GWTProjectProperties.getDefaultEntryPointModules(getProject()));
    entryPointModulesBlock.setModules(initialEntryPointModules);
  }

  private void recordInitialSettings() {
    initialUseGWT = GWTNature.isGWTProject(getProject());

    initialEntryPointModules = GWTProjectProperties.getEntryPointModules(getProject());
  }

  private void removeGWT() throws BackingStoreException, CoreException {
    GWTNature.removeNatureFromProject(getProject());
    ClasspathUtilities.replaceContainerWithClasspathEntries(getJavaProject(),
        GWTRuntimeContainer.CONTAINER_ID);
    GWTProjectProperties.setFileNamesCopiedToWebInfLib(getProject(),
        Collections.<String> emptyList());
  }

  private void reopenWithGWTJavaEditor(IEditorReference[] openEditors) {
    IWorkbenchPage page = JavaPlugin.getActivePage();

    for (IEditorReference editorRef : openEditors) {
      try {
        IEditorPart editor = editorRef.getEditor(false);
        IEditorInput input = editorRef.getEditorInput();

        // Close the editor, prompting the user to save if document is dirty
        if (page.closeEditor(editor, true)) {
          // Re-open the .java file in the GWT Java Editor
          IEditorPart gwtEditor = page.openEditor(input,
              GWTJavaEditor.EDITOR_ID);

          // Save the file from the new editor if the Java editor's
          // auto-format-on-save action screwed up the JSNI formatting
          gwtEditor.doSave(null);
        }
      } catch (PartInitException e) {
        GWTPluginLog.logError(e, "Could not open GWT Java editor on {0}",
            editorRef.getTitleToolTip());
      }
    }
  }

  private void saveChangesToEntryPointModules() throws BackingStoreException {
    List<String> userEntryPointModules = new ArrayList<String>();

    // Only save the set of entry point modules if it differs from the
    // project's default set. Otherwise, clear the project setting.
    if (!entryPointModulesBlock.isDefault()) {
      userEntryPointModules = entryPointModulesBlock.getModules();
    }
    GWTProjectProperties.setEntryPointModules(getProject(),
        userEntryPointModules);
  }

  private void updateControls() {
    IJavaProject javaProject = JavaCore.create(getProject());

    boolean shouldEnable = !GWTProjectsRuntime.isGWTRuntimeProject(javaProject);
    ExtensionQuery<GWTProjectPropertyPage.GwtSdkSelectionEnablementFinder> extQuery = new ExtensionQuery<GWTProjectPropertyPage.GwtSdkSelectionEnablementFinder>(
        GWTPlugin.PLUGIN_ID, "gwtSdkSelectionEnablementFinder", "class");
    List<ExtensionQuery.Data<GWTProjectPropertyPage.GwtSdkSelectionEnablementFinder>> enablementFinders = extQuery.getData();
    for (ExtensionQuery.Data<GWTProjectPropertyPage.GwtSdkSelectionEnablementFinder> enablementFinder : enablementFinders) {
      shouldEnable = enablementFinder.getExtensionPointData().shouldEnableGwtSdkSelection(
          javaProject.getProject());
    }
    if (shouldEnable) {
      sdkSelectionBlock.setEnabled(useGWT);
      entryPointModulesBlock.setEnabled(useGWT);
    } else {
      sdkSelectionBlock.setEnabled(false);
      entryPointModulesBlock.setEnabled(false);
    }
  }

  private void validateFields() {
    IStatus useGWTStatus = validateUseGWT();
    IStatus sdkStatus = validateSdk();
    updateStatus(new IStatus[] {useGWTStatus, sdkStatus});
  }

  private IStatus validateSdk() {
    if (!useGWT) {
      // Ignore any SDK problems since App Engine is not enabled
      return StatusUtilities.OK_STATUS;
    }

    // If we're adding the GWT nature to a project that's part of a GWT
    // projects runtime (or could be), we don't need to have any other GWT SDKs;
    // otherwise, we need a GWT SDK.
    IJavaProject javaProject = JavaCore.create(getProject());
    if (!GWTProjectsRuntime.isGWTRuntimeProject(javaProject)) {
      if (sdkSelectionBlock.getSdks().isEmpty()) {
        return StatusUtilities.newErrorStatus("Please configure an SDK",
            GWTPlugin.PLUGIN_ID);
      }
    }
    /*
     * TODO: Add validation to detect when the user has selected a GWT SDK that
     * would lead to an incompatible project configuration (i.e. using pre-GWT
     * 1.6 with a project that has the WAR Nature).
     */
    return StatusUtilities.OK_STATUS;
  }

  private IStatus validateUseGWT() {
    useGWT = useGWTCheckbox.getSelection();
    return StatusUtilities.OK_STATUS;
  }
}
