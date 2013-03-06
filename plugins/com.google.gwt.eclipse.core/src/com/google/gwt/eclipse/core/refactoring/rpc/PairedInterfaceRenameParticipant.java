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
package com.google.gwt.eclipse.core.refactoring.rpc;

import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.refactoring.ChangeBuilder;
import com.google.gwt.eclipse.core.refactoring.ChangeUtilities;
import com.google.gwt.eclipse.core.refactoring.GWTRenameTypeParticipant;
import com.google.gwt.eclipse.core.refactoring.NestedRefactoringContext;
import com.google.gwt.eclipse.core.refactoring.RefactoringException;
import com.google.gwt.eclipse.core.refactoring.regionupdater.CompilationUnitRenamedReferenceUpdater;
import com.google.gwt.eclipse.core.refactoring.regionupdater.RegionUpdaterChangeWeavingVisitor;
import com.google.gwt.eclipse.core.refactoring.regionupdater.RenamedElementAstMatcher;
import com.google.gwt.eclipse.core.validators.rpc.RemoteServiceUtilities;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameTypeProcessor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;

/**
 * A participant of JDT type renaming that ensures a rename of a GWT RPC
 * interface will be mirrored on the paired GWT RPC interface. For example, if
 * the user renames FooService to BarService, this participant ensures
 * FooServiceAsync is renamed to BarServiceAsync.
 */
@SuppressWarnings("restriction")
public class PairedInterfaceRenameParticipant extends GWTRenameTypeParticipant {

  private static class InterfaceRenameChangeBuilder extends
      ChangeBuilder<RenameJavaElementDescriptor> {

    private final RenameTypeProcessor processor;
    private final IType pairedType;
    private final String pairedNewName;

    public InterfaceRenameChangeBuilder(IType pairedType, String pairedNewName,
        RenameTypeProcessor processor, IWorkspace workspace) {
      super(RenameJavaElementDescriptor.class, IJavaRefactorings.RENAME_TYPE,
          workspace);

      this.processor = processor;
      this.pairedType = pairedType;
      this.pairedNewName = pairedNewName;
    }

    @Override
    protected void configureDescriptor(RenameJavaElementDescriptor descriptor) {
      descriptor.setJavaElement(pairedType);
      descriptor.setNewName(pairedNewName);
      descriptor.setProject(pairedType.getJavaProject().getProject().getName());

      // getFilePatterns and getMatchStrategy could return uninitialized values
      // if they have not been explicitly set

      String filePatterns = processor.getFilePatterns();
      if (filePatterns != null) {
        descriptor.setFileNamePatterns(filePatterns);
      }

      int strategy = processor.getMatchStrategy();
      if (strategy == RenameJavaElementDescriptor.STRATEGY_EXACT
          || strategy == RenameJavaElementDescriptor.STRATEGY_EMBEDDED
          || strategy == RenameJavaElementDescriptor.STRATEGY_SUFFIX) {
        descriptor.setMatchStrategy(strategy);
      }

      descriptor.setUpdateQualifiedNames(processor.getUpdateQualifiedNames());
      descriptor.setUpdateReferences(processor.getUpdateReferences());
      descriptor.setUpdateSimilarDeclarations(processor.getUpdateSimilarDeclarations());
      descriptor.setUpdateTextualOccurrences(processor.getUpdateTextualMatches());
    }
  }

  /**
   * The nested refactoring context type for interface renames. It tracks the
   * already visited type (e.g. the user renames FooService, our participant for
   * this rename launches a new refactoring on FooServiceAsync whose context
   * will have the visited type set to FooService.)
   */
  private static class InterfaceRenameRefactoringContext extends
      NestedRefactoringContext {

    private static InterfaceRenameRefactoringContext newNestedRefactoringContext(
        IType visitedType) {
      return new InterfaceRenameRefactoringContext(visitedType);
    }

    private static InterfaceRenameRefactoringContext newRootRefactoringContext() {
      return new InterfaceRenameRefactoringContext(null);
    }

    private final IType visitedType;

    public InterfaceRenameRefactoringContext(IType visitedType) {
      this.visitedType = visitedType;
    }
  }

  private TypeContainer typeContainer;

  private String pairedNewName;
  private RenameTypeProcessor processor;
  private InterfaceRenameRefactoringContext refactoringContext;

  @Override
  public Change createChange(IProgressMonitor pm) throws CoreException,
      OperationCanceledException {

    // checkConditions is the first entry point (for us) after the user
    // changes the refactoring's new name. Recompute it here.
    if (!updateNewNameMetadata()) {
      return null;
    }

    // Prevent infinite recursion
    if (typeContainer.getPairedType().equals(refactoringContext.visitedType)) {
      return null;
    }

    Change change;
    try {
      change = createChangeForInterfaceRename();
    } catch (RefactoringException e) {
      GWTPluginLog.logError(e);
      return null;
    }

    if (ChangeUtilities.mergeParticipantTextChanges(this, change)) {
      // All our changes were merged into existing shared text changes,
      // therefore this participant does nothing
      return null;
    }

    weaveChange(change);

    return change;
  }

  @Override
  public String getName() {
    return "Rename GWT RPC paired interface";
  }

  @Override
  protected boolean initialize(Object element) {
    if (!super.initialize(element)) {
      return false;
    }

    try {
      typeContainer = TypeContainer.createTypeContainer(getTypeRefactoringSupport().getOldType());
      if (typeContainer == null) {
        // This isn't a GWT RPC interface
        return false;
      }

      if (!updateNewNameMetadata()) {
        return false;
      }

    } catch (JavaModelException e) {
      GWTPluginLog.logError(
          e,
          "Error when checking whether a rename type refactoring is renaming a GWT RPC interface");
      return false;
    }

    RefactoringProcessor refactoringProcessor = getRenameProcessor();
    if (!(refactoringProcessor instanceof RenameTypeProcessor)) {
      GWTPluginLog.logWarning("Not participating in GWT RPC type rename due to invalid processor type: "
          + refactoringProcessor.getClass().getSimpleName());
      return false;
    }
    processor = (RenameTypeProcessor) refactoringProcessor;

    refactoringContext = (InterfaceRenameRefactoringContext) NestedRefactoringContext.forProcessor(refactoringProcessor);
    if (refactoringContext == null) {
      // This is a root (i.e. user-initiated) refactoring, not a
      // refactoring-initiated nested refactoring
      refactoringContext = InterfaceRenameRefactoringContext.newRootRefactoringContext();
      NestedRefactoringContext.storeForProcessor(refactoringProcessor,
          refactoringContext);
    }

    return true;
  }

  private Change createChangeForInterfaceRename() throws RefactoringException {
    InterfaceRenameChangeBuilder builder = new InterfaceRenameChangeBuilder(
        typeContainer.getPairedType(),
        pairedNewName,
        processor,
        typeContainer.getPairedType().getJavaProject().getProject().getWorkspace());

    ProcessorBasedRefactoring refactoring = (ProcessorBasedRefactoring) builder.createRefactoring();
    RefactoringProcessor nestedProcessor = refactoring.getProcessor();
    NestedRefactoringContext.storeForProcessor(
        nestedProcessor,
        InterfaceRenameRefactoringContext.newNestedRefactoringContext(typeContainer.getBaseType()));

    return builder.createChange();
  }

  private boolean updateNewNameMetadata() {
    String newName = getTypeRefactoringSupport().getNewElement().getElementName();
    if (typeContainer.isSync()) {
      pairedNewName = RemoteServiceUtilities.computeAsyncTypeName(newName);
    } else {
      pairedNewName = RemoteServiceUtilities.computeSyncTypeName(newName);
    }
    return pairedNewName != null;
  }

  /**
   * Ensures each compilation unit change in the tree rooted at the given change
   * will be wrapped with a
   * {@link com.google.gwt.eclipse.core.refactoring.regionupdater.RegionUpdaterChange}
   * to update text regions at change perform-time.
   */
  private void weaveChange(Change change) {
    String oldName = typeContainer.getBaseType().getElementName();
    String newName = getTypeRefactoringSupport().getNewElement().getElementName();
    RenamedElementAstMatcher astMatcher = new RenamedElementAstMatcher(oldName,
        newName);

    String oldCuName = typeContainer.getBaseType().getCompilationUnit().getElementName();
    String newCuName = getTypeRefactoringSupport().getNewType().getCompilationUnit().getElementName();
    CompilationUnitRenamedReferenceUpdater cuValidator = new CompilationUnitRenamedReferenceUpdater(
        oldName, newName,
        oldCuName, newCuName);

    // Walk through the created change tree and weave a change that, at
    // perform-time, will update the text regions
    ChangeUtilities.acceptOnChange(change,
        new RegionUpdaterChangeWeavingVisitor(astMatcher, cuValidator));
  }

}
