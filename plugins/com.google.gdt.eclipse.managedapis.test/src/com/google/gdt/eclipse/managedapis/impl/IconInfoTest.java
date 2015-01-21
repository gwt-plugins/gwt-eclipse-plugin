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
package com.google.gdt.eclipse.managedapis.impl;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import junit.framework.TestCase;

/**
 * Tests that the IconInfo class is a correct representation of the serialized descriptor.json's
 * "icon_files" member.
 */
public class IconInfoTest extends TestCase {
  private String jsonStr =
      "{\"icon_files\": {\"x32\": \"icons/icon-32.gif\", \"x16\": \"icons/icon-16.gif\"}}";

  public void testIconInfoCreation() {
    Gson gson = new GsonBuilder().setFieldNamingPolicy(
        FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    IconInfo iconInfo = gson.fromJson(jsonStr, IconInfo.class);

    assertEquals("icons/icon-32.gif", iconInfo.getIconFiles().getX32());
    assertEquals("icons/icon-16.gif", iconInfo.getIconFiles().getX16());
  }
}
