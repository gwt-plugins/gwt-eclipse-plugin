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
package com.google.gdt.eclipse.gph.wizards;

import com.google.gdt.eclipse.gph.model.GPHProject;
import com.google.gdt.eclipse.gph.providers.ScmProvider;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardNode;
import org.eclipse.swt.graphics.Point;

/**
 * A wizard node to launch the ProvisionSCMProviderWizard wizard.
 */
public class ProvisionSCMProviderNode implements IWizardNode {
  private GPHProject project;

  private ScmProvider provider;

  private ProvisionSCMProviderWizard wizard;

  public ProvisionSCMProviderNode(ScmProvider provider, GPHProject project) {
    this.provider = provider;
    this.project = project;
  }

  public void dispose() {
  }

  public Point getExtent() {
    // unknown
    return new Point(-1, -1);
  }

  public IWizard getWizard() {
    if (wizard == null) {
      wizard = new ProvisionSCMProviderWizard(provider, project);
    }

    return wizard;
  }

  public boolean isContentCreated() {
    return wizard != null;
  }

}
