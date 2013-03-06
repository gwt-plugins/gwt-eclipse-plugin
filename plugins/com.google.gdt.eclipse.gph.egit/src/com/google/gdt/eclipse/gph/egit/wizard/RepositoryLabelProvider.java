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
package com.google.gdt.eclipse.gph.egit.wizard;

import com.google.gdt.eclipse.gph.egit.EGitCheckoutProviderPlugin;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * A label provider for git repo locations.
 */
class RepositoryLabelProvider extends LabelProvider {

  /**
   * Create a new RepositoryLabelProvider.
   */
  public RepositoryLabelProvider() {

  }

  @Override
  public Image getImage(Object element) {
    return EGitCheckoutProviderPlugin.getImage("repository_rep.gif");
  }

}
