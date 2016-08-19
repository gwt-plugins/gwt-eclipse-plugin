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
package com.google.gwt.eclipse.core.launch;

import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.launch.ILaunchShortcutStrategy;
import com.google.gwt.eclipse.core.test.AbstractGWTPluginTestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;

import java.io.UnsupportedEncodingException;

/**
 * Test the implementations of ILaunchShortcutStrategy.
 */
public class LaunchShortcutStrategyTest extends AbstractGWTPluginTestCase {
  public void testWebAppLaunchShortcutStrategy() throws CoreException,
      UnsupportedEncodingException {
    IJavaProject javaProject = getTestProject();
    ILaunchShortcutStrategy strategy = new WebAppLaunchShortcutStrategy();

    WebAppUtilities.verifyIsWebApp(javaProject.getProject());
    IFolder warFolder = WebAppUtilities.getWarSrc(javaProject.getProject());

    IResource selection = warFolder.getFile("Hello.html");
    String url = strategy.generateUrl(selection, false);
    assertTrue(url.equals("Hello.html"));

    // Now we'll add a file in a subdirectory of war.
    IFolder warSubDir = warFolder.getFolder("subdir");
    warSubDir.create(true, true, null);
    IFile otherHello = warSubDir.getFile("OtherHello.html");
    ResourceUtils.createFile(otherHello.getFullPath(), "(other hello)");

    String otherUrl = strategy.generateUrl(otherHello, false);
    assertTrue(otherUrl.equals("subdir/OtherHello.html"));
  }

  @Override
  protected boolean requiresTestProject() {
    return true;
  }
}
