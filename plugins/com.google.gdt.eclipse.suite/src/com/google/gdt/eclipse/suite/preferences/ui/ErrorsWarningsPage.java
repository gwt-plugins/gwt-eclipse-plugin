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
package com.google.gdt.eclipse.suite.preferences.ui;

import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.gdt.eclipse.core.BuilderUtilities;
import com.google.gdt.eclipse.core.markers.GdtProblemCategory;
import com.google.gdt.eclipse.core.markers.GdtProblemSeverities;
import com.google.gdt.eclipse.core.markers.GdtProblemSeverity;
import com.google.gdt.eclipse.core.markers.IGdtProblemType;
import com.google.gdt.eclipse.suite.GdtPlugin;
import com.google.gdt.eclipse.suite.preferences.GdtPreferences;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.internal.ui.preferences.PropertyAndPreferencePage;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Preference page for customizing the severity of problem markers.
 * 
 * NOTE: We're subclassing the internal PropertyAndPreferencePage rather than
 * the vanilla PreferencePage, so we can easily add project-level overrides for
 * problem severities in the future.
 */
@SuppressWarnings("restriction")
public class ErrorsWarningsPage extends PropertyAndPreferencePage {

  private Map<GdtProblemCategory, Composite> problemCategoryPanels = new EnumMap<GdtProblemCategory, Composite>(
      GdtProblemCategory.class);

  private GdtProblemSeverities problemSeveritiesWorkingCopy = GdtProblemSeverities.getInstance().createWorkingCopy();

  private Map<IGdtProblemType, ComboViewer> problemSeverityCombos = new HashMap<IGdtProblemType, ComboViewer>();

  private Composite problemsPanel;

  private ScrolledComposite scrollPanel;

  private LabelProvider severityLabelProvider = new LabelProvider() {
    @Override
    public String getText(Object element) {
      GdtProblemSeverity severity = (GdtProblemSeverity) element;
      return severity.getDisplayName();
    }
  };

  private ViewerSorter severityViewerSorter = new ViewerSorter() {
    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
      return ((GdtProblemSeverity) e2).getSeverityId()
          - ((GdtProblemSeverity) e1).getSeverityId();
    }
  };

  private Composite topPanel;

  public ErrorsWarningsPage() {
    setDescription("Select the severity level for the following problems.");
  }
  
  @Override
  public boolean performOk() {
    updateWorkingCopyFromCombos();

    if (!GdtProblemSeverities.getInstance().equals(problemSeveritiesWorkingCopy)) {
      MessageDialog dialog = new MessageDialog(getShell(),
          "Errors/Warnings Settings Changed", null,
          "The Google Error/Warning settings have changed.  A full rebuild "
              + "of all GWT/App Engine projects is required for changes to "
              + "take effect.  Do the full build now?", MessageDialog.QUESTION,
          new String[] {
              IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL,
              IDialogConstants.CANCEL_LABEL}, 2); // Cancel is default
      int result = dialog.open();

      if (result == 2) { // Cancel
        return false;
      } else {
        updateWorkspaceSeveritySettingsFromWorkingCopy();

        if (result == 0) { // Yes
          BuilderUtilities.scheduleRebuildAll(GWTNature.NATURE_ID,
              GaeNature.NATURE_ID);
        }
      }
    }
    return true;
  }

  @Override
  protected Control createPreferenceContent(Composite parent) {
    scrollPanel = new ScrolledComposite(parent, SWT.V_SCROLL);
    GridData scrollPanelGridData = new GridData(GridData.FILL_BOTH);
    scrollPanel.setLayoutData(scrollPanelGridData);
    GridLayout scrollPanelLayout = new GridLayout(1, false);
    scrollPanel.setLayout(scrollPanelLayout);

    topPanel = new Composite(scrollPanel, SWT.NONE);
    GridLayout layout = new GridLayout(1, false);
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    topPanel.setLayout(layout);

    // Set up main composite to be scrollable
    scrollPanel.setContent(topPanel);
    scrollPanel.setExpandHorizontal(true);
    scrollPanel.setExpandVertical(true);

    // Create the list of problems (grouped by category)
    createProblemsList(topPanel);

    ExpandableComposite firstChild = (ExpandableComposite)problemsPanel.getChildren()[0];
    firstChild.setExpanded(true);
    
    // Initialize the severity combos
    populateSeverityCombosFromWorkingCopy();

    
    return topPanel;
  }

  @Override
  protected String getPreferencePageID() {
    return GdtPlugin.PLUGIN_ID + ".preferences.ui.errorsWarnings";
  }

  @Override
  protected String getPropertyPageID() {
    // When we implement project-level overrides for problem severities, this
    // will return the relevant property page ID
    return null;
  }

  @Override
  protected boolean hasProjectSpecificOptions(IProject project) {
    return false;
  }

  @Override
  protected void performDefaults() {
    problemSeveritiesWorkingCopy.resetToDefaults();
    populateSeverityCombosFromWorkingCopy();
  }

  private void addProblemTypeRow(Composite categoryProblemsPanel,
      IGdtProblemType problemType) {
    GridData problemLabelLayout = new GridData(SWT.FILL, SWT.CENTER, true,
        false);

    Label problemLabel = new Label(categoryProblemsPanel, SWT.NONE);
    problemLabel.setLayoutData(problemLabelLayout);
    problemLabel.setText(problemType.getDescription());

    ComboViewer severityCombo = new ComboViewer(categoryProblemsPanel,
        SWT.READ_ONLY);
    GridData severityComboLayout = new GridData(SWT.FILL, SWT.CENTER, false,
        false);
    severityCombo.getCombo().setLayoutData(severityComboLayout);
    severityCombo.setContentProvider(new ArrayContentProvider());
    severityCombo.setLabelProvider(severityLabelProvider);
    severityCombo.setSorter(severityViewerSorter);
    severityCombo.setInput(GdtProblemSeverity.values());

    // Save the association between the problem type and this combo
    problemSeverityCombos.put(problemType, severityCombo);
  }

  private void addProblemTypeRows() {
    List<IGdtProblemType> problemTypes = new ArrayList<IGdtProblemType>(
        problemSeveritiesWorkingCopy.getAllProblemTypes());
    Collections.sort(problemTypes, new Comparator<IGdtProblemType>() {
      public int compare(IGdtProblemType a, IGdtProblemType b) {
        return a.getProblemId() - b.getProblemId();
      }
    });

    for (IGdtProblemType problemType : problemTypes) {
      Composite categoryProblemsPanel = problemCategoryPanels.get(problemType.getCategory());
      addProblemTypeRow(categoryProblemsPanel, problemType);
    }
  }

  private Composite createProblemCategory(Composite parent, String label) {
    // Expandable panel for each category of problems
    ExpandableComposite expandPanel = new ExpandableComposite(parent, SWT.NONE,
        ExpandableComposite.TWISTIE | ExpandableComposite.CLIENT_INDENT);
    expandPanel.setText(label);
    expandPanel.setExpanded(false);
    expandPanel.setFont(JFaceResources.getFontRegistry().getBold(
        JFaceResources.DIALOG_FONT));
    expandPanel.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true,
        false));
    expandPanel.addExpansionListener(new ExpansionAdapter() {
      @Override
      public void expansionStateChanged(ExpansionEvent e) {
        topPanel.layout(true, true);
        scrollPanel.setMinSize(topPanel.computeSize(SWT.DEFAULT, SWT.DEFAULT));
      }
    });

    // Create panel to store the actual problems
    Composite categoryPanel = new Composite(expandPanel, SWT.NONE);
    categoryPanel.setLayout(new GridLayout(2, false));
    expandPanel.setClient(categoryPanel);

    return categoryPanel;
  }

  private void createProblemsList(Composite parent) {
    // Create container for problems list
    problemsPanel = new Composite(parent, SWT.NONE);
    GridLayout scLayout = new GridLayout(1, false);
    scLayout.marginHeight = 0;
    scLayout.marginWidth = 0;
    problemsPanel.setLayout(scLayout);

    GridData gridData = new GridData(GridData.FILL, GridData.FILL, true, false);
    problemsPanel.setLayoutData(gridData);

    // Create an expandable composite for each problem category
    for (GdtProblemCategory category : GdtProblemCategory.getAllCategoriesInDisplayOrder()) {
      Composite categoryPanel = createProblemCategory(problemsPanel,
          category.getDisplayName());

      // Associate each panel with its associated problem category
      problemCategoryPanels.put(category, categoryPanel);
    }

    addProblemTypeRows();
  }

  private void populateSeverityCombosFromWorkingCopy() {
    for (IGdtProblemType problemType : problemSeverityCombos.keySet()) {
      ComboViewer combo = problemSeverityCombos.get(problemType);
      GdtProblemSeverity severity = problemSeveritiesWorkingCopy.getSeverity(problemType);
      combo.setSelection(new StructuredSelection(severity));
    }
  }

  private void updateWorkingCopyFromCombos() {
    for (IGdtProblemType problemType : problemSeverityCombos.keySet()) {
      ComboViewer combo = problemSeverityCombos.get(problemType);
      GdtProblemSeverity severity = (GdtProblemSeverity) ((IStructuredSelection) combo.getSelection()).getFirstElement();
      problemSeveritiesWorkingCopy.setSeverity(problemType, severity);
    }
  }

  private void updateWorkspaceSeveritySettingsFromWorkingCopy() {
    for (IGdtProblemType problemType : problemSeveritiesWorkingCopy.getAllProblemTypes()) {
      GdtProblemSeverities.getInstance().setSeverity(problemType,
          problemSeveritiesWorkingCopy.getSeverity(problemType));
    }

    GdtPreferences.setEncodedProblemSeverities(GdtProblemSeverities.getInstance().toPreferenceString());
  }

}
