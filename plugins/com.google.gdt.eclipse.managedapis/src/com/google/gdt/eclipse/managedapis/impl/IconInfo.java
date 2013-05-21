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
package com.google.gdt.eclipse.managedapis.impl;

/**
 * Class used for getting the location of icons from descriptor.json.
 */
public class IconInfo {

  /**
   * Class that represents "icon_files" in descriptor.json.
   */
  public static class IconFiles {
    private String x32;
    private String x16;

    public String getX16() {
      return x16;
    }

    public String getX32() {
      return x32;
    }
  }

  private IconFiles iconFiles;

  public IconFiles getIconFiles() {
    return iconFiles;
  }
}
