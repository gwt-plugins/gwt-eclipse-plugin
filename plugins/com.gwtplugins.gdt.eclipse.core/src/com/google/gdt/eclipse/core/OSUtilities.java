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
package com.google.gdt.eclipse.core;

/**
 * Provides helper methods for OS identification.
 */
public class OSUtilities {

  public static boolean isMac() {
    String os = System.getProperty("os.name").toLowerCase();

    return os.indexOf("mac") >= 0;
  }

  public static boolean isUnix() {
    String os = System.getProperty("os.name").toLowerCase();

    return os.indexOf("unix") >= 0 || os.indexOf("linux") >= 0;
  }

  public static boolean isWindows() {
    String os = System.getProperty("os.name").toLowerCase();

    return os.indexOf("win") >= 0;
  }

  private OSUtilities() {
  }

}
