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
package com.google.gwt.eclipse.core.refactoring;

import com.google.gwt.eclipse.core.test.AbstractGWTPluginTestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IPackageFragment;

/**
 * Tests the {@link GWTMoveTypeInJsniParticipant} class.
 */
public class GWTMoveTypeInJsniParticipantTest extends
    AbstractRefactoringTestImpl {

  public void testMoveType() throws CoreException {
    IPath destPath = new Path("/"
        + AbstractGWTPluginTestCase.TEST_PROJECT_NAME
        + "/src/"
        + AbstractGWTPluginTestCase.TEST_PROJECT_MODULE_PACKAGE.replace('.',
            '/'));
    IPackageFragment dest = getTestProject().findPackageFragment(destPath);

    // Simulate moving R.java to package com.hello
    assertCompilationUnitMove(rClass.getCompilationUnit(), dest);
  }

  public void testMoveTypeToDefaultPackage() throws CoreException {
    IPath destPath = new Path("/" + AbstractGWTPluginTestCase.TEST_PROJECT_NAME
        + "/src");

    IPackageFragment dest = getTestProject().findPackageFragment(destPath);

    // Simulate moving R.java to default package
    assertCompilationUnitMove(rClass.getCompilationUnit(), dest);
  }

  @Override
  protected void log(String message) {
    System.out.println(message);
  }

  @Override
  protected boolean requiresTestProject() {
    return true;
  }
}
