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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;

/**
 * Invokes the JPA class synchronization action on the Eclipse 4.2 platform.
 * 
 * The name and package of this class must match that declared in the Eclipse 4.3 version of the JPA
 * plugin.
 */
public class SynchronizeClassesRunner implements ISynchronizeClassesRunner {
  @Override
  public void syncClasses(IFile persistenceXml, IProgressMonitor monitor) {
    SynchronizeClassesAction action = new SynchronizeClassesAction();
    ISelection selection = new StructuredSelection(persistenceXml);
    action.selectionChanged(null, selection);
    action.run(null);
  }
}
