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
package com.google.gwt.eclipse.wtp.facet.data;

import com.google.gwt.eclipse.wtp.GwtWtpPlugin;

/**
 * Keeper for GWT constants.
 */
public interface IGwtFacetConstants {
  
  /**
   * GWT FACET
   */
  static final String GWT_PLUGINS_FACET_ID = "com.gwtplugins.gwt.facet";

  static final String GWT_SDK = GWT_PLUGINS_FACET_ID + ".property.sdk";

  static final String LAUNCHER_ID = GWT_PLUGINS_FACET_ID + ".launcher.id";
  
  /**
   * Used in the data model to mark as maven project
   */
  public static final String USE_MAVEN_DEPS_PROPERTY_NAME = GwtWtpPlugin.PLUGIN_ID + ".useMavenDependencies";

}
