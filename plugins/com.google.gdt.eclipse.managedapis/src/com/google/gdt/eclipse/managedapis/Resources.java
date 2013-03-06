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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * An interface defining access to SWT resource accessors.
 */
public interface Resources {

  Image getAPIDefaultIcon32Image() throws MalformedURLException;

  Image getAPIIconForUrl(URL url) throws MalformedURLException;

  Color getColorDisabled();

  Display getDisplay();

  ImageDescriptor getGoogleAPIContainerIcon16ImageDescriptor()
      throws MalformedURLException;

  Image getInfoIcon16Image() throws MalformedURLException;

  ImageDescriptor getManagedApiImportIcon() throws MalformedURLException;

  LocalResourceManager getResourceManager();

  Font getSmallHeaderFont();

  ImageDescriptor getUpdateAvailableOverlay16ImageDescriptor()
      throws MalformedURLException;

}