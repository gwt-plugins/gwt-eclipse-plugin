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
package com.google.gdt.eclipse.core.reference;

import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.reference.location.ClasspathRelativeFileReferenceLocation;
import com.google.gdt.eclipse.core.reference.location.IReferenceLocation;
import com.google.gdt.eclipse.core.reference.location.LogicalJavaElementReferenceLocation;
import com.google.gdt.eclipse.core.reference.logicaljavamodel.ILogicalJavaElement;
import com.google.gdt.eclipse.core.reference.logicaljavamodel.LogicalPackage;
import com.google.gdt.eclipse.core.reference.logicaljavamodel.LogicalType;
import com.google.gdt.eclipse.core.reference.logicaljavamodel.UiBinderImportReferenceType;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IMemento;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

/**
 * Quick implementation to persist everything reachable from a reference
 * manager. Post launch, we will look into using Eclipse's patterns for
 * persisting (IPersistable, IPersistableElement, IElementFactory, ...), so
 * rather than spread temporary serialization logic around the different
 * classes, it is all contained here.
 * <p>
 * Anytime a persist calls out to another persist method, ensure the callee
 * receives a new memento for its scope. Anytime a load calls out to another
 * load method, ensure the callee receives a memento scoped for it.
 */
public class ReferenceManagerPersister {

  private static final String KEY_REFERENCES = "references";
  private static final String KEY_LOGICAL_JAVA_ELEMENT = "logicalJavaElement";
  private static final String KEY_PACKAGE_NAME = "packageName";
  private static final String KEY_FULLY_QUALIFIED_NAME = "fullyQualifiedName";
  private static final String KEY_CLASS = "class";
  private static final String KEY_CLASSPATH_RELATIVE_PATH = "classpathRelativePath";
  private static final String KEY_REFERENCE = "reference";
  private static final String KEY_TARGET_LOCATION = "targetLocation";
  private static final String KEY_SOURCE_LOCATION = "sourceLocation";
  private static final String KEY_SOURCE_PROJECT = "sourceProject";

  public static Set<IReference> load(IMemento memento)
      throws PersistenceException {
    return loadReferences(getChildMementoOrThrowException(memento,
        KEY_REFERENCES));
  }

  public static void persist(Set<IReference> references, IMemento memento) {
    persistReferences(references, memento.createChild(KEY_REFERENCES));
  }

  private static IMemento getChildMementoOrThrowException(
      IMemento parentMemento, String childMementoType)
      throws PersistenceException {
    IMemento childMemento = parentMemento.getChild(childMementoType);
    if (childMemento == null) {
      throw new PersistenceException("There is no child memento with type "
          + childMementoType);
    }

    return childMemento;
  }

  private static String getStringOrThrowException(IMemento memento, String key)
      throws PersistenceException {
    String string = memento.getString(key);
    if (string == null) {
      throw new PersistenceException("Could not get string for key " + key);
    }
    return string;
  }

  private static ILogicalJavaElement loadLogicalJavaElement(IMemento memento)
      throws PersistenceException {
    String className = getStringOrThrowException(memento, KEY_CLASS);

    // This if-statement should consider all the implementors of ILogicalJavaElement
    if (LogicalType.class.getSimpleName().equals(className)
        || UiBinderImportReferenceType.class.getSimpleName().equals(className)) {
      String fullyQualifiedName = getStringOrThrowException(memento,
          KEY_FULLY_QUALIFIED_NAME);
      return new LogicalType(fullyQualifiedName);

    } else if (LogicalPackage.class.getSimpleName().equals(className)) {
      String packageName = getStringOrThrowException(memento, KEY_PACKAGE_NAME);
      return new LogicalPackage(packageName);

    } else {
      throw new PersistenceException(
          "Trying to load an unknown Java element type " + className);
    }
  }

  private static IReference loadReference(IMemento memento)
      throws PersistenceException {
    String projectName = getStringOrThrowException(memento, KEY_SOURCE_PROJECT);
    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
    if (project == null || !project.exists()) {
      // This is not an exception, since the reference is no longer needed if
      // its originating point does not exist
      CorePluginLog.logWarning(MessageFormat.format(
          "Not loading reference since the source project {0} does not exist anymore.",
          projectName));
      return null;
    }
    
    IReferenceLocation sourceLocation = loadReferenceLocation(getChildMementoOrThrowException(
        memento, KEY_SOURCE_LOCATION));
    IReferenceLocation targetLocation = loadReferenceLocation(getChildMementoOrThrowException(
        memento, KEY_TARGET_LOCATION));

    return new Reference(sourceLocation, targetLocation, project);
  }

  private static IReferenceLocation loadReferenceLocation(IMemento memento)
      throws PersistenceException {
    String className = getStringOrThrowException(memento, KEY_CLASS);

    if (ClasspathRelativeFileReferenceLocation.class.getSimpleName().equals(
        className)) {
      String classpathRelativePath = getStringOrThrowException(memento,
          KEY_CLASSPATH_RELATIVE_PATH);
      return new ClasspathRelativeFileReferenceLocation(
          Path.fromPortableString(classpathRelativePath));

    } else if (LogicalJavaElementReferenceLocation.class.getSimpleName().equals(
        className)) {
      ILogicalJavaElement logicalJavaElement = loadLogicalJavaElement(getChildMementoOrThrowException(
          memento, KEY_LOGICAL_JAVA_ELEMENT));
      return new LogicalJavaElementReferenceLocation(logicalJavaElement);

    } else {
      throw new PersistenceException("Unknown reference location class "
          + className);
    }
  }

  private static Set<IReference> loadReferences(IMemento memento)
      throws PersistenceException {
    Set<IReference> references = new HashSet<IReference>();

    boolean hadException = false;

    for (IMemento childMemento : memento.getChildren(KEY_REFERENCE)) {
      try {
        IReference loadedReference = loadReference(childMemento);
        if (loadedReference != null) {
          references.add(loadedReference);
        }
      } catch (PersistenceException e) {
        CorePluginLog.logError(e, "Error loading persisted reference.");
        hadException = true;
      }
    }

    if (hadException) {
      throw new PersistenceException(
          "Error loading all the references, check log for more details.");
    }

    return references;
  }

  private static void persistLogicalJavaElement(ILogicalJavaElement element,
      IMemento memento) throws PersistenceException {

    memento.putString(KEY_CLASS, element.getClass().getSimpleName());

    if (element instanceof LogicalType) {
      memento.putString(KEY_FULLY_QUALIFIED_NAME,
          ((LogicalType) element).getFullyQualifiedName());

    } else if (element instanceof LogicalPackage) {
      memento.putString(KEY_PACKAGE_NAME,
          ((LogicalPackage) element).getPackageName());

    } else {
      throw new PersistenceException("Could not persist "
          + element.getClass().getName());
    }
  }

  private static void persistReference(IReference reference, IMemento memento)
      throws PersistenceException {
    memento.putString(KEY_SOURCE_PROJECT,
        reference.getSourceProject().getName());
    persistReferenceLocation(reference.getSourceLocation(),
        memento.createChild(KEY_SOURCE_LOCATION));
    persistReferenceLocation(reference.getTargetLocation(),
        memento.createChild(KEY_TARGET_LOCATION));
  }

  private static void persistReferenceLocation(IReferenceLocation location,
      IMemento memento) throws PersistenceException {

    memento.putString(KEY_CLASS, location.getClass().getSimpleName());

    if (location instanceof ClasspathRelativeFileReferenceLocation) {
      memento.putString(
          KEY_CLASSPATH_RELATIVE_PATH,
          ((ClasspathRelativeFileReferenceLocation) location).getClasspathRelativePath().toPortableString());

    } else if (location instanceof LogicalJavaElementReferenceLocation) {
      persistLogicalJavaElement(
          ((LogicalJavaElementReferenceLocation) location).getLogicalJavaElement(),
          memento.createChild(KEY_LOGICAL_JAVA_ELEMENT));

    } else {
      throw new PersistenceException("Could not persist "
          + location.getClass().getName());
    }
  }

  private static void persistReferences(Set<IReference> references,
      IMemento memento) {

    for (IReference reference : references) {
      // Creating multiple children with the same type is ok
      try {
        persistReference(reference, memento.createChild(KEY_REFERENCE));
      } catch (PersistenceException e) {
        CorePluginLog.logError(e, "Could not persist reference: " + reference);
      }
    }
  }
}
