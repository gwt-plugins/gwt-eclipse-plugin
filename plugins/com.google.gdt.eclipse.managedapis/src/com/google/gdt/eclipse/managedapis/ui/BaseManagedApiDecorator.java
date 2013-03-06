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
package com.google.gdt.eclipse.managedapis.ui;

import com.google.gdt.eclipse.core.ClasspathUtilities;
import com.google.gdt.eclipse.managedapis.ManagedApi;
import com.google.gdt.eclipse.managedapis.ManagedApiLogger;
import com.google.gdt.eclipse.managedapis.impl.ManagedApiProjectImpl;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.internal.core.JarPackageFragmentRoot;

/**
 * Provide common methods for decorator classes.
 */
@SuppressWarnings("restriction")
public abstract class BaseManagedApiDecorator {
  protected ManagedApi getManagedApiForPackageFragmentRoot(
      JarPackageFragmentRoot packageFragmentRoot) {
    ManagedApi managedApi = null;
    try {
      IClasspathEntry icpe = ClasspathUtilities.getNullableRawClasspathEntryForPackageFragmentRoot(packageFragmentRoot);
      managedApi = ManagedApiProjectImpl.getManagedApiProject(
          packageFragmentRoot.getJavaProject()).findManagedApi(
          icpe.getPath().removeFirstSegments(1).toString());
    } catch (CoreException e) {
      ManagedApiLogger.warn(e,
          "Caught core exception while trying to access ManagedApi");
    }
    return managedApi;
  }

}
