/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package copied.org.eclipse.jdt.internal.debug.ui.jres;

import com.google.appengine.eclipse.wtp.utils.PreferencesUtils;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.jdt.debug.ui.IJavaDebugUIConstants;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.actions.ControlAccessibleListener;
import org.eclipse.jdt.internal.debug.ui.jres.JREDescriptor;
import org.eclipse.jdt.internal.debug.ui.jres.JREMessages;
import org.eclipse.jdt.internal.debug.ui.jres.JREsPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.JavaBasePreferencePage;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMStandin;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * A composite that displays installed JREs in a combo box, with a 'manage...' button to modify
 * installed JREs.
 * <p>
 * This block implements ISelectionProvider - it sends selection change events when the checked JRE
 * in the table changes, or when the "use default" button check state changes.
 * </p>
 *
 * Google: originally, this internal class uses
 * org.eclipse.ui.dialogs.PreferencesUtil.createPreferenceDialogOn(Shell, String, String[], Object)
 * for displaying filtered preference page configuring installed JRE. But if preferences dialog is
 * open and this UI is open in another descendant dialog then using
 * PreferencesUtil.createPreferenceDialogOn() leads to reusing preferences dialog and thus it gets
 * displayed below (in Z-order) current descendant dialog.
 */
public class JREsComboBlock {
  
  public static final String PROPERTY_JRE = "PROPERTY_JRE"; //$NON-NLS-1$
  
  /**
   * This block's control
   */
  private Composite fControl;
  
  /**
   * VMs being displayed
   */
  private List<Object> fVMs = new ArrayList<Object>(); 
  
  /**
   * The main control
   */ 
  private Combo fCombo;
  
  // Action buttons
  private Link fManageButton;
    
  /**
   * JRE change listeners
   */
  private ListenerList fListeners = new ListenerList();
  
  /**
   * Whether the default JRE should be in first position (if <code>false</code>, it becomes last).
   */
  private boolean fDefaultFirst;
  
  /**
   * Default JRE descriptor or <code>null</code> if none.
   */
  private JREDescriptor fDefaultDescriptor = null;
  
  /**
   * Specific JRE descriptor or <code>null</code> if none.
   */
  private JREDescriptor fSpecificDescriptor = null;

  /**
   * Default JRE radio button or <code>null</code> if none
   */
  private Button fDefaultButton = null;
  
  /**
   * Selected JRE radio button
   */
  private Button fSpecificButton = null;
  
  /**
   * The title used for the JRE block
   */
  private String fTitle = null;
  
  // a path to an unavailable JRE
  private IPath fErrorPath;
  
  private IStatus fStatus = OK_STATUS;
  
  private static IStatus OK_STATUS = new Status(IStatus.OK, JDIDebugUIPlugin.getUniqueIdentifier(), 0, "", null); //$NON-NLS-1$

  /**
   * Creates a JREs combo block.
   * 
   * @param defaultFirst whether the default JRE should be in first position (if <code>false</code>, it becomes last)
   */
  public JREsComboBlock(boolean defaultFirst) {
    fDefaultFirst= defaultFirst;
  }

  public void addPropertyChangeListener(IPropertyChangeListener listener) {
    fListeners.add(listener);
  }
  
  public void removePropertyChangeListener(IPropertyChangeListener listener) {
    fListeners.remove(listener);
  }
  
  private void firePropertyChange() {
    PropertyChangeEvent event = new PropertyChangeEvent(this, PROPERTY_JRE, null, getPath());
    Object[] listeners = fListeners.getListeners();
    for (int i = 0; i < listeners.length; i++) {
      IPropertyChangeListener listener = (IPropertyChangeListener) listeners[i];
      listener.propertyChange(event);
    }
  }
  
  /**
   * Creates this block's control in the given control.
   * 
   * @param anscestor containing control
   */
  public void createControl(Composite ancestor) {
    fControl = SWTFactory.createComposite(ancestor, 1, 1, GridData.FILL_BOTH);
    ((GridLayout)fControl.getLayout()).marginWidth = 0;

    if (fTitle == null) {
      fTitle = JREMessages.JREsComboBlock_3; 
    }
    Group group = SWTFactory.createGroup(fControl, fTitle, 1, 1, GridData.FILL_HORIZONTAL); 
    Composite comp = SWTFactory.createComposite(group, group.getFont(), 3, 1, GridData.FILL_BOTH, 0, 0);
    
    if (fDefaultFirst) {
      createDefaultJREControls(comp);
    }
    createAlternateJREControls(comp);
    if (!fDefaultFirst) {
      createDefaultJREControls(comp);
    }
  }


  private void createAlternateJREControls(Composite comp) {
    String text = JREMessages.JREsComboBlock_1;
    if (fSpecificDescriptor != null) {
      text = fSpecificDescriptor.getDescription();
    }
    fSpecificButton = SWTFactory.createRadioButton(comp, text, 1);
    fSpecificButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        if (fSpecificButton.getSelection()) {
          fCombo.setEnabled(true);
          if (fCombo.getText().length() == 0 && !fVMs.isEmpty()) {
            fCombo.select(0);
          }
          if (fVMs.isEmpty()) {
            setError(JREMessages.JREsComboBlock_0);
          } else {
            setStatus(OK_STATUS);
          }         
          firePropertyChange();
        }
      }
    });
    fCombo = SWTFactory.createCombo(comp, SWT.DROP_DOWN | SWT.READ_ONLY, 1, null);
    ControlAccessibleListener.addListener(fCombo, fSpecificButton.getText());
    fCombo.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        setStatus(OK_STATUS);
        firePropertyChange();
      }
    });
        
    fManageButton = new Link(comp, SWT.NONE);
    fManageButton.setText("<a>" + JREMessages.JREsComboBlock_2 + "</a>"); 
    fManageButton.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        showPrefPage(JavaBasePreferencePage.JAVA_BASE_PREF_PAGE_ID, JREsPreferencePage.ID); 
      }
    });
    fillWithWorkspaceJREs();
  }

  private void createDefaultJREControls(Composite comp) {
    if (fDefaultDescriptor != null) {
      fDefaultButton = SWTFactory.createRadioButton(comp, fDefaultDescriptor.getDescription(), 3);
      fDefaultButton.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          if (fDefaultButton.getSelection()) {
            setUseDefaultJRE();
            setStatus(OK_STATUS);
            firePropertyChange();
          }
        }
      });
    }
  }
  
  /**
   * Opens the given preference page, and updates when closed.
   * Google changed.
   * @param id pref page id
   * @param page pref page
   */
  private void showPrefPage(String rootID, String pageID) {
    IVMInstall prevJRE = getJRE();
    PreferencesUtils.showPreferencePage(getShell(), rootID, pageID);
    fillWithWorkspaceJREs();
    restoreCombo(fVMs, prevJRE, fCombo);
    // update text
    setDefaultJREDescriptor(fDefaultDescriptor);
    if (isDefaultJRE()) {
      // reset in case default has changed
      setUseDefaultJRE();
    }
    setPath(getPath());
    firePropertyChange();   
  }
  
  private void restoreCombo(List<Object> elements, Object element, Combo combo) {
    int index = -1;
    if (element != null) {
      index = elements.indexOf(element);
    }
    if (index >= 0) {
      combo.select(index);
    } else {
      combo.select(0);
    }
  }
    
  /**
   * Returns this block's control
   * 
   * @return control
   */
  public Control getControl() {
    return fControl;
  }
  
  /**
   * Sets the JREs to be displayed in this block
   * 
   * @param vms JREs to be displayed
   */
  protected void setJREs(List<VMStandin> jres) {
    fVMs.clear();
    fVMs.addAll(jres);
    // sort by name
    Collections.sort(fVMs, new Comparator<Object>() {
      public int compare(Object o1, Object o2) {
        IVMInstall left = (IVMInstall)o1;
        IVMInstall right = (IVMInstall)o2;
        return left.getName().compareToIgnoreCase(right.getName());
      }

      @Override
      public boolean equals(Object obj) {
        return obj == this;
      }
    });
    // now make an array of names
    String[] names = new String[fVMs.size()];
    Iterator<Object> iter = fVMs.iterator();
    int i = 0;
    while (iter.hasNext()) {
      IVMInstall vm = (IVMInstall)iter.next();
      names[i] = vm.getName();
      i++;
    }
    fCombo.setItems(names);
    fCombo.setVisibleItemCount(Math.min(names.length, 20));
  }
  
  protected Shell getShell() {
    return getControl().getShell();
  }

  /**
   * Selects a specific JRE based on type/name.
   * 
   * @param vm JRE or <code>null</code>
   */
  private void selectJRE(IVMInstall vm) {
    fSpecificButton.setSelection(true);
    fDefaultButton.setSelection(false);
    fCombo.setEnabled(true);
    if (vm != null) {
      int index = fVMs.indexOf(vm);
      if (index >= 0) {
        fCombo.select(index);   
      }
    }
    firePropertyChange();
  }
  
  /**
   * Selects a JRE based environment.
   * 
   * @param env environment or <code>null</code>
   */
  
  /**
   * Returns the selected JRE or <code>null</code> if none.
   * 
   * @return the selected JRE or <code>null</code> if none
   */
  public IVMInstall getJRE() {
    int index = fCombo.getSelectionIndex();
    if (index >= 0) {
      return (IVMInstall)fVMs.get(index);
    }
    return null;
  }
  
  /**
   * Populates the JRE table with existing JREs defined in the workspace.
   */
  protected void fillWithWorkspaceJREs() {
    // fill with JREs
    List<VMStandin> standins = new ArrayList<VMStandin>();
    IVMInstallType[] types = JavaRuntime.getVMInstallTypes();
    for (int i = 0; i < types.length; i++) {
      IVMInstallType type = types[i];
      IVMInstall[] installs = type.getVMInstalls();
      for (int j = 0; j < installs.length; j++) {
        IVMInstall install = installs[j];
        standins.add(new VMStandin(install));
      }
    }
    setJREs(standins);  
  }
  
  /**
   * Sets the Default JRE Descriptor for this block.
   * 
   * @param descriptor default JRE descriptor
   */
  public void setDefaultJREDescriptor(JREDescriptor descriptor) {
    fDefaultDescriptor = descriptor;
    setButtonTextFromDescriptor(fDefaultButton, descriptor);
  }
  
  private void setButtonTextFromDescriptor(Button button, JREDescriptor descriptor) {
    if (button != null) {
      //update the description & JRE in case it has changed
      String currentText = button.getText();
      String newText = descriptor.getDescription();
      if (!newText.equals(currentText)) {
        button.setText(newText);
        fControl.layout();
      }
    }
  }

  /**
   * Sets the specific JRE Descriptor for this block.
   * 
   * @param descriptor specific JRE descriptor
   */
  public void setSpecificJREDescriptor(JREDescriptor descriptor) {
    fSpecificDescriptor = descriptor;
    setButtonTextFromDescriptor(fSpecificButton, descriptor);
  }
  
  /**
   * Returns whether the 'use default JRE' button is checked.
   * 
   * @return whether the 'use default JRE' button is checked
   */
  public boolean isDefaultJRE() {
    if (fDefaultButton != null) {
      return fDefaultButton.getSelection();
    }
    return false;
  }
  
  /**
   * Sets this control to use the 'default' JRE.
   */
  private void setUseDefaultJRE() {
    if (fDefaultDescriptor != null) {
      fDefaultButton.setSelection(true);
      fSpecificButton.setSelection(false);
      fCombo.setEnabled(false);
      firePropertyChange();
    }
  }
  
  /**
   * Sets the title used for this JRE block
   * 
   * @param title title for this JRE block 
   */
  public void setTitle(String title) {
    fTitle = title;
  }

  /**
   * Refresh the default JRE description.
   */
  public void refresh() {
    setDefaultJREDescriptor(fDefaultDescriptor);
  }
  
  /**
   * Returns a classpath container path identifying the selected JRE.
   * 
   * @return classpath container path or <code>null</code>
   * @since 3.2
   */
  public IPath getPath() {
    if (!getStatus().isOK() && fErrorPath != null) {
      return fErrorPath;
    }
    if (fSpecificButton.getSelection()) {
      int index = fCombo.getSelectionIndex();
      if (index >= 0) {
        IVMInstall vm = (IVMInstall) fVMs.get(index);
        return JavaRuntime.newJREContainerPath(vm);
      }
      return null;
    }
    return JavaRuntime.newDefaultJREContainerPath();
  }
  
  /**
   * Sets the selection based on the given container path and returns
   * a status indicating if the selection was successful.
   * 
   * @param containerPath
   * @return status 
   */
  public void setPath(IPath containerPath) {
    fErrorPath = null;
    setStatus(OK_STATUS);
    if (JavaRuntime.newDefaultJREContainerPath().equals(containerPath)) {
      setUseDefaultJRE();
    } else {
      IVMInstall install = JavaRuntime.getVMInstall(containerPath);
      if (install == null) {
        selectJRE(install);
        fErrorPath = containerPath;
        String installTypeId = JavaRuntime.getVMInstallTypeId(containerPath);
        if (installTypeId == null) {
          setError(JREMessages.JREsComboBlock_8);
        } else {
          IVMInstallType installType = JavaRuntime.getVMInstallType(installTypeId);
          if (installType == null) {
            setError(NLS.bind(JREMessages.JREsComboBlock_9, new String[] {installTypeId}));
          } else {
            String installName = JavaRuntime.getVMInstallName(containerPath);
            if (installName == null) {
              setError(NLS.bind(JREMessages.JREsComboBlock_10, new String[] {installType.getName()}));
            } else {
              setError(NLS.bind(JREMessages.JREsComboBlock_11, new String[] {
                  installName, installType.getName()}));
            }
          }
        }
      } else {
        selectJRE(install);
        File location = install.getInstallLocation();
        if (location == null) {
          setError(JREMessages.JREsComboBlock_12);
        } else if (!location.exists()) {
          setError(JREMessages.JREsComboBlock_13);
        }
      }
    }
  }
  
  private void setError(String message) {
    setStatus(new Status(IStatus.ERROR, JDIDebugUIPlugin.getUniqueIdentifier(),
        IJavaDebugUIConstants.INTERNAL_ERROR, message, null));    
  }
  
  /**
   * Returns the status of the JRE selection.
   * 
   * @return status
   */
  public IStatus getStatus() {
    return fStatus;
  }
  
  private void setStatus(IStatus status) {
    fStatus = status;
  }
}
