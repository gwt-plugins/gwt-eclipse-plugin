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
package com.google.gdt.eclipse.core.reference.location;

import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.java.ClasspathResourceUtilities;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Tracks a reference to a resource that is relative to the classpath.
 * <p>
 * This class is thread-safe due to its immutability.
 */
public final class ClasspathRelativeFileReferenceLocation extends
    AbstractReferenceLocation implements IMatchable {

  private final IPath classpathRelativePath;

  public ClasspathRelativeFileReferenceLocation(IPath classpathRelativePath) {
    this.classpathRelativePath = classpathRelativePath;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ClasspathRelativeFileReferenceLocation)) {
      return false;
    }

    ClasspathRelativeFileReferenceLocation other = (ClasspathRelativeFileReferenceLocation) obj;
    return classpathRelativePath.equals(other.classpathRelativePath);
  }

  public IPath getClasspathRelativePath() {
    return classpathRelativePath;
  }

  @Override
  public int hashCode() {
    return classpathRelativePath.hashCode();
  }

  public boolean matches(Object obj) {
    if (!(obj instanceof IResource)) {
      return false;
    }

    IResource resource = (IResource) obj;

    // Quick check to see if there's a possibility of this reference's path
    // matching the given resource's path.
    if (!resource.getProjectRelativePath().toPortableString().endsWith(
        classpathRelativePath.toPortableString())) {
      return false;
    }
    
    try {
      IJavaProject javaProject = JavaCore.create(getReference().getSourceProject());
      if (javaProject == null) {
        return false;
      }

      IPath resourceClasspathRelativePath = ClasspathResourceUtilities.getClasspathRelativePathOfResource(
          resource, javaProject);
      return resourceClasspathRelativePath != null
          && resourceClasspathRelativePath.equals(classpathRelativePath);

    } catch (JavaModelException e) {
      CorePluginLog.logError(e,
          "Could not check whether the given resource matches a reference location.");
      return false;
    }
  }

  @Override
  public String toString() {
    return classpathRelativePath.toString();
  }

}
