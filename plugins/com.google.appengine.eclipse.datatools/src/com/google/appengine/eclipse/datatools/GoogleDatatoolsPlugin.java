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
package com.google.appengine.eclipse.datatools;

import com.google.gdt.eclipse.core.AbstractGooglePlugin;

import org.osgi.framework.BundleContext;

/**
 * The plugin to add DTP support for Google Cloud SQL.
 */
public class GoogleDatatoolsPlugin extends AbstractGooglePlugin {
  private static GoogleDatatoolsPlugin plugin;
  public static GoogleDatatoolsPlugin getDefault() {
    return plugin;
  }
  
  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    plugin = this;
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    plugin = null;
    super.stop(context);
  }
}
