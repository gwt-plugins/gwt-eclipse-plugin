/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.appengine.eclipse.wtp.jpa;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jpt.jpa.core.JpaProject;

/**
 * Executes an action that synchronizes JPA-annotated classes with a persistence.xml file.
 *
 * TODO (rdayal): Should the method throw an exception?
 */
public interface ISynchronizeClassesRunner {

  /**
   * Executes the JPA synchronize classes action.
   *
   * @param the JPA project to search for persistence.xml file
   * @param monitor a progress monitor instance.
   */
  void synchronizeClasses(JpaProject jpaProject, IProgressMonitor monitor);
}
