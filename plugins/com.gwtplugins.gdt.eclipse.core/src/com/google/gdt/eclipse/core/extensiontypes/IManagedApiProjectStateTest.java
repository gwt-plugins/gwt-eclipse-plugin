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
package com.google.gdt.eclipse.core.extensiontypes;

import org.eclipse.core.resources.IProject;

/**
 * Interface provides mechanism to test whether the Managed API functionality
 * should be activated -- generally due to potential problems with compatibility
 * with other plugins.
 */
public interface IManagedApiProjectStateTest {
  public boolean isValidToAddManagedApiProjectState(IProject project);
}
