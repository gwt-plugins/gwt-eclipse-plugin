/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
 * 
 *  All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gdt.eclipse.managedapis;

import org.eclipse.core.resources.IFolder;

/**
 * Observer of ManagedApiProject events. Register with a ManagedApiProject to
 * get notification of events.
 */
public interface ManagedApiProjectObserver {
  void addManagedApis(ManagedApi[] api);

  void changeCopyToDirectory(IFolder originalFolder, IFolder newFolder);

  void refreshManagedApis(ManagedApi[] api);

  void removeManagedApis(ManagedApi[] api);
}
