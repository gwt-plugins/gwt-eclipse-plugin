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

import com.google.gdt.eclipse.core.AdapterUtilities;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;

/**
 * We can't use JavaLaunchableTester since it requires IJavaElements and we only
 * require IFile.
 */
public class GWTJUnitPropertyTester extends PropertyTester {

  /**
   * Property to test if receiver is a GWTTestCase.
   */
  public static final String PROPERTY_IS_GWT_TEST = "isGWTTest";

  private static final String GWT_TEST_CASE = "com.google.gwt.junit.client.GWTTestCase";

  private static final String GWT_TEST_SUITE = "com.google.gwt.junit.tools.GWTTestSuite";

  private static boolean isGWTTestCaseOrSuite(IType type) {
    try {
      ITypeHierarchy hierarchy = type.newSupertypeHierarchy(null);
      IType[] superclasses = hierarchy.getAllSuperclasses(type);
      for (IType superclass : superclasses) {
        if (GWT_TEST_CASE.equals(superclass.getFullyQualifiedName())
            || GWT_TEST_SUITE.equals(superclass.getFullyQualifiedName())) {
          return true;
        }
      }
      return false;
    } catch (CoreException e) {
      GWTPluginLog.logError(e);
      return false;
    }
  }

  // FIXME: This tester only does one test.
  public boolean test(Object receiver, String property, Object[] args,
      Object expectedValue) {
    IJavaElement element = AdapterUtilities.getAdapter(receiver,
        IJavaElement.class);
    if (element == null) {
      return false;
    }

    if (!GWTNature.isGWTProject(element.getJavaProject().getProject())) {
      // Not a GWT project
      return false;
    }

    if (PROPERTY_IS_GWT_TEST.equals(property)) {
      IType testType = null;
      if (element instanceof ICompilationUnit) {
        testType = (((ICompilationUnit) element)).findPrimaryType();
      } else if (element instanceof IClassFile) {
        testType = (((IClassFile) element)).getType();
      } else if (element instanceof IType) {
        testType = (IType) element;
      } else if (element instanceof IMember) {
        testType = ((IMember) element).getDeclaringType();
      }
      return testType != null && isGWTTestCaseOrSuite(testType);
    }
    return false;
  }
}
