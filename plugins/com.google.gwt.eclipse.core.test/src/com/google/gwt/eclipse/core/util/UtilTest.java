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
package com.google.gwt.eclipse.core.util;

import com.google.gwt.eclipse.core.test.AbstractGWTPluginTestCase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;

import java.text.MessageFormat;

/**
 * Test cases for the {@link Util} class.
 */
public class UtilTest extends AbstractGWTPluginTestCase {

  public void testGetProject() {
    String entryPointPath = MessageFormat.format("/{0}/src/{1}/{2}.java",
        AbstractGWTPluginTestCase.TEST_PROJECT_NAME,
        AbstractGWTPluginTestCase.TEST_PROJECT_SRC_PACKAGE.replace('.', '/'),
        AbstractGWTPluginTestCase.TEST_PROJECT_ENTRY_POINT);

    // Test valid file in project
    IProject project = Util.getProject(new Path(entryPointPath));
    assertEquals(AbstractGWTPluginTestCase.TEST_PROJECT_NAME, project.getName());

    // Test non-existent file in valid project
    assertNull(Util.getProject(new Path(
        AbstractGWTPluginTestCase.TEST_PROJECT_NAME
            + "/src/com/hello/Hello.java")));

    // Test non-existent project
    assertNull(Util.getProject(new Path(
        "/NonExistentProject/src/com/hello/Hello.java")));
  }

  public void testGetWorkspaceRoot() {
    assertNotNull(Util.getWorkspaceRoot());
  }

  @Override
  protected boolean requiresTestProject() {
    return true;
  }

}
