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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;

/**
 * TODO: Make this work in Eclipse 3.3 and Eclipse 3.4.
 */
public abstract class AbstractRefactoringTestImpl extends
    AbstractRefactoringTest {
  
  @Override
  protected void move(ICompilationUnit oldCu, IPackageFragment dest)
      throws CoreException {
    // FIXME - If two fragments can see each others contributions then we can enable this by having a fragment that provides the platform specific stuff.
    assertTrue(
        "This test has been disabled because it relies on Eclipse platform-specific code that needs to be fixed in order for this test to run correctly.",
        false);
  }

}
