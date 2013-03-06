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
package com.google.gdt.eclipse.core.reference.location;

import com.google.gdt.eclipse.core.reference.IReference;

/**
 * Models a location of a reference.
 */
public interface IReferenceLocation {

  /**
   * Returns the reference that this location is contained within.
   */
  IReference getReference();

  /**
   * Sets the reference that this location is contained within.
   * <p>
   * It is the job of the {@link IReference} implementation to set this when the
   * location is added.
   */
  void setReference(IReference reference);
}
