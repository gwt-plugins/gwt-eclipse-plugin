/*******************************************************************************
 * Copyright 2014 Google Inc. All Rights Reserved.
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
package com.google.gwt.eclipse.wtp.maven;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.wtp.facets.AbstractFacetDetector;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

import java.util.Map;

public class GwtFacetDetector extends AbstractFacetDetector {

  /**
   * Not applicable, it will always use version 1.
   */
  @Override
  public IProjectFacetVersion findFacetVersion(IMavenProjectFacade arg0, Map<?, ?> arg1, IProgressMonitor arg2)
      throws CoreException {

    return null;
  }

}
