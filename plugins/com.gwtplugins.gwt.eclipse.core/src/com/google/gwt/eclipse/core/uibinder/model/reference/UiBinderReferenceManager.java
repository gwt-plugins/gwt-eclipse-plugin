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

import com.google.gdt.eclipse.core.MarkerUtilities;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gdt.eclipse.core.java.JavaModelSearch;
import com.google.gdt.eclipse.core.java.JavaModelSearch.IJavaElementDeltaVisitor;
import com.google.gdt.eclipse.core.reference.IReference;
import com.google.gdt.eclipse.core.reference.PersistenceException;
import com.google.gdt.eclipse.core.reference.ReferenceManager;
import com.google.gdt.eclipse.core.reference.ReferenceManager.ReferenceChangeListener;
import com.google.gdt.eclipse.core.reference.location.ReferenceLocationType;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.markers.GWTJavaProblem;
import com.google.gwt.eclipse.core.nature.GWTNature;
import com.google.gwt.eclipse.core.uibinder.UiBinderConstants;
import com.google.gwt.eclipse.core.uibinder.UiBinderException;
import com.google.gwt.eclipse.core.uibinder.model.UiBinderSubtypeToOwnerIndex;
import com.google.gwt.eclipse.core.uibinder.model.UiBinderSubtypeToUiXmlIndex;
import com.google.gwt.eclipse.core.uibinder.model.UiXmlReferencedFieldIndex;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tracks references within the UiBinder domain.
 */
public enum UiBinderReferenceManager {
  INSTANCE;

  private static final String KEY_REFERENCE_MANAGER = "referenceManager";

  private static final String KEY_ROOT = "uiBinderReferences";

  private static final String KEY_UI_XML_REF_FIELDS = "uiXmlReferencedFields";

  private static final String KEY_UIBINDER_SUBTYPE_OWNER = "uiBinderSubtypeToOwner";

  private static final String KEY_UIBINDER_SUBTYPE_UI_XML = "uiBinderSubtypeToUiXml";

  private static IMemento getChild(IMemento parentMemento,
      String childMementoType) throws PersistenceException {
    IMemento childMemento = parentMemento.getChild(childMementoType);
    if (childMemento == null) {
      throw new PersistenceException("There is no child memento with type "
          + childMementoType);
    }

    return childMemento;
  }

  private static File getPersistenceFile() {
    // <workspace>/.metadata/.plugins/com.google.gwt.eclipse.core
    return GWTPlugin.getDefault().getStateLocation().append(
        "uiBinderReferences").toFile();
  }

  /**
   * The listener for referenced resource changes.
   */
  private final ReferenceManager.ReferenceChangeListener referenceChangeListener = new ReferenceChangeListener() {

    public void referencedJavaElementChanged(
        Map<IJavaElement, IJavaElementDelta> changedElements) {
      // TODO: possibly change the API so this callback only gets a collection
      // of references.
      HashSet<IReference> allReferencesToRefresh = new HashSet<IReference>();
      for (IJavaElement changedElement : changedElements.keySet()) {
        Set<IReference> referencesToRefresh = referenceManager.getReferencesWithMatchingJavaElement(
            changedElement, EnumSet.of(ReferenceLocationType.TARGET));
        for (IReference ref : referencesToRefresh) {
          allReferencesToRefresh.add(ref);
        }
      }
      scheduleJobToRefresh(allReferencesToRefresh);
    }

    public void referencedResourceChanged(
        Map<IResource, IResourceDelta> changedResources) {
      HashSet<IReference> allReferencesToRefresh = new HashSet<IReference>();
      for (IResource changedResource : changedResources.keySet()) {
        IResourceDelta delta = changedResources.get(changedResource);
        int flags = delta.getFlags();
        int kind = delta.getKind();

        // Either this is an add or remove, or it is changed with either content
        // changes or the resource was open/closed
        if (kind == IResourceDelta.ADDED
            || kind == IResourceDelta.REMOVED
            || ((flags & IResourceDelta.CONTENT) != 0 || (flags & IResourceDelta.OPEN) != 0)) {

          Set<IReference> referencesToRefresh = referenceManager.getReferencesWithMatchingResource(
              changedResource, EnumSet.of(ReferenceLocationType.TARGET));
          for (IReference referenceToRefresh : referencesToRefresh) {
            allReferencesToRefresh.add(referenceToRefresh);
          }
        }
      }
      
      scheduleJobToRefresh(allReferencesToRefresh);
    }

    private void scheduleJobToRefresh(final HashSet<IReference> references) {
      WorkspaceJob refreshJob = new WorkspaceJob("Refreshing references") {
        @Override
        public IStatus runInWorkspace(IProgressMonitor monitor) {
          try {
            for (IReference ref : references) {
              referenceRefresher.refreshReference(ref);
            }
          } catch (UiBinderException e) {
            GWTPluginLog.logError(e,
                "Could not refresh the source of a reference");
          }
          return StatusUtilities.OK_STATUS;
        }

      };

      refreshJob.setRule(null);
      refreshJob.schedule();
    }
  };

  /**
   * The {@link ReferenceManager} used to track UiBinder references.
   */
  private ReferenceManager referenceManager;

  /**
   * Knows how to refresh references.
   */
  private ReferenceRefresher referenceRefresher;

  /**
   * UiBinder subtype to owner class index.
   */
  private UiBinderSubtypeToOwnerIndex subtypeToOwnerIndex;

  /**
   * UiBinder subtype to ui.xml file index.
   */
  private UiBinderSubtypeToUiXmlIndex subtypeToUiXmlIndex;

  /**
   * When a UiBinder subtype is removed, this element changed listener purges it
   * from our indices.
   */
  private final IElementChangedListener uiBinderSubtypePurger = new IElementChangedListener() {
    private final IJavaElementDeltaVisitor visitor = new IJavaElementDeltaVisitor() {
      public boolean visit(IJavaElementDelta delta) {
        IJavaElement element = delta.getElement();
        if (element instanceof ICompilationUnit) {
          /*
           * Unfortunately the element changed listener is not told about IType
           * removals explicitly. Instead, we are told about either a CU removal
           * or a CU change. We traverse the UiBinder subtypes and find those
           * contained in the given CU. If the CU is removed or the UiBinder
           * subtype no longer exists, we treat it as a UiBinder subtype
           * removal.
           */
          ICompilationUnit cu = (ICompilationUnit) element;

          for (IType uiBinderSubtype : subtypeToOwnerIndex.getAllUiBinderTypes()) {
            ICompilationUnit uiBinderSubtypeCu = uiBinderSubtype.getCompilationUnit();
            if (uiBinderSubtypeCu != null && uiBinderSubtypeCu.equals(cu)) {
              if (!uiBinderSubtype.exists()
                  || delta.getKind() == IJavaElementDelta.REMOVED) {
                subtypeToOwnerIndex.removeUiBinderType(uiBinderSubtype);
                subtypeToUiXmlIndex.removeUiBinderSubtype(uiBinderSubtype);
                referenceManager.removeReferences(referenceManager.getReferencesWithMatchingJavaElement(
                    uiBinderSubtype, EnumSet.of(ReferenceLocationType.SOURCE)));
              }
            }
          }

          // No need to visit this CU's children
          return false;

        } else if (element instanceof IJavaProject
            && delta.getKind() == IJavaElementDelta.REMOVED) {
          /*
           * When a project is removed, we are not told about the individual CUs
           * being deleted.
           */
          IProject project = ((IJavaProject) element).getProject();
          subtypeToOwnerIndex.clear(project);
          subtypeToUiXmlIndex.clear(project);
          uiXmlReferencedFieldIndex.clear((IJavaProject) element);
          referenceManager.removeSourceReferences(project);

          // Visit children if there are any (my experience shows there will not
          // be any)
          return true;

        } else {
          // Go to children
          return true;
        }
      }
    };

    public void elementChanged(ElementChangedEvent event) {
      JavaModelSearch.visitJavaElementDelta(event.getDelta(), visitor);
    }
  };

  /**
   * When a resource is deleted, purge any references sourced from it.
   */
  private final IResourceChangeListener resourcePurger = new IResourceChangeListener() {
    private final IResourceDeltaVisitor visitor = new IResourceDeltaVisitor() {
      public boolean visit(IResourceDelta delta) throws CoreException {
        if (delta.getKind() != IResourceDelta.REMOVED) {
          return true;
        }

        IResource resource = delta.getResource();

        // Remove any references with this resource as the source location
        referenceManager.removeReferences(referenceManager.getReferencesWithMatchingResource(
            resource, EnumSet.of(ReferenceLocationType.SOURCE)));

        // Remove from the referenced field index
        uiXmlReferencedFieldIndex.remove(resource.getFullPath());

        return false;
      }
    };

    public void resourceChanged(IResourceChangeEvent event) {
      if (event.getDelta() != null) {
        try {
          event.getDelta().accept(visitor);
        } catch (CoreException e) {
          GWTPluginLog.logError(e);
        }
      }
    }
  };

  /**
   * Stores the ui:field references for ui.xml files.
   */
  private UiXmlReferencedFieldIndex uiXmlReferencedFieldIndex;

  private UiBinderReferenceManager() {
    referenceManager = new ReferenceManager();
    subtypeToOwnerIndex = new UiBinderSubtypeToOwnerIndex();
    subtypeToUiXmlIndex = new UiBinderSubtypeToUiXmlIndex();
    uiXmlReferencedFieldIndex = new UiXmlReferencedFieldIndex();
    referenceRefresher = new ReferenceRefresher(referenceManager);
  }

  public ReferenceManager getReferenceManager() {
    return referenceManager;
  }

  /**
   * Returns the UiBinder subtype to owner class index.
   */
  public UiBinderSubtypeToOwnerIndex getSubtypeToOwnerIndex() {
    return subtypeToOwnerIndex;
  }

  /**
   * Returns the UiBinder subtype to ui.xml file index.
   */
  public UiBinderSubtypeToUiXmlIndex getSubtypeToUiXmlIndex() {
    return subtypeToUiXmlIndex;
  }

  /**
   * Returns an index storing the ui:field references for each ui.xml file.
   */
  public UiXmlReferencedFieldIndex getUiXmlReferencedFieldIndex() {
    return uiXmlReferencedFieldIndex;
  }

  public void persist() {
    XMLMemento memento = XMLMemento.createWriteRoot(KEY_ROOT);
    persist(memento);

    File file = getPersistenceFile();
    FileWriter writer = null;
    try {
      try {
        writer = new FileWriter(file);
        memento.save(writer);
      } finally {
        if (writer != null) {
          writer.close();
        }
      }
    } catch (IOException e) {
      GWTPluginLog.logError(e, "Error persisting UiBinder references");

      // Make sure we remove any partially-written file
      if (file.exists()) {
        file.delete();
      }
    }
  }

  /**
   * Starts the reference manager.
   */
  public void start() {
    if (UiBinderConstants.UI_BINDER_ENABLED) {
      load();

      referenceManager.addReferencedResourceChangeListener(referenceChangeListener);
      referenceManager.start();

      JavaCore.addElementChangedListener(uiBinderSubtypePurger,
          ElementChangedEvent.POST_CHANGE);
      ResourcesPlugin.getWorkspace().addResourceChangeListener(resourcePurger,
          IResourceChangeEvent.POST_CHANGE);
    }
  }

  /**
   * Stops the reference manager.
   */
  public void stop() {
    if (UiBinderConstants.UI_BINDER_ENABLED) {
      ResourcesPlugin.getWorkspace().removeResourceChangeListener(
          resourcePurger);
      JavaCore.removeElementChangedListener(uiBinderSubtypePurger);

      referenceManager.stop();
      referenceManager.removeReferencedResourceChangeListener(referenceChangeListener);

      persist();
    }
  }

  private void load() {
    FileReader reader = null;
    try {
      try {
        reader = new FileReader(getPersistenceFile());
        load(XMLMemento.createReadRoot(reader));
      } finally {
        if (reader != null) {
          reader.close();
        }
      }
    } catch (FileNotFoundException e) {
      // Ignore this exception, which occurs when index does not yet exist
    } catch (Throwable e) {
      GWTPluginLog.logError(e, "Could not load UiBinder indices.");

      new WorkspaceJob("Notifying of failure to load UiBinder index") {
        @Override
        public IStatus runInWorkspace(IProgressMonitor monitor)
            throws CoreException {
          String errorMessage = "Failed to load the UiBinder index, please clean the project to rebuild it";
          try {
            for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
              if (GWTNature.isGWTProject(project)
                  && MarkerUtilities.findMarker(GWTJavaProblem.MARKER_ID, -1,
                      -1, project, errorMessage, false) == null) {
                MarkerUtilities.createMarker(GWTJavaProblem.MARKER_ID, project,
                    errorMessage, IMarker.SEVERITY_ERROR);
              }
            }
          } catch (CoreException e1) {
            GWTPluginLog.logError(e1, errorMessage);
          }

          return StatusUtilities.OK_STATUS;
        }
      }.schedule();
    }
  }

  private void load(IMemento memento) throws PersistenceException {
    IMemento refManagerMemento = getChild(memento, KEY_REFERENCE_MANAGER);
    referenceManager = ReferenceManager.load(refManagerMemento);
    referenceRefresher = new ReferenceRefresher(referenceManager);

    IMemento subtypeToOwnerMemento = getChild(memento,
        KEY_UIBINDER_SUBTYPE_OWNER);
    subtypeToOwnerIndex = UiBinderSubtypeToOwnerIndex.load(subtypeToOwnerMemento);

    IMemento subtypeToUiXmlMemento = getChild(memento,
        KEY_UIBINDER_SUBTYPE_UI_XML);
    subtypeToUiXmlIndex = UiBinderSubtypeToUiXmlIndex.load(subtypeToUiXmlMemento);

    IMemento uiXmlReferencedFieldsMemento = getChild(memento,
        KEY_UI_XML_REF_FIELDS);
    uiXmlReferencedFieldIndex = UiXmlReferencedFieldIndex.load(uiXmlReferencedFieldsMemento);
  }

  private void persist(IMemento memento) {
    referenceManager.persist(memento.createChild(KEY_REFERENCE_MANAGER));
    subtypeToOwnerIndex.persist(memento.createChild(KEY_UIBINDER_SUBTYPE_OWNER));
    subtypeToUiXmlIndex.persist(memento.createChild(KEY_UIBINDER_SUBTYPE_UI_XML));
    uiXmlReferencedFieldIndex.persist(memento.createChild(KEY_UI_XML_REF_FIELDS));
  }

}
