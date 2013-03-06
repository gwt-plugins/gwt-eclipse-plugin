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
package com.google.gdt.eclipse.core.sdk;

import com.google.gdt.eclipse.core.JavaProjectUtilities;
import com.google.gdt.eclipse.core.sdk.SdkClasspathContainer.Type;
import com.google.gdt.eclipse.core.ui.SdkSelectionBlock;
import com.google.gdt.eclipse.core.ui.SdkSelectionBlock.SdkSelection;

import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.dialogs.PreferencesUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays a WizardPage with an {@link SdkSelectionBlock} whenever an Sdk's
 * classpath container is edited via the Java Build Path controls.
 * 
 * This is an abstract class. Subclasses are meant to do the following:
 * 
 * 1) In the constructor, after delegating to the superclass' constructor, set
 * the message to be displayed in the WizardPage's header
 * 
 * 2) If there is any SDK-specific validation that needs to be performed,
 * override the {@link #validateJavaProjectAndSelectedSdk} method. More
 * information can be found in this method's javadoc.
 * 
 * @param <T> the type of {@link Sdk} for which to display the selection block
 */
@SuppressWarnings("restriction")
public abstract class SdkClasspathContainerPage<T extends Sdk> extends
    WizardPage implements IClasspathContainerPage,
    IClasspathContainerPageExtension {

  private abstract static class ClasspathContainerPageSdkSelectionBlock<T extends Sdk>
      extends SdkSelectionBlock<T> {
    private ClasspathContainerPageSdkSelectionBlock(Composite parent, int style) {
      super(parent, style);
    }

    @Override
    public void setSelection(int selection) {
      initializeSdkComboBox();
      updateSdkBlockControls();

      super.setSelection(selection);
    }
  }

  protected IJavaProject javaProject = null;
  private ClasspathContainerPageSdkSelectionBlock<T> sdkSelectionBlock;
  private SdkManager<T> sdkManager;
  private String containerID;
  private String sdkPreferencePageID;
  private IClasspathEntry containerEntry;

  /**
   * Constructor for SdkClasspathContainerPage. Subclasses must delegate to this
   * constructor.
   * 
   * @param pageName the name of the page
   * @param title the title for this wizard page, or <code>null</code> if none
   * @param titleImage the image descriptor for the title of this wizard page,
   *          or <code>null</code> if none
   * @param sdkManager an SDK Manager, which can be used to resolve SDK
   *          Container Paths to SDKs
   * @param containerID the container ID of the classpath container
   * @param sdkPreferencePageID the ID of the Workspace SDK Preference page that
   *          should be opened when the "Configure SDKs.." link in the
   *          SdkSelectionBlock is selected
   */
  protected SdkClasspathContainerPage(String pageName, String title,
      ImageDescriptor titleImage, SdkManager<T> sdkManager, String containerID,
      String sdkPreferencePageID) {
    super(pageName, title, titleImage);
    this.sdkManager = sdkManager;
    this.containerID = containerID;
    this.sdkPreferencePageID = sdkPreferencePageID;
  }

  public final void createControl(Composite parent) {
    Composite panel = new Composite(parent, SWT.NONE);
    final GridLayout gridLayout = new GridLayout();
    gridLayout.marginWidth = 10;
    gridLayout.marginHeight = 10;
    panel.setLayout(gridLayout);
    panel.setFont(parent.getFont());
    panel.setLayoutData(new GridData(GridData.FILL_BOTH));

    Group group = SWTFactory.createGroup(panel, this.getTitle(), 1, 1,
        GridData.FILL_HORIZONTAL);

    sdkSelectionBlock = new ClasspathContainerPageSdkSelectionBlock<T>(group,
        SWT.NONE) {
      @Override
      protected void doConfigure() {
        if (Window.OK == PreferencesUtil.createPreferenceDialogOn(getShell(),
            sdkPreferencePageID, new String[] {sdkPreferencePageID}, null).open()) {
          SdkClasspathContainerPage.this.validateJavaProjectAndSelectedSdk();
        }
      }

      @Override
      protected T doGetDefaultSdk() {
        return sdkManager.findSdkForPath(SdkClasspathContainer.computeDefaultContainerPath(containerID));
      }

      @Override
      protected List<T> doGetSpecificSdks() {
        return new ArrayList<T>(sdkManager.getSdks());
      }
    };

    sdkSelectionBlock.addSdkSelectionListener(new SdkSelectionBlock.SdkSelectionListener() {
      public void onSdkSelection(SdkSelectionEvent e) {
        validateJavaProjectAndSelectedSdk();
      }
    });

    setControl(panel);

    // Need to set the title and message from here to correctly initialize
    setTitle(getTitle());
    setMessage("Select an SDK to add to the classpath.");

    int selection = -1;
    if (containerEntry != null
        && !SdkClasspathContainer.isDefaultContainerPath(containerID,
            containerEntry.getPath())) {
      T sdkForPath = sdkManager.findSdkForPath(containerEntry.getPath());
      if (sdkForPath != null) {
        selection = new ArrayList<T>(sdkManager.getSdks()).indexOf(sdkForPath);
      }
    }

    sdkSelectionBlock.setSelection(selection);
  }

  public final boolean finish() {
    return true;
  }

  public final IClasspathEntry getSelection() {
    SdkSelection<T> sdkSelection = sdkSelectionBlock.getSdkSelection();
    IPath containerPath = SdkClasspathContainer.computeContainerPath(
        containerID, sdkSelection.getSelectedSdk(), sdkSelection.isDefault()
            ? Type.DEFAULT : Type.NAMED);
    return JavaCore.newContainerEntry(containerPath);
  }

  public final void initialize(IJavaProject project,
      IClasspathEntry[] currentEntries) {
    this.javaProject = project;
  }

  public final void setSelection(IClasspathEntry containerEntry) {
    this.containerEntry = containerEntry;
  }

  /**
   * Validates that the Java project, passed in via the initialize method,
   * exists, and that the SDK that the user has selected in the
   * SdkSelectionBlock exists.
   * 
   * Subclasses may override this method if they want to contribute their own
   * SDK-specific validations. When overriding this method, implementations
   * should first delegate to the superclass' implementation. If the returned
   * SDK from the call to the superclass' implementation, then the subclass
   * implementation should not perform any additional validation, and return
   * <code>null</code>.
   * 
   * If the SDK returned from the superclass' implementation is non-null, then
   * subclass implementations can now perform their own validations. In the
   * event that an error is detected, the subclass' implementation should do the
   * following:
   * 
   * <pre>
   * setErrorMessage("<Specific Error Message>"); 
   * setPageComplete(false);
   * return null;
   * </pre>
   * 
   * If no errors occur in the subclass validation, then the selected SDK should
   * be returned.
   * 
   * Note that subclass implementations should NEVER set the error message to
   * <code>null</code> or set the page completion value to <code>true</code>.
   * This is done at the top of the superclass' implementation, before any sort
   * of validation takes place, so there is no need to clear this state.
   * 
   * @return the SDK that the user selected in the SdkSelectionBlock, provided
   *         that it is valid, and the Java project associated with the SDK
   *         exists
   */
  protected T validateJavaProjectAndSelectedSdk() {

    setErrorMessage(null);
    setPageComplete(true);

    // Verify that the java project whose classpath is being edited actually
    // exists.
    if (!JavaProjectUtilities.isJavaProjectNonNullAndExists(javaProject)) {
      // When can this actually happen?
      setErrorMessage("The Java project for this classpath entry does not exist.");
      setPageComplete(false);
      return null;
    }

    // Verify that the currently selected SDK actually exists, and is valid.
    SdkSelection<T> sdkSelection = sdkSelectionBlock.getSdkSelection();
    T selectedSdk = sdkSelection == null ? null : sdkSelection.getSelectedSdk();
    if (selectedSdk == null || !selectedSdk.validate().isOK()) {
      setErrorMessage("The selected SDK is invalid.");
      setPageComplete(false);
      return null;
    }

    return selectedSdk;
  }
}
