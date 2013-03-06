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
package com.google.gwt.eclipse.core.refactoring.regionupdater;

import com.google.gwt.eclipse.core.GWTPluginLog;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A reference updater that is knowledgeable about a compilation unit rename.
 */
public class CompilationUnitRenamedReferenceUpdater extends ReferenceUpdater {

  private final String oldElementName;
  private final String newElementName;

  private final String oldCuName;
  private final String newCuName;

  public CompilationUnitRenamedReferenceUpdater(String oldElementName,
      String newElementName, String oldCompilationUnitName,
      String newCompilationUnitName) {
    this.oldElementName = oldElementName;
    this.newElementName = newElementName;
    this.oldCuName = oldCompilationUnitName;
    this.newCuName = newCompilationUnitName;
  }

  @Override
  public String getUpdatedBindingKey(String bindingKey) {
    // Pad the binding key, which will simplify our regex
    String paddedBindingKey = "@" + bindingKey + "@";

    // In a binding key, non-inner class names are preceded by either 'L' if it
    // is in the default package or '/' if it is in any other package, and
    // followed by ';'
    Pattern pattern = Pattern.compile(String.format("([L/])%s(;)",
        Pattern.quote(oldElementName)));
    Matcher matcher = pattern.matcher(paddedBindingKey);
    // Replace with the new element name, copy back the character before and
    // after
    String newPaddedBindingKey = matcher.replaceAll(String.format("$1%s$2",
        Matcher.quoteReplacement(newElementName)));

    // Remove the padding
    return newPaddedBindingKey.substring(1, newPaddedBindingKey.length() - 1);
  }

  @Override
  public ICompilationUnit getUpdatedCompilationUnit(
      ICompilationUnit compilationUnit) {
    if (!compilationUnit.getElementName().equals(oldCuName)) {
      return compilationUnit;
    }

    // Sanity check
    if (!(compilationUnit.getParent() instanceof IPackageFragment)) {
      GWTPluginLog.logWarning(String.format(
          "The parent of %s is not a package fragment (it is %s of type %s).",
          compilationUnit.getElementName(),
          compilationUnit.getParent().getElementName(),
          compilationUnit.getParent().getClass().getSimpleName()));
      return compilationUnit;
    }

    IPackageFragment pkgFragment = (IPackageFragment) compilationUnit.getParent();
    ICompilationUnit newCu = pkgFragment.getCompilationUnit(newCuName);
    return newCu;
  }

}
