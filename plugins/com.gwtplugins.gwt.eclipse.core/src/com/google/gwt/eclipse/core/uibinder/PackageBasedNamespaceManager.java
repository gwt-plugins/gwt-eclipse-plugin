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
package com.google.gwt.eclipse.core.uibinder;

import com.google.gdt.eclipse.core.collections.OneToManyIndex;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.Set;

/**
 * Manages the Java package to UiBinder XML namespace mappings. Its primary use
 * is to allow clients to re-use the namespace prefix for already imported
 * packages, and ensure there isn't a naming clash for new prefixes.
 * <p>
 * The typical usage is to maintain a short-lived instance since there is no
 * cleanup code. For example, the UiBinder XML template parser generates this
 * once in the beginning and reuses it for the duration of the parse, then
 * throws it away.
 */
public class PackageBasedNamespaceManager {

  /**
   * Generates a new prefix, ideally in some human-understandable sequence.
   */
  private class ArbitraryPrefixGenerator {

    private int nextPrefix = 0;

    /**
     * Computes an unused prefix.
     * 
     * @return an unused prefix
     * @throws RuntimeException if there are no more prefixes available in this
     *           function's search space
     */
    String computePrefix() {
      while (packageNameToPrefix.hasElement("ns" + ++nextPrefix)) {
      }

      return "ns" + nextPrefix;
    }
  }

  private static int getLastUncommonFragmentIndex(String[] fragments) {
    for (int i = fragments.length - 1; i >= 0; i--) {
      if (!isCommonFragment(fragments[i])) {
        return i;
      }
    }

    return -1;
  }

  /**
   * Returns standard prefixes used for common packages. For example, the GWT
   * user library's UI package uses the "g" prefix in sample code.
   * 
   * @return the preferred prefix, or null if this isn't a standard package name
   */
  private static String getStandardPrefix(String packageName) {
    if (packageName.equals(UiBinderConstants.GWT_USER_LIBRARY_UI_PACKAGE_NAME)) {
      return UiBinderConstants.GWT_USER_LIBRARY_UI_NAMESPACE_PREFIX;
    }

    return null;
  }

  private static boolean isCommonFragment(String fragment) {
    return fragment.equals("client") || fragment.equals("ui")
        || fragment.equals("widget") || fragment.equals("widgets");
  }

  private final OneToManyIndex<String, String> packageNameToPrefix = new OneToManyIndex<String, String>();

  private final ArbitraryPrefixGenerator arbitraryPrefixGenerator = new ArbitraryPrefixGenerator();

  /**
   * Returns the set of package names.
   * 
   * @return the set of package names, do not modify the contents
   */
  public Set<String> getImportedPackageNames() {
    return packageNameToPrefix.keys();
  }

  /**
   * Returns the package name that maps to the prefix, or null.
   */
  public String getPackageName(String prefix) {
    Set<String> packageNames = packageNameToPrefix.getKeys(prefix);
    assert packageNames.size() <= 1;
    return packageNames.size() == 0 ? null : packageNames.iterator().next();
  }

  /**
   * Gets the prefix for an already imported package name.
   * 
   * @param packageName the Java package name
   * @return the prefix for an already imported package name, or null if the
   *         package name has not been imported
   */
  public String getPrefix(String packageName) {
    Set<String> elements = packageNameToPrefix.getElements(packageName);
    return elements.size() > 0 ? elements.iterator().next() : null;
  }

  /**
   * Keeps track of all of the already imported XML namespaces on the given root
   * element.
   * 
   * @param rootElement the root element which contains the namespace definition
   *          attributes.
   */
  public void readFromElement(Element rootElement) {
    NamedNodeMap attributes = rootElement.getAttributes();
    for (int i = 0; i < attributes.getLength(); i++) {
      Node item = attributes.item(i);

      String namespace = item.getNamespaceURI();
      if (namespace == null
          || !namespace.equals(UiBinderConstants.XMLNS_NAMESPACE)) {
        continue;
      }

      String value = item.getNodeValue();
      if (value == null
          || !value.startsWith(UiBinderConstants.URN_IMPORT_NAMESPACE_BEGINNING)) {
        continue;
      }

      String packageName = value.substring(UiBinderConstants.URN_IMPORT_NAMESPACE_BEGINNING.length());
      String nodeName = item.getNodeName();
      String prefix = item.getPrefix() + ":";
      if (prefix != null && nodeName.startsWith(prefix)) {
        nodeName = nodeName.substring(prefix.length());
      }

      packageNameToPrefix.addElement(packageName, nodeName);
    }
  }

  /**
   * Resolves a prefix for the given package name. If the package name is
   * already imported, the existing prefix is returned. Otherwise, a suitable
   * prefix is generated.
   * 
   * @param packageName the Java package name
   * @return the resolved prefix for the namespace importing the given package
   */
  public String resolvePrefix(String packageName) {
    Set<String> allPrefixes = packageNameToPrefix.getElements(packageName);
    if (allPrefixes.size() == 0) {
      String prefix = computeAvailablePrefix(packageName);
      packageNameToPrefix.addElement(packageName, prefix);
      return prefix;
    } else {
      return allPrefixes.iterator().next();
    }
  }

  /**
   * Writes all of the managed package to namespace mappings to the root element
   * as namespace definition attributes.
   * <p>
   * Note: There isn't duplicate checking for attributes pointing to the same
   * namespace. If there is a name conflict between an prefix name managed by
   * this class and an existing attribute on the root element, the existing
   * attribute will be replaced.
   */
  public void writeToElement(Element rootElement) {
    for (String packageName : packageNameToPrefix.keys()) {
      for (String prefix : packageNameToPrefix.getElements(packageName)) {
        rootElement.setAttributeNS(UiBinderConstants.XMLNS_NAMESPACE,
          UiBinderConstants.XMLNS_PREFIX + ":" + prefix,
          UiBinderConstants.URN_IMPORT_NAMESPACE_BEGINNING
              + packageName);
      }
    }
  }

  private String computeAvailablePrefix(String packageName) {
    String curPrefix = getStandardPrefix(packageName);
    if (curPrefix != null && !packageNameToPrefix.hasElement(curPrefix)) {
      return curPrefix;
    }

    if (packageName.contains(".")) {
      // The argument is a regular expression, hence the brackets.
      String[] fragments = packageName.split("[.]");
      int lastUncommonFragmentIndex = getLastUncommonFragmentIndex(fragments);
      if (lastUncommonFragmentIndex >= 0) {
        // Try the first character of the last uncommon fragment
        curPrefix = fragments[lastUncommonFragmentIndex].substring(0, 1);
        if (!packageNameToPrefix.hasElement(curPrefix)) {
          return curPrefix;
        }

        // Try the last uncommon fragment, the last two uncommon fragments, ...
        StringBuilder prefixBuilder = new StringBuilder(packageName.length());
        for (int i = lastUncommonFragmentIndex; i >= 0; i--) {
          if (i != lastUncommonFragmentIndex) {
            // e.g. google-gwt for the second iteration of com.google.gwt
            prefixBuilder.insert(0, '-');
          }

          prefixBuilder.insert(0, fragments[i]);

          if (!packageNameToPrefix.hasElement(prefixBuilder.toString())) {
            return prefixBuilder.toString();
          }
        }
      }
    }

    return arbitraryPrefixGenerator.computePrefix();
  }
}
