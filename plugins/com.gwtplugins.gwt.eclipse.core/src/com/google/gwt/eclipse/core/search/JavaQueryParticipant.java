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
package com.google.gwt.eclipse.core.search;

import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.util.Util;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.search.ElementQuerySpecification;
import org.eclipse.jdt.ui.search.IMatchPresentation;
import org.eclipse.jdt.ui.search.IQueryParticipant;
import org.eclipse.jdt.ui.search.ISearchRequestor;
import org.eclipse.jdt.ui.search.PatternQuerySpecification;
import org.eclipse.jdt.ui.search.QuerySpecification;
import org.eclipse.search.ui.text.Match;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Find Java references inside JSNI blocks.
 */
public class JavaQueryParticipant implements IQueryParticipant {

  private static final IJavaSearchScope WORKSPACE_SCOPE = SearchEngine.createWorkspaceScope();

  public static Set<IIndexedJavaRef> findWorkspaceReferences(
      IJavaElement element, boolean resolveMatches) {
    JavaQueryParticipant searchEngine = new JavaQueryParticipant();
    ElementQuerySpecification query = new ElementQuerySpecification(element,
        IJavaSearchConstants.REFERENCES, WORKSPACE_SCOPE, "");

    return searchEngine.findMatches(query, resolveMatches);
  }

  private static boolean isTypeSearch(QuerySpecification query) {
    if (query instanceof ElementQuerySpecification) {
      return (((ElementQuerySpecification) query).getElement().getElementType() == IJavaElement.TYPE);
    }

    return (((PatternQuerySpecification) query).getSearchFor() == IJavaSearchConstants.TYPE);
  }

  public int estimateTicks(QuerySpecification specification) {
    // TODO: better guess here
    return 500;
  }

  public IMatchPresentation getUIParticipant() {
    return null;
  }

  public void search(ISearchRequestor requestor, QuerySpecification query,
      IProgressMonitor monitor) throws CoreException {
    try {
      monitor.subTask("Locating GWT matches...");

      // Make sure we're searching for Java references
      switch (query.getLimitTo()) {
        case IJavaSearchConstants.REFERENCES:
        case IJavaSearchConstants.ALL_OCCURRENCES:
          // These we handle as expected
          break;
        case IJavaSearchConstants.READ_ACCESSES:
        case IJavaSearchConstants.WRITE_ACCESSES:
          // We don't actually check field references to see if they're read or
          // write accesses; we just treat this as a generic references search
          break;
        default:
          // Anything else (e.g., Declarations), we don't support
          return;
      }

      Set<IIndexedJavaRef> matchingJavaRefs = null;

      // Find all matching Java references in the index. The search algorithm
      // differs depending on whether we're doing an element query
      // (Ctrl-Shift-G) or a pattern query (Java Search dialog box)
      if (query instanceof ElementQuerySpecification) {
        ElementQuerySpecification elementQuery = (ElementQuerySpecification) query;
        matchingJavaRefs = findMatches(elementQuery, true);
      } else {
        assert (query instanceof PatternQuerySpecification);
        PatternQuerySpecification patternQuery = (PatternQuerySpecification) query;
        matchingJavaRefs = findMatches(patternQuery);
      }

      for (IIndexedJavaRef javaRef : matchingJavaRefs) {
        Match match = createMatch(javaRef, query);
        if (match != null) {
          // Report the match location to the Java Search engine
          requestor.reportMatch(match);
        }
      }
    } catch (Exception e) {
      // If we allow any exceptions to escape, the JDT will disable us
      GWTPluginLog.logError("Error finding Java Search matches", e);
    }
  }

  private Match createMatch(IIndexedJavaRef ref, QuerySpecification query) {
    // Get the file that the indexed Java reference is contained in
    IPath filePath = ref.getSource();
    IFile file = Util.getWorkspaceRoot().getFile(filePath);
    if (!file.exists()) {
      // This may happen if a file in the index is removed from the project
      return null;
    }

    // Get the location of the Java reference within its containing file
    int offset, length;
    if (isTypeSearch(query)) {
      offset = ref.getClassOffset();
      length = ref.className().length();

      if (query instanceof ElementQuerySpecification) {
        IType type = (IType) ((ElementQuerySpecification) query).getElement();

        // Using this instead of ref.className().length() allows us to highlight
        // outer types correctly on refs which reference their inner types.
        length = type.getFullyQualifiedName().length();
      }
    } else {
      offset = ref.getMemberOffset();
      length = ref.memberName().length();
    }

    // By default, we'll pass the file itself as the match element
    Object container = file;

    try {
      // See if the resource containing the reference is a .java file
      ICompilationUnit cu = (ICompilationUnit) JavaCore.create(file);
      if (cu != null) {
        // If the reference is in a .java file, find the JSNI method it's in
        IJavaElement jsniMethod = cu.getElementAt(offset);
        assert (jsniMethod instanceof IMethod);

        // Make sure the method is within the search scope
        if (!query.getScope().encloses(jsniMethod)) {
          return null;
        }

        // Pass the method as the more specific match element
        container = jsniMethod;
      } else {
        // In this case, the Java reference is not inside a .java file (e.g.,
        // it could be inside a module XML file)

        // Make sure this file is within the search scope
        if (!query.getScope().encloses(filePath.toString())) {
          return null;
        }
      }

      return new Match(container, offset, length);

    } catch (JavaModelException e) {
      GWTPluginLog.logError(e);
      return null;
    }
  }

  private Set<IIndexedJavaRef> findMatches(ElementQuerySpecification query,
      boolean resolveMatches) {
    IJavaElement javaElement = query.getElement();
    JavaRefIndex index = JavaRefIndex.getInstance();
    int elementType = javaElement.getElementType();

    // Type matches are easy: just compare the fully-qualified type name, and if
    // they are equal, then we have a match
    if (elementType == IJavaElement.TYPE) {
      String typeName = ((IType) javaElement).getFullyQualifiedName();
      return index.findTypeReferences(typeName);
    }

    // Besides types, we only support searching for fields and methods
    if (elementType != IJavaElement.FIELD && elementType != IJavaElement.METHOD) {
      return Collections.emptySet();
    }

    // Get the type that actually declares this member (could be a super type)
    IType declType = ((IMember) javaElement).getDeclaringType();
    assert (declType != null && declType.exists());

    // Search the index for matches based only on the member name and type
    Set<IIndexedJavaRef> nameMatches = (elementType == IJavaElement.METHOD)
        ? index.findMethodReferences(javaElement.getElementName())
        : index.findFieldReferences(javaElement.getElementName());

    // We optionally return the full set of "potential" matches (i.e., index
    // entries that match by name but may not resolve to the query element)
    if (!resolveMatches) {
      return nameMatches;
    }

    Set<IIndexedJavaRef> matches = new HashSet<IIndexedJavaRef>();
    for (IIndexedJavaRef nameMatch : nameMatches) {
      /*
       * Try to resolve each potential match to see if it actually references
       * the target Java element. This takes care of matching the method
       * parameter lists, as well as correctly searching for a super type's
       * members from a reference to one of its subclasses.
       */
      IJavaElement matchElement = nameMatch.resolve();
      if (javaElement.equals(matchElement)) {
        matches.add(nameMatch);
      }
    }

    return matches;
  }

  private Set<IIndexedJavaRef> findMatches(PatternQuerySpecification query) {
    // Translate the IJavaSearchConstant element type constants to IJavaElement
    // type constants.
    int elementType;
    switch (query.getSearchFor()) {
      case IJavaSearchConstants.TYPE:
        elementType = IJavaElement.TYPE;
        break;
      case IJavaSearchConstants.FIELD:
        elementType = IJavaElement.FIELD;
        break;
      case IJavaSearchConstants.METHOD:
      case IJavaSearchConstants.CONSTRUCTOR:
        // IJavaElement.METHOD represents both methods & ctors
        elementType = IJavaElement.METHOD;
        break;
      default:
        // We only support searching for types, fields, methods, and ctors
        return Collections.emptySet();
    }

    return JavaRefIndex.getInstance().findElementReferences(query.getPattern(),
        elementType, query.isCaseSensitive());
  }

}
