/*******************************************************************************
 * Copyright 2012 Google Inc. All Rights Reserved.
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
package com.google.appengine.eclipse.webtools.facet.listeners;

import com.google.appengine.eclipse.webtools.facet.JpaFacetHelper;


// Due to code-reorganization in the WTP plugins used for Eclipse 3.7 vs
// Eclipse 3.6 vs Eclipse 3.5, the App Engine WTP plugin needed to be split
// into 3.7, 3.6, and 3.5 versions.
// Whenever you modify this class, please make corresponding changes to the
// 3.5 and 3.6 classes.
public class FacetInstallListener extends AbstractFacetInstallListener {

  @Override
  protected void startJpaListener() {
    if (JpaFacetHelper.areJpaDepsAvailable()) {
      JpaProjectsListener.start();
    }
  }
}
