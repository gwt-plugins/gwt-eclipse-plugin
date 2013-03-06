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
package com.google.gdt.eclipse.gph.extensions;

/**
 * Implementers are notified of lifecycle events during project import via the
 * <code>importcallback</code> extension point.
 */
public interface IImportCallback {

  /**
   * Invoked when the user has clicked the "Finish" button in the Import Hosted
   * Projects wizard.
   * 
   * Note that this event is before the project has been actually checked out.
   * There is no easy way to get notified when a project is done checking out,
   * because we outsource the actual project checkout to 3rd party plugins.
   */
  void onFinish();
}
