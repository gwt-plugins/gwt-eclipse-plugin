/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.appengine.eclipse.wtp.maven;

import com.google.appengine.eclipse.wtp.facet.IGaeFacetConstants;

public class Constants {

  // Components of appengine-maven-plugin Maven coordinates:
  public static final String APPENGINE_GROUP_ID = "com.google.appengine";
  public static final String APPENGINE_MAVEN_PLUGIN_ARTIFACT_ID = "appengine-maven-plugin";
  
  // Components of datanucleus-maven-plugin coordinates:
  public static final String DATANUCLEUS_GROUP_ID = "org.datanucleus";
  public static final String DATANUCLEUS_MAVEN_PLUGIN_ARTIFACT_ID = "datanucleus-maven-plugin";
  
  // Facet IDs:
  public static final String GAE_WAR_FACET_ID = IGaeFacetConstants.GAE_FACET_ID;
  public static final String GAE_EAR_FACET_ID = IGaeFacetConstants.GAE_EAR_FACET_ID;
  public static final String JPA_FACET_ID = "jpt.jpa";

}
