/*******************************************************************************
 * Copyright 2012 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.cloudsdk.eclipse.wtp.ui;

import com.google.cloudsdk.eclipse.wtp.CloudSdkPlugin;
import com.google.cloudsdk.eclipse.wtp.runtime.CloudSdkRuntime;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

import javax.swing.SwingWorker;

/**
 * {@link WizardFragment} for configuring Google Cloud SDK Runtime.
 */
public class CloudSdkRuntimeWizardFragment extends WizardFragment {
  private IWizardHandle wizard;
  private CloudSdkRuntime runtime;
  private Text dirTextBox;
  private int status = Status.ERROR;

  @Override
  public Composite createComposite(Composite parent, IWizardHandle handle) {
    wizard = handle;
    runtime = getRuntimeDelegate();

    String title = getRuntimeTitle();
    wizard.setTitle("New " + title + " Runtime");
    wizard.setDescription("Define a new " + title + " runtime");

    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout());
    createContents(composite);
    return composite;
  }

  @Override
  public boolean hasComposite() {
    return true;
  }

  @Override
  public boolean isComplete() {
    if (status == Status.OK) {
      return true;
    }
    return false;
  }

  private void createContents(final Composite composite) {
    Group group = new Group(composite, SWT.NONE);
    group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    GridLayout layout = new GridLayout(6, true);
    group.setLayout(layout);
    group.setText("Google Cloud SDK");

    new Label(group, SWT.NONE).setText("SDK Directory:");

    dirTextBox = new Text(group, SWT.BORDER);
    GridData data = new GridData(GridData.FILL_HORIZONTAL);
    data.horizontalSpan = 4;
    dirTextBox.setLayoutData(data);
    dirTextBox.addModifyListener(new ModifyListener(){
        @Override
        public void modifyText(ModifyEvent e) {
          validate();
        }
    });

    Button button = new Button(group, SWT.PUSH);
    button.setText("&Browse...");
    button.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent event) {
        DirectoryDialog dlg = new DirectoryDialog(composite.getShell(), SWT.OPEN);
        dlg.setText("Cloud SDK's Directory");
        dlg.setMessage("Select a directory");

        // It will return the selected directory, or
        // null if user cancels
        String dir = dlg.open();
        if (dir != null) {
          dirTextBox.setText(dir);
        }
      }
    });
  }

  private CloudSdkRuntime getRuntimeDelegate() {
    IRuntimeWorkingCopy wc = (IRuntimeWorkingCopy) getTaskModel().getObject(TaskModel.TASK_RUNTIME);
    if (wc == null) {
      return null;
    }
    return (CloudSdkRuntime) wc.loadAdapter(CloudSdkRuntime.class, new NullProgressMonitor());
  }

  private String getRuntimeTitle() {
    IRuntimeType runtimeType = runtime.getRuntime().getRuntimeType();
    return runtimeType.getName();
  }

  private void updateStatus(String message, int currentStatus) {
    switch (currentStatus) {
      case Status.ERROR:
        wizard.setMessage(message, IMessageProvider.ERROR);
        status = Status.ERROR;
        break;
      case Status.OK:
        wizard.setMessage(null, IMessageProvider.NONE);
        status = Status.OK;
        break;
      case Status.INFO:
        wizard.setMessage(message, IMessageProvider.INFORMATION);
        status = Status.INFO;
        break;
      default:
        wizard.setMessage(message, IMessageProvider.ERROR);
        status = Status.ERROR;
        break;
    }
  }

  private void validate() {
    if (runtime == null) {
      updateStatus("Runtime delegate is missing or invalid", Status.ERROR);
      return;
    }

    updateStatus("Validating...", Status.INFO);
    Path path = new Path (dirTextBox.getText());
    runtime.getRuntimeWorkingCopy().setLocation(path);

    SwingWorker<IStatus, Void> worker = new SwingWorker<IStatus, Void>() {
      @Override
      protected IStatus doInBackground() throws Exception {
        return runtime.validate();
      }

      @Override
      public void done() {
        final IStatus runtimeStatus;
        try {
          runtimeStatus = get();
        } catch (Exception e) {
          CloudSdkPlugin.logError(e);
          return;
        }

        Display.getDefault().asyncExec(new Runnable() {
          @Override
          public void run() {
            if (runtimeStatus != null && !runtimeStatus.isOK()) {
              updateStatus(runtimeStatus.getMessage(), Status.ERROR);
            } else {
              updateStatus(null, Status.OK);
            }
            wizard.update();
          }
        });
      }
    };
    worker.execute();
  }
}