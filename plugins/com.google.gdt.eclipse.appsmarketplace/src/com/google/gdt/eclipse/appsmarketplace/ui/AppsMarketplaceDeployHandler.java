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
package com.google.gdt.eclipse.appsmarketplace.ui;

import com.google.gdt.eclipse.appsmarketplace.resources.AppsMarketplaceProject;

import org.eclipse.core.commands.ExecutionEvent;

/**
 * If the project does not have apps marketplace support adds support for it. If
 * the project have apps marketplace support list it on apps marketplace.
 */
public class AppsMarketplaceDeployHandler extends AbstractMarketplaceHandler {
  public Object execute(ExecutionEvent event) {
    try {
      executeProjectSelection(event);
      if (AppsMarketplaceProject.isAppsMarketplaceEnabled(project)) {
        executeLogin(event);
        executeListOnMarketplace(event);
      } else {
        executeAddMarketplaceSupport(event, false);
      }
    } catch (AbstractMarketplaceHandlerException e) {
      // Consume Exception. Do nothing.
    }
    return null;
  }
}
