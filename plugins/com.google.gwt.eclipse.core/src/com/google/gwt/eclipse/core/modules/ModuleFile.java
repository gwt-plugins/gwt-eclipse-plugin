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
package com.google.gwt.eclipse.core.modules;

import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.util.Util;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.provisional.format.FormatProcessorXML;
import org.w3c.dom.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a GWT module file (.gwt.xml) on disk.
 */
@SuppressWarnings("restriction")
public class ModuleFile extends AbstractModule {

  // TODO: use XmlUtilities.EditOperation instead
  private abstract class EditModelOperation {

    public void run() {
      IDOMModel model = null;
      try {
        model = getModelForEdit();
        model.aboutToChangeModel();

        // Now do something to the model
        editModel(model);

        // Notify listeners that we've changed the model
        model.changedModel();

        // Save the model if necessary
        if (!model.isSharedForEdit() && model.isSaveNeeded()) {
          model.save();
        }
      } catch (IOException e) {
        GWTPluginLog.logError(e);
      } catch (CoreException e) {
        GWTPluginLog.logError(e);
      } finally {
        if (model != null) {
          model.releaseFromEdit();
        }
      }
    }

    protected abstract void editModel(IDOMModel model);

    private IDOMModel getModelForEdit() throws IOException, CoreException {
      IModelManager modelManager = StructuredModelManager.getModelManager();
      return (IDOMModel) modelManager.getModelForEdit(getFile());
    }
  }

  protected ModuleFile(IFile file) {
    super(file);
  }

  /**
   * Adds a new entry point to a module.
   * 
   * @param qualifiedTypeName name of the entry point class, which should
   *          com.google.gwt.core.client.EntryPoint
   * @throws Exception if there was a problem modifying or saving the module XML
   */
  public void addEntryPoint(final String qualifiedTypeName) throws Exception {
    new EditModelOperation() {
      @Override
      public void editModel(IDOMModel model) {
        IDOMDocument editDoc = model.getDocument();

        // TODO: create an empty element (no closing </entry-point>)

        Element entryPointElement = editDoc.createElement(ENTRY_POINT_TAG_NAME);
        entryPointElement.setAttribute(CLASS_ATTRIBUTE_NAME, qualifiedTypeName);
        editDoc.getDocumentElement().appendChild(entryPointElement);

        // Reformat the XML source to keep it nice and neat
        new FormatProcessorXML().formatModel(model);
      }
    }.run();
  }

  /**
   * Returns the backing IFile for the module XML file.
   * 
   * @return IFile referencing the module XML
   */
  public IFile getFile() {
    // We received storage as an IFile in our ctor, so we know this cast works
    return (IFile) storage;
  }

  /**
   * Returns the IFolder corresponding to a path relative to this module. The
   * IFolder is just a resource handle; call its exists() method before using.
   * 
   * TODO: wrap public, source, and super-src paths in a class and add this as
   * an instance method?
   * 
   * @param moduleRelativePath module-relative path
   * @return IFolder corresponding to the path
   */
  public IFolder getFolder(IPath moduleRelativePath) {
    IPath moduleFolderPath = storage.getFullPath().removeLastSegments(1);
    IPath folderPath = moduleFolderPath.append(moduleRelativePath);
    IResource folder = Util.getWorkspaceRoot().findMember(folderPath);
    return (IFolder) folder;
  }

  // TODO: move this to class that wraps higher-level DOM elements (entry
  // points, source paths, inherited modules, etc.) and links them to source
  // positions
  public int getLineOfOffset(final int offset) {
    final int[] line = new int[1];

    new ReadModelOperation() {
      @Override
      protected void readModel(IDOMModel model) {
        line[0] = model.getStructuredDocument().getLineOfOffset(offset);
      }
    }.run();

    return line[0];
  }

  public boolean isBinary() {
    return false;
  }

  /**
   * Returns whether a folder is on the public path of this module. The public
   * paths include the paths explicitly declared with
   * <code>&lt;public&gt;</code> tags and all of their descendant sub-folders.
   * 
   * @param folder the folder to check
   * @return <code>true</code> if this folder is on a public path, and
   *         <code>false</code> otherwise
   */
  public boolean isPublicPath(IFolder folder) {
    IFolder[] publicFolders = getFolders(getPublicPaths());

    IContainer moduleContainer = getFile().getParent();
    IContainer container = folder;

    // Walk up the ancestor chain looking for a public path matching this folder
    while (container.getType() == IResource.FOLDER) {
      // If we reach the module's container, we're done searching
      if (container.equals(moduleContainer)) {
        return false;
      }

      for (IFolder publicFolder : publicFolders) {
        if (container.equals(publicFolder)) {
          return true;
        }
      }

      container = container.getParent();
    }

    return false;
  }

  /**
   * <p>
   * Returns whether a package is on the client source path of this module. The
   * source paths include the paths explicitly declared with
   * <code>&lt;source&gt;</code> tags and all of their descendant packages.
   * </p>
   * <p>
   * For example, if a module is located in <code>com.hello</code> and has the
   * default client source path "client", then <code>com.hello.client</code>
   * and <code>com.hello.client.utils</code> would both return true.
   * </p>
   * 
   * @param pckg package to check
   * @return <code>true</code> if this package is on a client source path, and
   *         <code>false</code> otherwise
   */
  public boolean isSourcePackage(IPackageFragment pckg) {
    IResource resource = pckg.getResource();
    if (resource.getType() == IResource.FOLDER) {
      IPath pckgFolderPath = resource.getFullPath();

      // Check each source path to see if it's an ancestor of this package
      for (IFolder clientFolder : getFolders(getSourcePaths())) {
        IPath clientFolderPath = clientFolder.getFullPath();
        if (clientFolderPath.isPrefixOf(pckgFolderPath)) {
          return true;
        }
      }
    }

    return false;
  }

  @Override
  protected IDOMModel doGetModelForRead() throws IOException, CoreException {
    IModelManager modelManager = StructuredModelManager.getModelManager();
    return (IDOMModel) modelManager.getModelForRead(getFile());
  }

  @Override
  protected String doGetPackageName() {
    IFolder moduleFolder = (IFolder) getFile().getParent();
    IJavaElement javaElement = JavaCore.create(moduleFolder);
    if (javaElement != null) {
      if (javaElement.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
        return javaElement.getElementName();
      }
    } else {
      // TODO: handle super-source case here
    }

    return "";
  }

  private IFolder[] getFolders(List<IPath> paths) {
    List<IFolder> folders = new ArrayList<IFolder>();
    for (IPath path : paths) {
      IFolder folder = getFolder(path);
      if (folder != null) {
        folders.add(folder);
      }
    }
    return folders.toArray(new IFolder[0]);
  }

}
