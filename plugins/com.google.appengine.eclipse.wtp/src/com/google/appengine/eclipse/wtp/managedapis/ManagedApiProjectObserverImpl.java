/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.appengine.eclipse.wtp.managedapis;

import com.google.api.client.util.Maps;
import com.google.appengine.eclipse.wtp.AppEnginePlugin;
import com.google.common.collect.Lists;
import com.google.gdt.eclipse.managedapis.ManagedApi;
import com.google.gdt.eclipse.managedapis.ManagedApiProject;
import com.google.gdt.eclipse.managedapis.ManagedApiProjectObserver;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jst.j2ee.classpathdep.ClasspathDependencyUtil;
import org.eclipse.jst.j2ee.project.EarUtilities;
import org.eclipse.jst.j2ee.project.JavaEEProjectUtilities;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.datamodel.properties.IAddReferenceDataModelProperties;
import org.eclipse.wst.common.componentcore.internal.operation.AddReferenceDataModelProvider;
import org.eclipse.wst.common.componentcore.internal.resources.VirtualArchiveComponent;
import org.eclipse.wst.common.componentcore.internal.resources.VirtualReference;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.frameworks.datamodel.IDataModelProvider;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * An observer implementation for dealing with managed APIs.
 */
@SuppressWarnings("restriction")
final class ManagedApiProjectObserverImpl implements ManagedApiProjectObserver {
  private ManagedApiProject managedApiProject;
  private IVirtualComponent rootComponent;

  public ManagedApiProjectObserverImpl(ManagedApiProject project) {
    managedApiProject = project;
    rootComponent = ComponentCore.createComponent(project.getProject());
  }

  @Override
  public void addManagedApis(ManagedApi[] apis) {
    // get existing
    Map<String, Object> options = Maps.newHashMap();
    options.put(IVirtualComponent.REQUESTED_REFERENCE_TYPE,
        IVirtualComponent.DISPLAYABLE_REFERENCES_ALL);
    Map<String, IVirtualReference> existingReferencies = Maps.newHashMap();
    for (IVirtualReference reference : rootComponent.getReferences(options)) {
      existingReferencies.put(reference.getArchiveName(), reference);
    }

    try {
      List<IPath> archives = Lists.newArrayList();
      for (ManagedApi api : apis) {
        IClasspathEntry[] classpathEntries = api.getClasspathEntries();
        for (IClasspathEntry entry : classpathEntries) {
          archives.add(entry.getPath());
        }
      }
      IVirtualReference[] newReferencies = createVirtualReferences(existingReferencies, archives);
      for (IVirtualReference reference : newReferencies) {
        addReference(reference);
      }
    } catch (CoreException e) {
      AppEnginePlugin.logMessage(e);
    }
  }

  @Override
  public void changeCopyToDirectory(IFolder originalFolder, IFolder newFolder) {
  }

  @Override
  public void refreshManagedApis(ManagedApi[] api) {
  }

  @Override
  public void removeManagedApis(ManagedApi[] api) {
  }

  /**
   * Adds a give {@link IVirtualReference} as a dependency.
   */
  private void addReference(IVirtualReference reference) throws CoreException {
    IDataModelProvider provider = getAddReferenceDataModelProvider(reference);
    IDataModel dm = DataModelFactory.createDataModel(provider);
    dm.setProperty(IAddReferenceDataModelProperties.SOURCE_COMPONENT, rootComponent);
    dm.setProperty(IAddReferenceDataModelProperties.TARGET_REFERENCE_LIST, Arrays.asList(reference));

    IStatus stat = dm.validateProperty(IAddReferenceDataModelProperties.TARGET_REFERENCE_LIST);
    if (!stat.isOK()) {
      throw new CoreException(stat);
    }
    try {
      dm.getDefaultOperation().execute(new NullProgressMonitor(), null);
    } catch (ExecutionException e) {
      AppEnginePlugin.logMessage(e);
    }
  }

  /**
   * Create a {@link IVirtualReference} for given jar library.
   */
  private IVirtualReference createVirtualReference(Map<String, IVirtualReference> referencies,
      String runtimeLocation, IPath jarPath) {
    String type = VirtualArchiveComponent.LIBARCHIVETYPE + IPath.SEPARATOR;
    IVirtualComponent archive = ComponentCore.createArchiveComponent(rootComponent.getProject(),
        type + jarPath.makeRelative().toString());
    VirtualReference reference = new VirtualReference(rootComponent, archive);
    reference.setArchiveName(jarPath.lastSegment());
    if (runtimeLocation != null) {
      reference.setRuntimePath(new Path(runtimeLocation).makeAbsolute());
    }
    return reference;
  }

  /**
   * Creates a {@link IVirtualReference} for very unique jar name within given list of jar
   * libraries.
   */
  private IVirtualReference[] createVirtualReferences(Map<String, IVirtualReference> referencies,
      List<IPath> archives) {
    String runtimeLocation = getRuntimePath().toString();
    if (archives != null && archives.size() > 0) {
      List<IVirtualReference> refList = Lists.newArrayList();
      for (IPath path : archives) {
        IVirtualReference reference = createVirtualReference(referencies, runtimeLocation, path);
        if (!referencies.containsKey(reference.getArchiveName())) {
          refList.add(reference);
          referencies.put(reference.getArchiveName(), reference);
        }
      }
      return refList.toArray(new IVirtualReference[refList.size()]);
    }
    return new IVirtualReference[0];
  }

  private IDataModelProvider getAddReferenceDataModelProvider(IVirtualReference ref) {
    return new AddReferenceDataModelProvider();
  }

  /**
   * @return the path of runtime libs for the project, i.e., "WEB-INF/lib".
   */
  private IPath getRuntimePath() {
    boolean isWebApp = JavaEEProjectUtilities.isDynamicWebProject(managedApiProject.getProject());
    if (isWebApp) {
      return ClasspathDependencyUtil.getDefaultRuntimePath(isWebApp, false);
    }
    IProject[] earProjects = EarUtilities.getReferencingEARProjects(managedApiProject.getProject());
    if (earProjects.length > 0) {
      IVirtualComponent earComponent = ComponentCore.createComponent(earProjects[0]);
      if (earComponent != null) {
        return ClasspathDependencyUtil.calculateDefaultRuntimePath(earComponent, rootComponent);
      }
    }
    return ClasspathDependencyUtil.getDefaultRuntimePath(false, false);
  }
}