/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.appengine.eclipse.wtp.swarm;

import com.google.appengine.eclipse.wtp.utils.ProjectUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gdt.eclipse.appengine.swarm.util.ConnectedProjectHandler;
import com.google.gdt.eclipse.appengine.swarm.util.SwarmAnnotationUtils;
import com.google.gdt.eclipse.appengine.swarm.wizards.helpers.SwarmGenerationException;
import com.google.gdt.eclipse.appengine.swarm.wizards.helpers.SwarmServiceCreator;
import com.google.gdt.eclipse.core.MarkerUtilities;
import com.google.gdt.eclipse.managedapis.ManagedApiPlugin;

import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A builder tracking changes made to annotated classes and re-generate endpoint classes and libs if
 * necessary.
 */
@SuppressWarnings("restriction")
public final class CloudEndpointsBuilder extends IncrementalProjectBuilder {
  public static final String ID = AppEngineSwarmPlugin.PLUGIN_ID + ".gaeCloudEndpointsBuilder";
  private Set<IType> apiTypes;
  private String appId;
  private boolean appIdChanged;

  @Override
  protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor)
      throws CoreException {
    boolean fullBuild = kind == IncrementalProjectBuilder.FULL_BUILD;
    checkState(fullBuild);
    IProject project = getProject();
    MarkerUtilities.clearMarkers(CloudEndpointsUtils.MARKER_ID, project);
    IResourceDelta delta = getDelta(project);

    boolean shouldGenerate = fullBuild || delta != null && apiClassesChanged(delta, monitor)
        || appIdChanged;
    boolean withLibs = false;

    // return if disabled, allowing to track changes
    if (project.getSessionProperty(CloudEndpointsUtils.PROP_DISABLE_ENDPOINTS_BUILDER) != null) {
      return null;
    }

    if (appIdChanged || fullBuild) {
      if (ConnectedProjectHandler.getConnectedProject(project) != null) {
        withLibs = true;
      } else {
        withLibs = areLibsGeneratedLocally();
      }
    }
    if (shouldGenerate) {
      doGenerate(fullBuild, withLibs, monitor);
    }
    return null;
  }

  @Override
  protected void clean(IProgressMonitor monitor) throws CoreException {
    apiTypes = null;
    IProject project = getProject();
    MarkerUtilities.clearMarkers(CloudEndpointsUtils.MARKER_ID, project);
    super.clean(monitor);
  }

  /**
   * @return <code>true</code> if api-annotated class added, removed or changed. Modifies
   *         <code>{@link #apiTypes}</code> in case of adding/removing.
   */
  private boolean apiClassesChanged(IResourceDelta delta, final IProgressMonitor monitor)
      throws CoreException {
    final boolean changed[] = new boolean[1];
    delta.accept(new IResourceDeltaVisitor() {
      @Override
      public boolean visit(IResourceDelta delta) throws CoreException {
        IResource resource = delta.getResource();
        // don't look into generated data
        if (isGeneratedLibsResource(resource)) {
          return false;
        }
        // go with java code
        if (isJavaFileResource(resource)) {
          ICompilationUnit cu = (ICompilationUnit) JavaCore.create(resource);
          List<IType> apiAnnotatedTypes = Lists.newArrayList();
          SwarmAnnotationUtils.collectApiTypes(apiAnnotatedTypes, cu);
          if (apiAnnotatedTypes.isEmpty()) {
            // no annotated types, check if annotation has been removed
            IType[] types = cu.getTypes();
            for (IType type : types) {
              if (apiTypes.contains(type)) {
                removeTypeWithSubtypes(type, monitor);
                changed[0] = true;
              }
              return true;
            }
          } else {
            // either changed or annotation is added
            apiTypes.addAll(apiAnnotatedTypes);
            changed[0] = true;
          }
        }
        return true;
      }
    });
    return changed[0];
  }

  /**
   * @return <code>true</code> if the project has "endpoint-libs" folder.
   */
  private boolean areLibsGeneratedLocally() {
    IProject project = getProject();
    File libFolder = project.getLocation().append(ManagedApiPlugin.SWARM_LIB_FOLDER_NAME).toFile();
    return libFolder.exists();
  }

  /**
   * Records builder state if unknown, re-creates state if full build initiated or updates the
   * existing state.
   */
  private void checkState(boolean fullBuild) throws CoreException {
    IProject project = getProject();
    if (apiTypes == null || fullBuild) {
      apiTypes = Sets.newHashSet();
      apiTypes.addAll(collectAllApiTypes(project));
      appId = ProjectUtils.getAppId(project);
      appIdChanged = false;
      return;
    }
    appIdChanged = !appId.equals(ProjectUtils.getAppId(project));
    if (appIdChanged) {
      appId = ProjectUtils.getAppId(project);
    }
  }

  /**
   * Traverses the project and collect Api annotated classes.
   */
  private List<IType> collectAllApiTypes(IProject project) throws JavaModelException {
    List<IType> entityList = Lists.newArrayList();
    for (IPackageFragment pkgFragment : JavaCore.create(project).getPackageFragments()) {
      if (pkgFragment.getKind() != IPackageFragmentRoot.K_SOURCE) {
        continue;
      }
      for (ICompilationUnit cu : pkgFragment.getCompilationUnits()) {
        SwarmAnnotationUtils.collectApiTypes(entityList, cu);
      }
    }
    return entityList;
  }

  /**
   * Traverses the project and collect all swarm-annotated classes.
   */
  private List<IType> collectAllEntities(IProject project) throws JavaModelException {
    List<IType> entityList = Lists.newArrayList();
    for (IPackageFragment pkgFragment : JavaCore.create(project).getPackageFragments()) {
      if (pkgFragment.getKind() != IPackageFragmentRoot.K_SOURCE) {
        continue;
      }
      for (ICompilationUnit cu : pkgFragment.getCompilationUnits()) {
        SwarmAnnotationUtils.collectSwarmTypesInHierarchy(entityList, cu);
      }
    }
    return entityList;
  }

  /**
   * Create an error marker indication that there is a generation error. Neither resource, nor error
   * severity is provided, so set the marker project-wide.
   */
  private void createErrorMarker(Exception e) throws CoreException {
    MarkerUtilities.createMarker(CloudEndpointsUtils.MARKER_ID, getProject(), e.getMessage(),
        IMarker.SEVERITY_ERROR);
  }

  /**
   * Performs generation classes and libs (if needed).
   */
  private void doGenerate(boolean fullBuild, boolean withLibs, IProgressMonitor monitor)
      throws CoreException {
    IProject project = getProject();
    // TODO(amitin): not clear, should entityList be always null? Or always contains all
    // swarm-annotated types? We seem to need only Api-annotated types re-generated here
    // List<IType> entityList = fullBuild ? collectAllEntities(project) : null;
    List<IType> entityList = null;
    SwarmServiceCreator creator = CloudEndpointsUtils.createServiceCreator(project, entityList);
    creator.setProject(project);
    try {
      creator.create(withLibs, monitor);
    } catch (SwarmGenerationException se) {
      createErrorMarker(se);
      throw new ResourceException(IResourceStatus.BUILD_FAILED, null,
          "Error in generating Cloud Enpoints", se);
    } catch (Exception e) {
      throw new ResourceException(IResourceStatus.BUILD_FAILED, null,
          "Error in generating Cloud Enpoints", e);
    }
  }

  /**
   * @return <code>true</code> if the given resource is the endpoint-libs resource.
   */
  private boolean isGeneratedLibsResource(IResource resource) {
    String resourcePath = resource.getFullPath().toOSString();
    return resourcePath.indexOf(ManagedApiPlugin.SWARM_LIB_FOLDER_NAME) != -1;
  }

  /**
   * @return <code>true</code> if the given resource is java file resource.
   */
  private boolean isJavaFileResource(IResource resource) {
    return resource != null && resource.getType() == IResource.FILE && resource.exists()
        && resource.getName().endsWith(".java");
  }

  /**
   * Removes {@link IType} from {@link #apiTypes} removing it's subtypes if any.
   */
  private void removeTypeWithSubtypes(IType type, IProgressMonitor monitor) throws CoreException {
    apiTypes.remove(type);
    // look for subtypes
    IProject project = getProject();
    IJavaProject javaProject = JavaCore.create(project);
    // TODO(amitin): this could be time-consuming
    ITypeHierarchy typeHierarchy = type.newTypeHierarchy(javaProject, monitor);
    List<IType> toRemove = Lists.newArrayList();
    for (IType apiType : apiTypes) {
      if (typeHierarchy.contains(apiType)) {
        toRemove.add(apiType);
      }
    }
    apiTypes.removeAll(toRemove);
  }
}
