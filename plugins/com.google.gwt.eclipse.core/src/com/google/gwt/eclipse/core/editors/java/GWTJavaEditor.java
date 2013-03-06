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
package com.google.gwt.eclipse.core.editors.java;

import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CleanUpPostSaveListener;
import org.eclipse.jdt.internal.corext.fix.CleanUpPreferenceUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.CompositeActionGroup;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.javaeditor.saveparticipant.IPostSaveListener;
import org.eclipse.jdt.internal.ui.javaeditor.saveparticipant.SaveParticipantRegistry;
import org.eclipse.jdt.ui.actions.OpenEditorActionGroup;
import org.eclipse.jdt.ui.text.IJavaPartitions;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.actions.ActionGroup;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Subclasses the JDT Java editor to add GWT-specific functionality such as
 * custom handling for JSNI method.
 */
@SuppressWarnings("restriction")
public class GWTJavaEditor extends CompilationUnitEditor {

  public static final String EDITOR_ID = GWTPlugin.PLUGIN_ID
      + ".editors.gwtJavaEditor";

  // TODO: this should actually come from CleanUpConstants.TRUE for Eclipse 3.3,
  // and CleanUpOptions.TRUE for 3.4
  private static final String CLEAN_UP_OPTION_TRUE = "true";

  // Default to Java partitioning, but this will be overwritten in doSetInput
  // with GWT partitioning if the input file is in a GWT project.
  private String inputPartitioning = IJavaPartitions.JAVA_PARTITIONING;

  public String getInputPartitioning() {
    return inputPartitioning;
  }

  /**
   * This adds the Open Declaration (F3) behavior to Java references inside JSNI
   * blocks.
   */
  @Override
  protected void createActions() {
    super.createActions();

    GWTOpenEditorActionGroup gwtOpenEditorActionGroup = new GWTOpenEditorActionGroup(
        this);

    try {
      // Replace the OpenEditorActionGroup from JavaEditor that contributes the
      // main menu items and keyboard binding
      replaceOpenEditorAction(fActionGroups, gwtOpenEditorActionGroup);

      // Also need to replace the OpenEditorActionGroup that contributes the
      // context menu items. This field is private, so we use reflection here.
      Field contextMenuField = JavaEditor.class.getDeclaredField("fContextMenuGroup");
      contextMenuField.setAccessible(true);
      CompositeActionGroup contextMenuGroup = (CompositeActionGroup) contextMenuField.get(this);
      replaceOpenEditorAction(contextMenuGroup, gwtOpenEditorActionGroup);

    } catch (Exception e) {
      GWTPluginLog.logError(e);
    }
  }

  @Override
  protected JavaSourceViewerConfiguration createJavaSourceViewerConfiguration() {
    JavaTextTools textTools = JavaPlugin.getDefault().getJavaTextTools();
    return new GWTSourceViewerConfiguration(textTools.getColorManager(),
        getPreferenceStore(), this);
  }

  @Override
  protected void doSetInput(IEditorInput input) throws CoreException {
    IJavaProject javaProject = EditorUtility.getJavaProject(input);

    if (javaProject != null && GWTNature.isGWTProject(javaProject.getProject())) {
      // Use GWT partitioning if the compilation unit is in a GWT project
      inputPartitioning = GWTPartitions.GWT_PARTITIONING;
    } else {
      // Otherwise, use Java partitioning to emulate the Java editor
      inputPartitioning = IJavaPartitions.JAVA_PARTITIONING;
    }

    super.doSetInput(input);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected void performSave(final boolean overwrite,
      final IProgressMonitor progressMonitor) {

    final ICompilationUnit cu = (ICompilationUnit) getInputJavaElement();
    final IJavaProject javaProject = cu.getJavaProject();
    if (javaProject == null || !formatOnSaveEnabled(javaProject)) {
      // If the user doesn't have format-on-save enabled, just delegate to super
      super.performSave(overwrite, progressMonitor);
      return;
    }

    final IDocument document = getDocumentProvider().getDocument(
        getEditorInput());

    // save all the original jsni methods as strings, in case the JS formatter
    // fails (e.g., syntax error), so that it has something to fall back on
    final String[] originalJsniMethods = JsniFormattingUtil.getJsniMethods(document);

    runBehindSmokescreen(new Runnable() {
      public void run() {
        // Have the JDT perform its own save-and-format first
        GWTJavaEditor.super.performSave(overwrite, progressMonitor);

        /*
         * We need to work around the JDT bug which causes JSNI blocks to
         * continually be shifted to the right when .java files are formatted
         * automatically on save. The fix is to reformat the file ourselves with
         * correct indentation and then re-save.
         */
        try {

          // Get the formatting edit and apply it
          TextEdit edit = JsniFormattingUtil.format(document,
              javaProject, originalJsniMethods);
          if (edit != null) {
            edit.apply(document);

            // If we made changes, re-save the .java file
            IBuffer buffer = cu.getBuffer();
            if (buffer.hasUnsavedChanges()) {
              buffer.save(null, true);
            }
          }
        } catch (Exception e) {
          GWTPluginLog.logError(e);
        }
      }
    });
  }

  @Override
  protected void setPreferenceStore(IPreferenceStore store) {
    super.setPreferenceStore(store);
    setSourceViewerConfiguration(createJavaSourceViewerConfiguration());
  }

  ICompilationUnit getCompilationUnit() {
    return (ICompilationUnit) getInputJavaElement();
  }

  IJavaProject getInputJavaProject() {
    return getInputJavaElement().getJavaProject();
  }

  @SuppressWarnings("unchecked")
  private boolean formatOnSaveEnabled(IJavaProject javaProject) {
    SaveParticipantRegistry spr = JavaPlugin.getDefault().getSaveParticipantRegistry();
    IPostSaveListener[] listeners = spr.getEnabledPostSaveListeners(javaProject.getProject());

    for (IPostSaveListener listener : listeners) {
      if (listener instanceof CleanUpPostSaveListener) {
        Map settings = CleanUpPreferenceUtil.loadSaveParticipantOptions(new ProjectScope(
            javaProject.getProject()));

        if (settings == null) {
          return false;
        }

        return (CLEAN_UP_OPTION_TRUE.equals(settings.get(CleanUpConstants.FORMAT_SOURCE_CODE)));
      }
    }

    return false;
  }

  /**
   * Overwrite the OpenEditorActionGroup in the CompositeActionGroup with our
   * custom GWTOpenEditorActionGroup that contains our JSNI-aware GWTOpenAction.
   */
  private void replaceOpenEditorAction(CompositeActionGroup actionGroup,
      GWTOpenEditorActionGroup newAction) throws Exception {

    // The fGroups backing array in CompositeActionGroup is private, so we have
    // to resort to reflection to update it.
    Field groupsField = CompositeActionGroup.class.getDeclaredField("fGroups");
    groupsField.setAccessible(true);
    ActionGroup[] actionGroups = (ActionGroup[]) groupsField.get(actionGroup);

    // Search the ActionGroup array and replace the OpenEditorActionGroup
    for (int i = 0; i < actionGroups.length; i++) {
      if (actionGroups[i] instanceof OpenEditorActionGroup) {
        actionGroups[i] = newAction;
        return;
      }
    }

    throw new Exception("No existing OpenEditorActionGroup found");
  }

  /**
   * This is pretty hacky, but this code comes straight from the JDT's
   * RenameLinkedMode class. Its doRename() method uses this method to hide the
   * fact that it rolls back the document changes made during a linked-mode
   * rename refactoring before reapplying the changes through the regular rename
   * refactoring processor. If we can find a different way to work around the
   * visual quirk caused by JDT's auto-format-on-save-bug, we should definitely
   * use that instead.
   * 
   * @param action the editor operation to run hidden
   */
  private void runBehindSmokescreen(Runnable action) {
    Image image = null;
    Label label = null;

    try {
      ISourceViewer viewer = getViewer();
      if (viewer instanceof SourceViewer) {
        SourceViewer sourceViewer = (SourceViewer) viewer;
        Control viewerControl = sourceViewer.getControl();
        if (viewerControl instanceof Composite) {
          Composite composite = (Composite) viewerControl;
          Display display = composite.getDisplay();

          // Flush pending redraw requests
          while (!display.isDisposed() && display.readAndDispatch()) {
          }

          // Copy editor area into an Image
          GC gc = new GC(composite);
          Point size;
          try {
            size = composite.getSize();
            image = new Image(gc.getDevice(), size.x, size.y);
            gc.copyArea(image, 0, 0);
          } finally {
            gc.dispose();
            gc = null;
          }

          // Hide the editor area behind the Image
          label = new Label(composite, SWT.NONE);
          label.setImage(image);
          label.setBounds(0, 0, size.x, size.y);
          label.moveAbove(null);
        }
      }

      // Perform the action
      action.run();

    } finally {
      if (label != null) {
        label.dispose();
      }
      if (image != null) {
        image.dispose();
      }
    }
  }

}
