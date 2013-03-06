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
package com.google.gdt.eclipse.core.reference;

import com.google.gdt.eclipse.core.reference.location.IReferenceLocation;

import org.eclipse.core.resources.IProject;

/*
 * TODO: This is not currently needed, but we will need to track what type of
 * reference this is. For example, refactoring will need to know whether the
 * source should be renamed if the target is, whereas validation only needs to
 * know when the target is touched.
 */
/**
 * Models a reference from one location (source) to another location (target).
 * <p>
 * Note: It is the job of {@link IReference}-implementors to
 * {@link IReferenceLocation#setReference(IReference)} on contained
 * {@link IReferenceLocation}s.
 */
public interface IReference {
  IReferenceLocation getSourceLocation();

  /**
   * Returns the project corresponding to the source location.
   */
  IProject getSourceProject();

  IReferenceLocation getTargetLocation();
}
