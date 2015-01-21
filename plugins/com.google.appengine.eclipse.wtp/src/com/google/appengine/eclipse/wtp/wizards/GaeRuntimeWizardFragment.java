/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.appengine.eclipse.wtp.wizards;

import com.google.appengine.eclipse.core.AppEngineCorePlugin;
import com.google.appengine.eclipse.core.preferences.GaePreferences;
import com.google.appengine.eclipse.core.preferences.ui.GaePreferencePage;
import com.google.appengine.eclipse.core.resources.GaeImages;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.appengine.eclipse.wtp.runtime.GaeRuntime;
import com.google.appengine.eclipse.wtp.utils.PreferencesUtils;
import com.google.common.collect.Lists;
import com.google.gdt.eclipse.core.sdk.Sdk;
import com.google.gdt.eclipse.core.sdk.SdkSet;
import com.google.gdt.eclipse.core.ui.SdkSelectionBlock;
import com.google.gdt.eclipse.core.ui.SdkSelectionBlock.SdkSelectionListener;

import copied.org.eclipse.jdt.internal.debug.ui.jres.JREsComboBlock;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.internal.debug.ui.jres.BuildJREDescriptor;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

import java.util.List;

/**
 * {@link WizardFragment} for configuring Google App Engine.
 */
public final class GaeRuntimeWizardFragment extends WizardFragment {
  /**
   * Select a {@link GaeSdk} from the set of {@link Sdk} known to the workspace.
   */
  private final class GaeServerRuntimeSdkSelectionBlock extends SdkSelectionBlock<GaeSdk> {
    private GaeServerRuntimeSdkSelectionBlock(Composite parent, int style) {
      super(parent, style);

      updateSdkBlockControls();
      initializeSdkComboBox();

      setSelection(-1);
    }

    /**
     * Initializes GaeSdk selection block.
     *
     * @param sdkName
     */
    public void selectSdk(String sdkName) {
      if (sdkName == null) {
        setSelection(-1);
        return;
      }
      SdkSet<GaeSdk> sdks = GaePreferences.getSdkManager().getSdks();
      GaeSdk sdk = sdks.findSdk(sdkName);
      GaeSdk defaultSdk = sdks.getDefault();
      if (sdk != null && sdk.equals(defaultSdk) || sdk == null) {
        setSelection(-1);
      } else {
        List<GaeSdk> specificSdks = doGetSpecificSdks();
        setSelection(specificSdks.indexOf(sdk));
      }
    }

    @Override
    protected void doConfigure() {
      if (PreferencesUtils.showPreferencePage(getShell(),
          "com.google.gdt.eclipse.suite.preferences.ui.googlePreferences", GaePreferencePage.ID)) {
        updateSdkBlockControls();
        validate();
      }
    }

    @Override
    protected GaeSdk doGetDefaultSdk() {
      return GaePreferences.getDefaultSdk();
    }

    @Override
    protected List<GaeSdk> doGetSpecificSdks() {
      return Lists.newArrayList(GaePreferences.getSdks());
    }

  }

  private IWizardHandle wizard;
  private JREsComboBlock jreBlock;
  private GaeServerRuntimeSdkSelectionBlock gaeBlock;
  private GaeRuntime runtime;
  private int status = Status.ERROR;

  /**
   * Default constructor
   */
  public GaeRuntimeWizardFragment() {
    super();
  }

  @Override
  public Composite createComposite(Composite parent, IWizardHandle handle) {
    wizard = handle;
    runtime = getRuntimeDelegate();

    wizard.setTitle(getTitle());
    wizard.setDescription(getDescription());
    wizard.setImageDescriptor(AppEngineCorePlugin.getDefault().getImageDescriptor(
        GaeImages.APP_ENGINE_DEPLOY_LARGE));

    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout());
    createContents(composite);
    return composite;
  }

  @Override
  public void enter() {
    if (runtime == null) {
      setErrorStatus("Runtime delegate is missing or invalid");
      return;
    }
    // initial setup
    selectJRE(runtime.getVMInstall());
    gaeBlock.selectSdk(runtime.getGaeSdkName());
  }

  @Override
  public void exit() {
    if (runtime != null) {
      runtime.getRuntimeWorkingCopy().setName(createName());
    }
    super.exit();
  }

  @Override
  public boolean hasComposite() {
    return true;
  }

  @Override
  public boolean isComplete() {
    if (status == Status.OK) {
      IStatus runtimeStatus = runtime.validate();
      return runtimeStatus != null && runtimeStatus.isOK();
    }
    return false;
  }

  /**
   * Removes any error messages and set status to OK.
   */
  private void clearErrorStatus() {
    wizard.setMessage(null, IMessageProvider.NONE);
    status = Status.OK;
  }

  /**
   * Creates wizard fragment contents.
   *
   * @param <T>
   */
  private <T> void createContents(final Composite composite) {
    {
      Group appEngineGroup = new Group(composite, SWT.NONE);
      appEngineGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      GridLayout glAppEngineGroup = new GridLayout();
      glAppEngineGroup.verticalSpacing = 0;
      glAppEngineGroup.marginWidth = 0;
      appEngineGroup.setLayout(glAppEngineGroup);
      appEngineGroup.setText("Google App Engine SDK");
      gaeBlock = new GaeServerRuntimeSdkSelectionBlock(appEngineGroup, SWT.NONE);
      gaeBlock.addSdkSelectionListener(new SdkSelectionListener() {
        @Override
        public void onSdkSelection(SdkSelectionEvent e) {
          validate();
        }
      });
    }
    {
      jreBlock = new JREsComboBlock(true);
      jreBlock.setDefaultJREDescriptor(new BuildJREDescriptor());
      jreBlock.setTitle("Java Runtime Environment");
      jreBlock.createControl(composite);
      GridData gd = new GridData(GridData.FILL_HORIZONTAL);
      jreBlock.getControl().setLayoutData(gd);
      jreBlock.addPropertyChangeListener(new IPropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent event) {
          validate();
        }
      });
    }
  }

  private String createName() {
    IRuntimeType runtimeType = runtime.getRuntime().getRuntimeType();
    String name = runtimeType.getName() + " (" + runtime.getGaeSdkVersion() + ")";
    IRuntime[] list = ServerCore.getRuntimes();
    int suffix = 1;
    String suffixName = name;
    for (int i = 0; i < list.length; i++) {
      if ((list[i].getName().equals(name) || list[i].getName().equals(suffixName))
          && !list[i].equals(runtime.getRuntime())) {
        suffix++;
      }
      suffixName = name + ' ' + suffix;
    }
    if (suffix > 1) {
      return suffixName;
    }
    return name;
  }

  private String getDescription() {
    return NLS.bind("Define a new {0} runtime", getRuntimeTitle());
  }

  private GaeRuntime getRuntimeDelegate() {
    IRuntimeWorkingCopy wc = (IRuntimeWorkingCopy) getTaskModel().getObject(TaskModel.TASK_RUNTIME);
    if (wc == null) {
      return null;
    }
    return (GaeRuntime) wc.loadAdapter(GaeRuntime.class, new NullProgressMonitor());
  }

  private String getRuntimeTitle() {
    IRuntimeType runtimeType = runtime.getRuntime().getRuntimeType();
    return runtimeType.getName();
  }

  private String getTitle() {
    return NLS.bind("New {0} Runtime", getRuntimeTitle());
  }

  private void selectJRE(IVMInstall vmInstall) {
    boolean isDefault = vmInstall != null
        && vmInstall.getId().equals(JavaRuntime.getDefaultVMInstall().getId());
    if (isDefault) {
      jreBlock.setPath(JavaRuntime.newDefaultJREContainerPath());
    } else {
      if (vmInstall != null) {
        jreBlock.setPath(JavaRuntime.newJREContainerPath(vmInstall));
      }
    }
  }

  /**
   * Sets the wizard error message with {@link IMessageProvider#ERROR} state.
   *
   * @param message a message to set.
   */
  private void setErrorStatus(String message) {
    wizard.setMessage(message, IMessageProvider.ERROR);
    status = Status.ERROR;
  }

  /**
   * Apply and validate selected options.
   */
  private void validate() {
    // no runtime
    if (runtime == null) {
      setErrorStatus("Runtime delegate is missing or invalid");
      return;
    }
    // validate jre
    IStatus jreStatus = jreBlock.getStatus();
    if (jreStatus != null && !jreStatus.isOK()) {
      setErrorStatus(jreStatus.getMessage());
      return;
    }
    if (jreBlock.isDefaultJRE()) {
      runtime.setVMInstall(JavaRuntime.getDefaultVMInstall());
    } else {
      runtime.setVMInstall(jreBlock.getJRE());
    }
    // validate GaeSdk
    GaeSdk selectedSdk = gaeBlock.getSdkSelection().getSelectedSdk();
    if (selectedSdk == null) {
      setErrorStatus("Please configure App Engine SDK");
      return;
    }
    IStatus gaeStatus = selectedSdk.validate();
    if (gaeStatus != null && !gaeStatus.isOK()) {
      setErrorStatus(gaeStatus.getMessage());
      return;
    }
    runtime.getRuntimeWorkingCopy().setLocation(selectedSdk.getInstallationPath());
    runtime.setGaeSdk(selectedSdk);
    // validate runtime
    IStatus runtimeStatus = runtime.validate();
    if (runtimeStatus != null && !runtimeStatus.isOK()) {
      setErrorStatus(runtimeStatus.getMessage());
      return;
    }
    // all ok
    clearErrorStatus();
  }
}
