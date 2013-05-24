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

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;

/**
 * Basic class for GaeFacet resource creation.
 */
public abstract class GaeResourceCreateOperation extends AbstractDataModelOperation {

  protected IResource resource;
  private boolean resourceCreated;

  public GaeResourceCreateOperation(IDataModel model) {
    super(model);
  }

  @Override
  public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
    resource = getResource();
    if (!resource.exists()) {
      createResource();
      resourceCreated = true;
    }
    return configureResource();
  }

  @Override
  public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
    return null;
  }

  @Override
  public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
    if (resourceCreated && resource != null && resource.exists()) {
      try {
        resource.delete(true, monitor);
      } catch (CoreException e) {
        throw new ExecutionException("Undo failed", e);
      }
    }
    return Status.OK_STATUS;
  }

  /**
   * Override here if the resource (either existing or new) requires configuration.
   */
  protected IStatus configureResource() throws ExecutionException {
    return Status.OK_STATUS;
  }

  /**
   * If the resource doesn't exist this is the chance to create it.
   */
  protected abstract void createResource() throws ExecutionException;

  /**
   * Return here the resource which should be created and/or configured.
   */
  protected abstract IResource getResource();
}
