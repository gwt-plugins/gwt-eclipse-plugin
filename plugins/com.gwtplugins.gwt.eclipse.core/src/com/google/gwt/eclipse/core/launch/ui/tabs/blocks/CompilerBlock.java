/**
 *
 */
package com.google.gwt.eclipse.core.launch.ui.tabs.blocks;

import com.google.gdt.eclipse.core.SWTUtilities;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;
import com.google.gwt.eclipse.core.launch.GWTLaunchConfiguration;
import com.google.gwt.eclipse.core.launch.GWTLaunchConfigurationWorkingCopy;
import com.google.gwt.eclipse.core.launch.processors.LogLevelArgumentProcessor;
import com.google.gwt.eclipse.core.launch.ui.tabs.IUpdateLaunchConfig;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.internal.debug.ui.SWTFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

public class CompilerBlock extends Composite {

  /**
   * Provides the labels for the log level and output style combos. The label is calculated by
   * calculating just the first letter of the element.
   */
  private static class DefaultComboLabelProvider extends LabelProvider {
    @Override
    public String getText(Object element) {
      String element2 = (String) element;

      // TODO: this is sensitive to locale. Consider using a helper class
      // to do the label generation
      return element2.toUpperCase().charAt(0) + element2.toLowerCase().substring(1);
    }
  }

  private static final String GROUP_COMPILER_TITLE = "Compiler Arguments";

  private final Group groupDevMode;
  private final ComboViewer logLevelComboViewer;

  private IUpdateLaunchConfig updateLaunchConfigHandler;

  public CompilerBlock(Composite root, int style) {
    super(root, style);

    setLayout(new FillLayout(SWT.HORIZONTAL));

    Composite parent = new Composite(this, SWT.NONE);
    parent.setLayout(new GridLayout(1, false));

    groupDevMode = SWTFactory.createGroup(parent, GROUP_COMPILER_TITLE, 2, 1, GridData.FILL_HORIZONTAL);

    // Log level
    Label label = SWTFactory.createLabel(groupDevMode, "Log level:", 1);
    GridData gd_label = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
    gd_label.widthHint = 101;
    label.setLayoutData(gd_label);
    logLevelComboViewer = new ComboViewer(groupDevMode, SWT.READ_ONLY);
    new Label(groupDevMode, SWT.NONE);
    logLevelComboViewer.setContentProvider(new ArrayContentProvider());
    logLevelComboViewer.setLabelProvider(new DefaultComboLabelProvider());
    logLevelComboViewer.setInput(LogLevelArgumentProcessor.LOG_LEVELS);
    logLevelComboViewer.addPostSelectionChangedListener(new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        updateLaunchConfigHandler.updateLaunchConfig();
      }
    });
  }

  public String getLogLevel() {
    StructuredSelection logLevelSelection = (StructuredSelection) logLevelComboViewer.getSelection();
    return logLevelSelection.getFirstElement() != null ? logLevelSelection.getFirstElement().toString()
        : LogLevelArgumentProcessor.DEFAULT_LOG_LEVEL;
  }

  public void initializeFrom(ILaunchConfiguration config) throws CoreException {
    SWTUtilities.setText(groupDevMode, GROUP_COMPILER_TITLE);

    logLevelComboViewer.setSelection(new StructuredSelection(GWTLaunchConfiguration.getLogLevel(config)));
  }

  public void performApply(ILaunchConfigurationWorkingCopy launchConfig) {
    // Dev Mode Save the log level
    GWTLaunchConfigurationWorkingCopy.setLogLevel(launchConfig, getLogLevel());
    LaunchConfigurationProcessorUtilities.updateViaProcessor(new LogLevelArgumentProcessor(), launchConfig);
  }

  @Override
  protected void checkSubclass() {
    // Disable the check that prevents subclassing of SWT components
  }

  public void addUpdateLaunchConfigHandler(IUpdateLaunchConfig updateLaunchConfigHandler) {
    this.updateLaunchConfigHandler = updateLaunchConfigHandler;
  }

}
