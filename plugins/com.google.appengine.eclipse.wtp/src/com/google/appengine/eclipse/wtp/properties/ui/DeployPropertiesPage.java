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
package com.google.appengine.eclipse.wtp.properties.ui;

import com.google.appengine.eclipse.core.properties.GaeProjectProperties;
import com.google.gdt.eclipse.core.ui.AbstractProjectPropertyPage;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;

/**
 * Base class for deployment project properties.
 */
public abstract class DeployPropertiesPage extends AbstractProjectPropertyPage {

  protected DeployOptionsComponent deployOptionsComponent = new DeployOptionsComponent();
  protected boolean enableJarSplitting;
  protected boolean jarClasses;
  protected boolean retainStagingDir;

  protected void createDeploymentOptionsComponent(Composite composite) {
    deployOptionsComponent.createContents(composite);
    deployOptionsComponent.setSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        enableJarSplitting = deployOptionsComponent.getEnableJarSplittingButton().getSelection();
        jarClasses = deployOptionsComponent.getDoJarClassesButton().getSelection();
        retainStagingDir = deployOptionsComponent.getRetainDirectoryButton().getSelection();
      }
    });
  }

  protected void initializeValues() {
    IProject project = getProject();
    enableJarSplitting = GaeProjectProperties.getGaeEnableJarSplitting(project);
    jarClasses = GaeProjectProperties.getGaeDoJarClasses(project);
    retainStagingDir = GaeProjectProperties.getGaeRetainStagingDir(project);
    deployOptionsComponent.setEnableJarSplittingSelection(enableJarSplitting);
    deployOptionsComponent.setDoJarClassesSelection(jarClasses);
    deployOptionsComponent.setRetainStagingDirSelection(retainStagingDir);
  }

  @Override
  protected void saveProjectProperties() throws Exception {
    IProject project = getProject();
    GaeProjectProperties.setGaeEnableJarSplitting(project, enableJarSplitting);
    GaeProjectProperties.setGaeDoJarClasses(project, jarClasses);
    GaeProjectProperties.setGaeRetaingStagingDir(project, retainStagingDir);
  }
}