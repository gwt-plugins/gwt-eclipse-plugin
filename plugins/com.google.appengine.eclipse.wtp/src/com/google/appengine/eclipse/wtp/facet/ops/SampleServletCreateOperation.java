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
package com.google.appengine.eclipse.wtp.facet.ops;

import com.google.appengine.eclipse.core.resources.GaeProjectResources;
import com.google.appengine.eclipse.wtp.AppEnginePlugin;
import com.google.appengine.eclipse.wtp.facet.IGaeFacetConstants;
import com.google.appengine.eclipse.wtp.utils.ProjectUtils;
import com.google.common.collect.Lists;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.codegen.jet.JETException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.common.jdt.internal.javalite.JavaLiteUtilities;
import org.eclipse.jst.j2ee.application.internal.operations.IAnnotationsDataModel;
import org.eclipse.jst.j2ee.internal.common.operations.CreateJavaEEArtifactTemplateModel;
import org.eclipse.jst.j2ee.internal.common.operations.INewJavaClassDataModelProperties;
import org.eclipse.jst.j2ee.internal.common.operations.NewJavaEEArtifactClassOperation;
import org.eclipse.jst.j2ee.internal.web.operations.AddServletOperation;
import org.eclipse.jst.j2ee.internal.web.operations.INewServletClassDataModelProperties;
import org.eclipse.jst.j2ee.internal.web.operations.INewWebClassDataModelProperties;
import org.eclipse.jst.j2ee.internal.web.operations.NewServletClassDataModelProvider;
import org.eclipse.jst.j2ee.internal.web.operations.NewServletClassOperation;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.internal.operation.IArtifactEditOperationDataModelProperties;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.frameworks.datamodel.IDataModelOperation;

import java.util.List;

/**
 * Generates sample servlet and registers it into web.xml.
 */
@SuppressWarnings("restriction")
public final class SampleServletCreateOperation extends AbstractDataModelOperation {
  private String servletClassName;
  private String servletPath;

  /**
   * Default constructor
   */
  public SampleServletCreateOperation(IDataModel model, String className, String path) {
    super(model);
    servletClassName = className;
    servletPath = path;
  }

  @Override
  public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
    IProject project = ProjectUtils.getProject(model);
    // prepare model
    IDataModel servletModel = DataModelFactory.createDataModel(new NewServletClassDataModelProvider());
    servletModel.setStringProperty(INewJavaClassDataModelProperties.SOURCE_FOLDER,
        getDefaultJavaSourceFolder(project));
    final String servletPackage = model.getStringProperty(IGaeFacetConstants.GAE_PROPERTY_PACKAGE);
    servletModel.setStringProperty(INewJavaClassDataModelProperties.JAVA_PACKAGE, servletPackage);
    servletModel.setStringProperty(IArtifactEditOperationDataModelProperties.PROJECT_NAME,
        project.getName());
    servletModel.setStringProperty(INewJavaClassDataModelProperties.CLASS_NAME, servletClassName);
    servletModel.setStringProperty(INewWebClassDataModelProperties.DESCRIPTION, "Sample Servlet");
    servletModel.setBooleanProperty(IAnnotationsDataModel.USE_ANNOTATIONS, false);
    servletModel.setBooleanProperty(INewJavaClassDataModelProperties.GENERATE_DD, false);
    servletModel.setProperty(INewServletClassDataModelProperties.URL_MAPPINGS,
        Lists.<String[]> newArrayList(new String[] {"/" + servletPath}));
    // create servlet, use custom operation to provide GPE's servlet source.
    IDataModelOperation addServletOperation = new AddServletOperation(servletModel) {
      @Override
      protected NewJavaEEArtifactClassOperation getNewClassOperation() {
        return new NewServletClassOperation(getDataModel()) {
          @Override
          protected String generateTemplateSource(CreateJavaEEArtifactTemplateModel templateModel,
              Object templateImpl) throws JETException {
            try {
              return GaeProjectResources.createSampleServletSource(servletPackage, servletClassName);
            } catch (Throwable e) {
              throw new JETException(e);
            }
          }
        };
      }
    };
    IStatus status = addServletOperation.execute(monitor, info);
    // done creation
    if (status.getSeverity() == IStatus.ERROR) {
      return status;
    }
    // request open editor
    openJavaClass(project, servletModel);
    return Status.OK_STATUS;
  }

  /**
   * Searches project containers for any source folder available. Returns first available or
   * <code>null</code>.
   */
  private String getDefaultJavaSourceFolder(IProject project) {
    IVirtualComponent comp = ComponentCore.createComponent(project);
    List<IContainer> containers = JavaLiteUtilities.getJavaSourceContainers(comp);
    if (!containers.isEmpty()) {
      return containers.get(0).toString();
    }
    return null;
  }

  /**
   * Opens default editor for java class file.
   */
  private void openEditor(final IFile file) {
    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        try {
          IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
          IDE.openEditor(page, file, true);
        } catch (PartInitException e) {
          AppEnginePlugin.logMessage(e);
        }
      }
    });
  }

  /**
   * Searches for created servlet type and opens it in IDE.
   */
  private void openJavaClass(IProject project, IDataModel servletModel) {
    try {
      String className = servletModel.getStringProperty(INewJavaClassDataModelProperties.QUALIFIED_CLASS_NAME);
      IJavaProject javaProject = JavaCore.create(project);
      IType type = javaProject.findType(className);
      if (type != null) {
        IFile file = (IFile) type.getResource();
        if (file != null) {
          openEditor(file);
        }
      }
    } catch (Throwable e) {
      AppEnginePlugin.logMessage(e);
    }
  }
}
