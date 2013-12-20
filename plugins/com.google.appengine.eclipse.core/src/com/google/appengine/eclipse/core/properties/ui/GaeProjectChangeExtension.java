/*******************************************************************************
 * Copyright 2012 Google Inc. All Rights Reserved.
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

package com.google.appengine.eclipse.core.properties.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * The class to be implemented by all the plugins extending
 * com.google.appengine.eclipse.core.appIdChange, to be notified of App Id change of any App Engine
 * Project.
 */
public interface GaeProjectChangeExtension {
  public void gaeProjectRebuilt(IProject appEngineProject, boolean appEngineWebXmlChanged,
      IProgressMonitor monitor) throws Exception;
}
