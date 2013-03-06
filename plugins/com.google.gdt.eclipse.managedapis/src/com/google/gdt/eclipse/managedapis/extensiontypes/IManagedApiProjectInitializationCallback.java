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
package com.google.gdt.eclipse.managedapis.extensiontypes;

import com.google.gdt.eclipse.managedapis.ManagedApiProject;

/**
 * Used by the managedApiProjectInitializationCallback extension point to define
 * units of work that should be called during the initialization of
 * ManagedApiProjects. The managed API project calls the onInitialization()
 * method of registered extensions as part of the initialization process. This
 * callback is typically used to register a ManagedApiProjectObserver on a
 * project to capture additional life-cycle events.
 */
public interface IManagedApiProjectInitializationCallback {

  /**
   * The managed API project calls the onInitialization during initialization.
   * 
   * @param project
   */
  public void onInitialization(ManagedApiProject project);
}
