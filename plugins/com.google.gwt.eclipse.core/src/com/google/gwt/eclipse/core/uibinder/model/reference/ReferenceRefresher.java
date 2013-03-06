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
package com.google.gwt.eclipse.core.uibinder.model.reference;

import com.google.gdt.eclipse.core.BuilderUtilities;
import com.google.gdt.eclipse.core.JavaUtilities;
import com.google.gdt.eclipse.core.SseUtilities;
import com.google.gdt.eclipse.core.java.ClasspathResourceUtilities;
import com.google.gdt.eclipse.core.java.JavaModelSearch;
import com.google.gdt.eclipse.core.reference.IReference;
import com.google.gdt.eclipse.core.reference.ReferenceManager;
import com.google.gdt.eclipse.core.reference.location.ClasspathRelativeFileReferenceLocation;
import com.google.gdt.eclipse.core.reference.location.IReferenceLocation;
import com.google.gdt.eclipse.core.reference.location.LogicalJavaElementReferenceLocation;
import com.google.gdt.eclipse.core.reference.logicaljavamodel.ILogicalJavaElement;
import com.google.gdt.eclipse.core.reference.logicaljavamodel.LogicalType;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.uibinder.UiBinderException;
import com.google.gwt.eclipse.core.uibinder.problems.MarkerPlacementStrategy;
import com.google.gwt.eclipse.core.uibinder.problems.UiBinderProblemMarkerManager;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

/**
 * Triggers revalidation on the source location of references.
 * <p>
 * This class refreshes inline (i.e. it does not schedule a job to refresh.)
 * However, this class will lock on a granular basis--for example, on each
 * resource while it is being refreshed. If the client schedules a job to call a
 * method in this class, it can set the rule on its job to null.
 */
@SuppressWarnings("restriction")
public class ReferenceRefresher {

  private final ReferenceManager referenceManager;

  public ReferenceRefresher(ReferenceManager referenceManager) {
    this.referenceManager = referenceManager;
  }

  public void refreshReference(IReference reference) throws UiBinderException {
    IReferenceLocation srcLocation = reference.getSourceLocation();
    IProject project = reference.getSourceProject();
    IJavaProject javaProject = JavaCore.create(project);
    if (javaProject == null) {
      throw new UiBinderException(
          "The Java project could not be found for the reference.");
    }

    if (srcLocation instanceof ClasspathRelativeFileReferenceLocation) {
      // Currently, we only support .ui.xml files for this location
      try {
        refreshUiXml((ClasspathRelativeFileReferenceLocation) srcLocation,
            javaProject);
      } catch (JavaModelException e) {
        throw new UiBinderException(e);
      }

    } else if (srcLocation instanceof LogicalJavaElementReferenceLocation) {
      refreshLogicalJavaElement((LogicalJavaElementReferenceLocation) srcLocation);
    }
  }

  private void logCouldNotRefreshResource(Throwable t, IResource resource) {
    GWTPluginLog.logError(t, "Could not refresh the references on the {0}",
        resource.getLocation().toOSString());
  }

  private void refreshLogicalJavaElement(
      LogicalJavaElementReferenceLocation location) {
    ILogicalJavaElement logicalJavaElement = location.getLogicalJavaElement();
    if (!(logicalJavaElement instanceof LogicalType)) {
      // Only support types right now
      return;
    }

    IJavaProject project = JavaCore.create(location.getReference().getSourceProject());
    assert project != null;

    LogicalType logicalType = (LogicalType) logicalJavaElement;
    try {
      IType type = logicalType.getType(project);
      if (!JavaModelSearch.isValidElement(type)) {
        return;
      }

      /*
       * If we only touch a compilation unit containing a type, JCP dependency
       * management will not trigger a build of dependent types. We depended on
       * this behavior for re-validating owner classes (in a separate CU from
       * their UiBinder) when a ui.xml changes (ui.xml -> touch CU of UiBinder
       * type -> rebuilds owner type). So, as a workaround, we directly touch
       * the CUs of owner types here.
       */
      Set<ICompilationUnit> compilationUnits = new HashSet<ICompilationUnit>();
      String ownerTypeName = UiBinderReferenceManager.INSTANCE.getSubtypeToOwnerIndex().getOwnerTypeName(
          type);
      if (ownerTypeName != null) {
        IType ownerType = JavaModelSearch.findType(type.getJavaProject(),
            ownerTypeName);
        if (JavaModelSearch.isValidElement(ownerType)) {
          ICompilationUnit compilationUnit = ownerType.getCompilationUnit();
          if (JavaModelSearch.isValidElement(compilationUnit)) {
            compilationUnits.add(compilationUnit);
          }
        }
      } else {
        GWTPluginLog.logWarning(new Exception(), MessageFormat.format(
            "Could not find owner type for {0}", type.getElementName()));
      }

      ICompilationUnit compilationUnit = type.getCompilationUnit();
      if (JavaModelSearch.isValidElement(compilationUnit)) {
        compilationUnits.add(compilationUnit);
      }

      if (compilationUnits.size() > 0) {
        BuilderUtilities.revalidateCompilationUnits(compilationUnits,
            "Revalidating UiBinder subtype");
      }

    } catch (JavaModelException e) {
      GWTPluginLog.logWarning(e, "Could not force revalidation of "
          + logicalType.getFullyQualifiedName());
    }
  }

  private void refreshUiXml(ClasspathRelativeFileReferenceLocation location,
      IJavaProject javaProject) throws JavaModelException {
    IPath uiXmlPath = location.getClasspathRelativePath();
    String packageName = JavaUtilities.getPackageNameFromPath(uiXmlPath.removeLastSegments(1));

    for (IPackageFragment pckgFragment : JavaModelSearch.getPackageFragments(
        javaProject, packageName)) {
      Object uiXmlRes = ClasspathResourceUtilities.resolveFileOnPackageFragment(
          uiXmlPath.lastSegment(), pckgFragment);
      if (uiXmlRes instanceof IFile) {
        refreshUiXml((IFile) uiXmlRes);
      } else {
        // TODO: support ui.xml inside JARs
      }
    }
  }

  private void refreshUiXml(final IFile uiXmlFile) {
    try {
      // Lock it
      ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
        public void run(IProgressMonitor monitor) throws CoreException {
          refreshUiXmlUnsafe(uiXmlFile);
        }
      }, uiXmlFile, 0, null);
    } catch (CoreException e) {
      GWTPluginLog.logError(e, "Could not refresh {0}.",
          uiXmlFile.getLocation().toOSString());
    }
  }

  private void refreshUiXmlUnsafe(final IFile uiXmlFile) {
    IDOMModel xmlModel = null;
    try {
      xmlModel = SseUtilities.getModelForRead(uiXmlFile);
      if (xmlModel != null) {
        UiBinderXmlParser.newInstance(xmlModel, referenceManager,
            new MarkerPlacementStrategy(UiBinderProblemMarkerManager.MARKER_ID)).parse();
      }
    } catch (IOException e) {
      logCouldNotRefreshResource(e, uiXmlFile);
    } catch (CoreException e) {
      logCouldNotRefreshResource(e, uiXmlFile);
    } catch (UiBinderException e) {
      logCouldNotRefreshResource(e, uiXmlFile);
    } finally {
      if (xmlModel != null) {
        xmlModel.releaseFromRead();
      }
    }
  }

}
