/*******************************************************************************
 * Copyright 2015 Google Inc. All Rights Reserved.
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
package com.google.gwt.eclipse.testing;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

public class GwtTestingPlugin extends Plugin {
  // The plug-in ID
  public static final String PLUGIN_ID = "com.gwtplugins.gwt.eclipse.testing";

  // The shared instance
  private static GwtTestingPlugin instance;

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    instance = this;
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    instance = null;
    super.stop(context);
  }

  /**
   * Returns the activator's {@link Plugin} instance.
   */
  public static GwtTestingPlugin getDefault() {
    return instance;
  }
}
