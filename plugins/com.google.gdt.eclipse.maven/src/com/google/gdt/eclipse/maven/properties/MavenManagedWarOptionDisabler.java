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
package com.google.gdt.eclipse.maven.properties;

import com.google.gdt.eclipse.core.properties.ui.WebappProjectPropertyPage.ManagedWarOptionEnablementFinder;
import com.google.gdt.eclipse.maven.MavenUtils;

import org.eclipse.core.resources.IProject;

/**
 * Disables the ability to change the setting for a managed war folder in the
 * case of a Maven project.
 */
public class MavenManagedWarOptionDisabler implements ManagedWarOptionEnablementFinder {

  public String isManagedWarOptionEnabled(IProject project) {
    if (MavenUtils.hasMavenNature(project)) {
      return "disabled because this is a Maven project";
    }
    return null;
  }
}
