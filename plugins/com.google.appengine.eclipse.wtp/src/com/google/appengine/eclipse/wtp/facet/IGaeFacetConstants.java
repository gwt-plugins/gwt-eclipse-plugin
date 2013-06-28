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
package com.google.appengine.eclipse.wtp.facet;

/**
 * Keeper for GAE constants.
 */
public interface IGaeFacetConstants {
  static final String GAE_FACET_ID = "com.google.appengine.facet";
  static final String GAE_PROPERTY_APP_ID = GAE_FACET_ID + ".property.app.id";
  static final String GAE_PROPERTY_APP_VERSION = GAE_FACET_ID + ".property.app.version";
  static final String GAE_PROPERTY_CREATE_SAMPLE = GAE_FACET_ID + ".property.createSample";
  static final String GAE_PROPERTY_PACKAGE = GAE_FACET_ID + ".property.package";

  static final String GAE_EAR_FACET_ID = "com.google.appengine.facet.ear";

  static final String DEFAULT_PACKAGE_NAME = "com.example.myproject";
}
