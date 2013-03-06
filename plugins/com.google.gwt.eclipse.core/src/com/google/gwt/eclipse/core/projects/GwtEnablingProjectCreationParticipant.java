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
package com.google.gwt.eclipse.core.projects;

import com.google.gdt.eclipse.core.projects.IWebAppProjectCreator;
import com.google.gdt.eclipse.core.projects.IWebAppProjectCreator.Participant;
import com.google.gwt.eclipse.core.nature.GWTNature;
import com.google.gwt.eclipse.core.runtime.GWTRuntimeContainer;

/**
 * Sets up GWT on the project being created.
 */
public class GwtEnablingProjectCreationParticipant implements Participant {
  public void updateWebAppProjectCreator(IWebAppProjectCreator creator) {
    creator.addContainerPath(GWTRuntimeContainer.getDefaultRuntimePath());
    creator.addNature(GWTNature.NATURE_ID);
  }
}
