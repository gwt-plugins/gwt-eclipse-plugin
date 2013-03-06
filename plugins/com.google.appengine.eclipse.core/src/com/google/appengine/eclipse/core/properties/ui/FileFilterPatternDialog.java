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
package com.google.appengine.eclipse.core.properties.ui;

import com.google.appengine.eclipse.core.AppEngineCorePlugin;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gdt.eclipse.core.ui.ResourceTreeSelectionDialog;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import java.util.List;

/**
 * Edits a file filter pattern.
 */
@SuppressWarnings("restriction")
public class FileFilterPatternDialog extends StatusDialog {

  private class PatternFieldAdapter implements IDialogFieldListener,
      IStringButtonAdapter {

    public void changeControlPressed(DialogField field) {
      choosePattern();
    }

    public void dialogFieldChanged(DialogField field) {
      validateFields();
    }
  }

  public static List<IPath> choosePatterns(Shell shell, String title,
      String message, final IProject project, IPath initialPath,
      boolean multiSelection) {
    int resourceTypes = IResource.FOLDER | IResource.FILE;
    IResource initialResource = project.findMember(initialPath);
    ViewerFilter filter = new ResourceTreeSelectionDialog.ResourceFilter(
        resourceTypes) {
      @Override
      public boolean select(Viewer viewer, Object parentElement, Object element) {
        boolean isValid = super.select(viewer, parentElement, element);
        if (!isValid) {
          return false;
        }

        // Only show resources on the Java build path
        return JavaCore.create(project).isOnClasspath((IResource) element);
      }
    };

    ResourceTreeSelectionDialog dlg = new ResourceTreeSelectionDialog(shell,
        title, message, project, initialResource, resourceTypes, resourceTypes,
        multiSelection);
    dlg.addFilter(filter);
    return dlg.chooseResourcePaths(true);
  }

  private final IPath existingPattern;

  private final boolean isExclusion;

  private IPath pattern;

  private StringButtonDialogField patternField;

  private final PatternFieldAdapter patternFieldAdapter = new PatternFieldAdapter();

  private final IProject project;

  public FileFilterPatternDialog(Shell parent, IProject project,
      boolean isExclusion, IPath existingPattern) {
    super(parent);
    this.project = project;
    this.isExclusion = isExclusion;
    this.existingPattern = existingPattern;
  }

  public IPath getPattern() {
    return pattern.makeRelative();
  }

  @Override
  protected void configureShell(Shell shell) {
    String filterType = (isExclusion ? "Exclusion" : "Inclusion");
    String action = (existingPattern == null ? "Add" : "Edit");
    String title = action + " " + filterType + " Filter";
    setTitle(title);
    setHelpAvailable(false);

    super.configureShell(shell);
  }

  @Override
  protected Control createContents(Composite parent) {
    Control contents = super.createContents(parent);

    initializeControls();
    addEventHandlers();
    validateFields();

    return contents;
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite composite = (Composite) super.createDialogArea(parent);

    Composite inner = new Composite(composite, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.numColumns = 2;
    inner.setLayout(layout);

    Label descriptionLabel = new Label(inner, SWT.WRAP);
    String description = "Enter a pattern for "
        + (isExclusion ? "excluding" : "including")
        + " classes.  Allowed wildcards are *, ?, and **.\n\n"
        + "Examples: java/util/A*.java, java/util/, **/Test*";
    descriptionLabel.setText(description);
    GridData descriptionLabelGridData = new GridData();
    descriptionLabelGridData.horizontalSpan = 3;
    descriptionLabelGridData.widthHint = convertWidthInCharsToPixels(80);
    descriptionLabel.setLayoutData(descriptionLabelGridData);

    Label spacer = new Label(inner, SWT.NONE);
    GridData spacerGridData = new GridData();
    spacerGridData.horizontalSpan = 2;
    spacer.setLayoutData(spacerGridData);

    patternField = new StringButtonDialogField(patternFieldAdapter);
    patternField.setLabelText("Filter pattern (relative to project):");
    patternField.setButtonLabel("Browse...");
    patternField.doFillIntoGrid(inner, 3);

    int widthHint = convertWidthInCharsToPixels(60);
    LayoutUtil.setWidthHint(patternField.getLabelControl(null), widthHint);
    LayoutUtil.setHorizontalSpan(patternField.getLabelControl(null), 2);
    LayoutUtil.setWidthHint(patternField.getTextControl(null), widthHint);
    LayoutUtil.setHorizontalGrabbing(patternField.getTextControl(null));
    patternField.postSetFocusOnDialogField(parent.getDisplay());

    return composite;
  }

  private void addEventHandlers() {
    patternField.setDialogFieldListener(patternFieldAdapter);
  }

  private void choosePattern() {
    String title = (isExclusion ? "Exclusion" : "Inclusion") + " Selection";
    String message = "Choose a folder or file to "
        + (isExclusion ? "exclude" : "include") + ":";
    List<IPath> patterns = choosePatterns(getShell(), title, message, project,
        pattern, false);
    if (patterns != null) {
      assert (patterns.size() == 1);
      patternField.setText(patterns.get(0).toString());
    }
  }

  private void initializeControls() {
    if (existingPattern != null) {
      patternField.setText(existingPattern.toString());
    }
  }

  private void validateFields() {
    IStatus patternStatus = validatePattern();
    updateStatus(patternStatus);
  }

  private IStatus validatePattern() {
    pattern = null;

    String patternString = patternField.getText().trim();
    if (patternString.length() == 0) {
      return StatusUtilities.newErrorStatus("", AppEngineCorePlugin.PLUGIN_ID);
    }

    pattern = new Path(patternString);
    return StatusUtilities.OK_STATUS;
  }

}
