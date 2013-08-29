/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.appengine.eclipse.wtp.properties.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

/**
 * UI for setting deployment options
 */
public class DeployOptionsComponent {
  private final class DefaultSelectionListener implements SelectionListener {
    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
      if (listener != null) {
        listener.widgetDefaultSelected(e);
      }
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
      if (listener != null) {
        listener.widgetSelected(e);
      }
    }
  }

  private Button enableJarSplittingButton;
  private Button doJarClassesButton;
  private Button retainDirectoryButton;
  private SelectionListener listener;
  private DefaultSelectionListener defaultListener = new DefaultSelectionListener();
  private Group optionsGroup;

  /**
   * @return the {@link Control} to embed into enclosing UI.
   */
  public Control createContents(Composite parent) {
    optionsGroup = new Group(parent, SWT.NONE);
    optionsGroup.setText("Deployment Options");
    GridLayout layout = new GridLayout();
    optionsGroup.setLayout(layout);
    optionsGroup.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
    {
      enableJarSplittingButton = new Button(optionsGroup, SWT.CHECK);
      enableJarSplittingButton.setText("Enable jar splitting");
      enableJarSplittingButton.addSelectionListener(defaultListener);
    }
    {
      doJarClassesButton = new Button(optionsGroup, SWT.CHECK);
      doJarClassesButton.setText("Package WEB-INF/classes as a jar");
      doJarClassesButton.addSelectionListener(defaultListener);
    }
    {
      retainDirectoryButton = new Button(optionsGroup, SWT.CHECK);
      retainDirectoryButton.setText("Retain staging directory");
      retainDirectoryButton.addSelectionListener(defaultListener);
    }
    return optionsGroup;
  }

  public Button getDoJarClassesButton() {
    return doJarClassesButton;
  }

  public Button getEnableJarSplittingButton() {
    return enableJarSplittingButton;
  }

  public Button getRetainDirectoryButton() {
    return retainDirectoryButton;
  }

  public void setDoJarClassesSelection(boolean selected) {
    doJarClassesButton.setSelection(selected);
  }

  /**
   * Enable/disable the entire component.
   */
  public void setEnabled(boolean enabled) {
    optionsGroup.setEnabled(enabled);
    enableJarSplittingButton.setEnabled(enabled);
    doJarClassesButton.setEnabled(enabled);
    retainDirectoryButton.setEnabled(enabled);
  }

  public void setEnableJarSplittingSelection(boolean selected) {
    enableJarSplittingButton.setSelection(selected);
  }

  public void setRetainStagingDirSelection(boolean selected) {
    retainDirectoryButton.setSelection(selected);
  }

  public void setSelectionListener(SelectionListener listener) {
    this.listener = listener;
  }
}