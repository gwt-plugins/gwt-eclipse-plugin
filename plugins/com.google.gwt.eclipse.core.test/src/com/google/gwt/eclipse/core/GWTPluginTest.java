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
package com.google.gwt.eclipse.core;

import com.google.gwt.eclipse.core.resources.GWTImages;
import com.google.gwt.eclipse.core.test.AbstractGWTPluginTestCase;

import java.lang.reflect.Field;

/**
 * Tests the GWTPlugin class.
 */
public class GWTPluginTest extends AbstractGWTPluginTestCase {

  private String[] imageKeys;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    Field[] imageConstants = GWTImages.class.getFields();
    imageKeys = new String[imageConstants.length];

    // Get all the GWT image keys
    for (int i = 0; i < imageConstants.length; i++) {
      imageKeys[i] = (String) imageConstants[i].get(null);
    }
  }

  public void testGetActiveWorkbenchShell() {
    assertNotNull(GWTPlugin.getActiveWorkbenchShell());
  }

  public void testGetActiveWorkbenchWindow() {
    assertNotNull(GWTPlugin.getActiveWorkbenchWindow());
  }

  public void testGetImage() {
    for (String key : imageKeys) {
      assertNotNull("No image for key: " + key,
          GWTPlugin.getDefault().getImage(key));
    }
  }

  public void testGetImageDescriptor() {
    for (String key : imageKeys) {
      assertNotNull("No image descriptor for key: " + key,
          GWTPlugin.getDefault().getImageDescriptor(key));
    }
  }
}
