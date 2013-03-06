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
package com.google.gdt.eclipse.managedapis;

import org.eclipse.osgi.util.NLS;

/**
 * NLS interface to messages used to localize the plugin.
 */
public class Messages extends NLS {
  private static final String BUNDLE_NAME = "com.google.gdt.eclipse.managedapis.ui.messages"; //$NON-NLS-1$

  public static String Browse;

  public static String ClasspathContainerDisplayName;

  public static String DirErr;

  public static String DirLabel;

  public static String DirSelect;

  public static String ExtErr;

  public static String ExtLabel;

  public static String InvalidContainer;

  public static String MalformedUrl;

  public static String PageDesc;

  public static String PageName;

  public static String PageTitle;

  public static String UnexpectedException;

  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
