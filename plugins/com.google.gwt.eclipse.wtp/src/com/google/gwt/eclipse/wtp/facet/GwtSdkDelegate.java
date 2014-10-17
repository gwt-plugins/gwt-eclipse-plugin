/*******************************************************************************
 * Copyright 2014 Google Inc. All Rights Reserved.
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
package com.google.gwt.eclipse.wtp.facet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

public final class GwtSdkDelegate implements IDelegate {

  @Override
  public void execute(final IProject pj, final IProjectFacetVersion fv,
      final Object config, final IProgressMonitor monitor) throws CoreException {
    monitor.beginTask("", 2);
    try {
      // final IFolder webInfLib = Utils.getWebInfLibDir(pj);
      //
      // Utils.copyFromPlugin(new Path("libs/formgen-core.jar"),
      // webInfLib.getFile("formgen-core.jar"));
      //
      // monitor.worked(1);
      //
      // Utils.registerFormGenServlet(pj);
      //
      // monitor.worked(1);

    } finally {
      monitor.done();
    }
  }

}
