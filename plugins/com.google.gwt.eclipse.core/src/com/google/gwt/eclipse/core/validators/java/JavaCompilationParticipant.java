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
package com.google.gwt.eclipse.core.validators.java;

import com.google.gdt.eclipse.core.BuilderUtilities;
import com.google.gdt.eclipse.core.JavaASTUtils;
import com.google.gdt.eclipse.core.validation.ValidationResult;
import com.google.gwt.dev.util.Empty;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.clientbundle.ClientBundleResourceDependencyIndex;
import com.google.gwt.eclipse.core.markers.ClientBundleProblem;
import com.google.gwt.eclipse.core.markers.GWTJavaProblem;
import com.google.gwt.eclipse.core.nature.GWTNature;
import com.google.gwt.eclipse.core.search.IIndexedJavaRef;
import com.google.gwt.eclipse.core.search.IndexedJsniJavaRef;
import com.google.gwt.eclipse.core.search.JavaRefIndex;
import com.google.gwt.eclipse.core.search.JsniJavaRefParamType;
import com.google.gwt.eclipse.core.uibinder.UiBinderConstants;
import com.google.gwt.eclipse.core.uibinder.model.UiBinderSubtypeToOwnerIndex;
import com.google.gwt.eclipse.core.uibinder.model.UiBinderSubtypeToOwnerIndex.UiBinderSubtypeAndOwner;
import com.google.gwt.eclipse.core.uibinder.model.UiBinderSubtypeToUiXmlIndex;
import com.google.gwt.eclipse.core.uibinder.model.reference.UiBinderReferenceManager;
import com.google.gwt.eclipse.core.uibinder.problems.java.UiBinderJavaProblem;
import com.google.gwt.eclipse.core.uibinder.validators.UiBinderJavaValidator;
import com.google.gwt.eclipse.core.validators.clientbundle.ClientBundleValidator;
import com.google.gwt.eclipse.core.validators.rpc.RemoteServiceProblem;
import com.google.gwt.eclipse.core.validators.rpc.RemoteServiceValidator;

import org.eclipse.core.internal.jobs.JobStatus;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.BuildContext;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.CompilationParticipant;
import org.eclipse.jdt.core.compiler.ReconcileContext;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.corext.dom.ASTBatchParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Integrates into the JDT reconcile and build process to flag GWT-specific
 * errors and find Java references inside JSNI blocks.
 */
public class JavaCompilationParticipant extends CompilationParticipant {

  private static final String[] NO_STRINGS = new String[0];

  private static final CategorizedProblem[] EMPTY_PROBLEMS = new CategorizedProblem[0];

  private static final ICompilationUnit[] NO_UNITS = new ICompilationUnit[0];

  private static final Map<IProject, Job> validationJobs = new HashMap<IProject, Job>();

  public static void cleanBuildArtifacts(IProject project) {
    try {
      project.deleteMarkers(GWTJavaProblem.MARKER_ID, true,
          IResource.DEPTH_INFINITE);

      // Clear Java ref index entries for this project
      JavaRefIndex.getInstance().clear(project);

      // Clear ClientBundle resource index entries
      ClientBundleResourceDependencyIndex.getInstance().clear(project);

      // Clear UiBinder source references from the various indices
      UiBinderReferenceManager.INSTANCE.getReferenceManager().removeSourceReferences(
          project);
      UiBinderReferenceManager.INSTANCE.getSubtypeToOwnerIndex().clear(project);
      UiBinderReferenceManager.INSTANCE.getSubtypeToUiXmlIndex().clear(project);
      UiBinderReferenceManager.INSTANCE.getUiXmlReferencedFieldIndex().clear(
          JavaCore.create(project));
    } catch (CoreException e) {
      GWTPluginLog.logError(e);
    }
  }

  public static JavaValidationResult validateCompilationUnit(ASTNode ast) {
    ICompilationUnit cu = JavaASTUtils.getCompilationUnit(ast);

    // If the compilation unit is not on the build classpath, return an empty
    // list of problems/JSNI Java references.
    if (!cu.getJavaProject().isOnClasspath(cu)) {
      return new JavaValidationResult();
    }

    // Walk the Java AST to find problems and Java references in JSNI blocks
    JavaValidationVisitor visitor = new JavaValidationVisitor();
    ast.accept(visitor);

    /*
     * Index the Java references in the compilation unit, but only if we're
     * doing a reconcile from the Java Editor or from our direct call from
     * JsniReferenceChange.perform method. There will be no working copy owner
     * in both of these cases. However, during the pre-refactoring checks, the
     * JDT will call reconcile on a working copy with a JavaRenameProcessor as
     * the working copy owner. If we index the Java refs during that reconcile,
     * which is based on a particular JDT refactoring, we may have inaccurate
     * offsets when we go to calculate the edits for our own refactoring.
     */
    if (cu.getOwner() == null) {
      List<JsniJavaRef> jsniJavaRefs = visitor.getValidationResult().getJavaRefs();
      indexJavaRefs(cu, jsniJavaRefs);
    }

    return visitor.getValidationResult();
  }

  private static void indexJavaRefs(ICompilationUnit cu,
      List<JsniJavaRef> jsniRefs) {
    Set<IIndexedJavaRef> indexRefs = new HashSet<IIndexedJavaRef>(
        jsniRefs.size());

    for (JsniJavaRef jsniRef : jsniRefs) {
      // Add the main JSNI ref
      indexRefs.add(new IndexedJsniJavaRef(jsniRef));

      // For method refs, add each parameter type reference to the index also
      if (jsniRef.isMethod() && !jsniRef.matchesAnyOverload()) {
        int paramTypeOffset = jsniRef.getParamTypesOffset();
        if (paramTypeOffset > -1) {
          for (String paramType : jsniRef.paramTypes()) {
            IIndexedJavaRef paramTypeRef = JsniJavaRefParamType.parse(
                jsniRef.getSource(), paramTypeOffset, paramType);
            if (paramTypeRef != null) {
              indexRefs.add(paramTypeRef);
            }
            paramTypeOffset += paramType.length();
          }
        }
      }
    }

    // Add all the Java references to the index
    JavaRefIndex.getInstance().add(cu.getPath(), indexRefs);
  }

  @Override
  public void buildStarting(final BuildContext[] files, boolean isBatch) {
    // We handle batch builds in a separate job to avoid blocking for a long
    // time in the case of batch (clean) builds.
    if (isBatch) {
      assert files.length > 0;
      IProject project = files[0].getFile().getProject();

      synchronized (this) {
        Job job = validationJobs.get(project);
        if (job != null) {
          job.cancel();
        }

        job = new Job("Validating GWT components") {
          @Override
          protected IStatus run(IProgressMonitor monitor) {

            monitor.beginTask("validating", files.length);
            handleBuildStarting(files, monitor);
            monitor.done();

            return new JobStatus(Status.OK, this, "done");
          }
        };

        validationJobs.put(project, job);
        job.schedule();
      }
    } else {
      handleBuildStarting(files, null);
    }
  }

  @Override
  public void cleanStarting(IJavaProject project) {
    // Cancel the current validation job.
    synchronized (this) {
      Job buildJob = validationJobs.get(project.getProject());
      if (buildJob != null) {
        buildJob.cancel();
      }
    }

    cleanBuildArtifacts(project.getProject());
  }

  @Override
  public boolean isActive(IJavaProject project) {
    try {
      boolean active = project.exists()
          && project.getProject().hasNature(GWTNature.NATURE_ID);
      return active;
    } catch (CoreException e) {
      GWTPluginLog.logError(e);
      return false;
    }
  }

  @Override
  public void reconcile(ReconcileContext context) {
    ICompilationUnit cu = context.getWorkingCopy();

    try {
      CompilationUnit ast = null;

      try {
        ast = context.getAST3();
      } catch (JavaModelException e) {
        // Fall through to null check below
      }
      if (ast == null) {
        GWTPluginLog.logError("Could not get AST for " + cu.getPath());
        return;
      }

      // TODO: Merge this code with that of buildStarting

      CategorizedProblem[] currentProblems = context.getProblems(GWTJavaProblem.MARKER_ID);
      // GWT-validation and error reporting
      JavaValidationResult result = validateCompilationUnit(ast);
      List<GWTJavaProblem> gwtCoreProblems = result.getProblems();
      ArrayList<CategorizedProblem> finalProblemSet = new ArrayList<CategorizedProblem>();
      if (currentProblems != null) {
        finalProblemSet.addAll(Arrays.asList(currentProblems));
      }
      finalProblemSet.addAll(gwtCoreProblems);
      context.putProblems(GWTJavaProblem.MARKER_ID, (finalProblemSet.size() > 0
          ? finalProblemSet.toArray(EMPTY_PROBLEMS) : null));

      // GWT RPC validation and error reporting
      RemoteServiceValidator rsv = new RemoteServiceValidator();
      ValidationResult validationResult = rsv.validate(ast);
      List<CategorizedProblem> rpcProblems = validationResult.getProblems();
      context.putProblems(RemoteServiceProblem.MARKER_ID,
          (rpcProblems.size() > 0 ? rpcProblems.toArray(EMPTY_PROBLEMS) : null));

      // ClientBundle validation
      ClientBundleValidator cbv = new ClientBundleValidator();
      ValidationResult cbvResult = cbv.validate(ast);
      List<CategorizedProblem> cbProblems = cbvResult.getProblems();
      context.putProblems(ClientBundleProblem.MARKER_ID, (cbProblems.size() > 0
          ? cbProblems.toArray(EMPTY_PROBLEMS) : null));

      if (UiBinderConstants.UI_BINDER_ENABLED) {
        /*
         * Set up the UiBinder validator. Note that we're passing in copies of
         * the subtype-to-owner and subtype-to-xml indices instead of using the
         * "real" indices (i.e. the one that we update during builds). This
         * ensures that any updates the validator makes during a reconcile are
         * only used by that reconcile pass, and are not persisted.
         */
        UiBinderJavaValidator uiv = new UiBinderJavaValidator(ast,
            new UiBinderSubtypeToOwnerIndex(
                UiBinderReferenceManager.INSTANCE.getSubtypeToOwnerIndex()),
            new UiBinderSubtypeToUiXmlIndex(
                UiBinderReferenceManager.INSTANCE.getSubtypeToUiXmlIndex()),
            UiBinderReferenceManager.INSTANCE.getUiXmlReferencedFieldIndex(),
            null);
        ValidationResult uivResult = uiv.validate();
        List<CategorizedProblem> uivProblems = uivResult.getProblems();
        context.putProblems(UiBinderJavaProblem.MARKER_ID,
            (uivProblems.size() > 0 ? uivProblems.toArray(EMPTY_PROBLEMS)
                : null));
      }
    } catch (OperationCanceledException e) {
      // Thrown by Eclipse to abort long-running processes
      throw e;
    } catch (Exception e) {
      // Don't want to allow any unexpected exceptions to escape
      GWTPluginLog.logError(e, "Unexpected error while validating {0}",
          cu.getElementName());
    }
  }

  private void handleBuildStarting(BuildContext[] files,
      final IProgressMonitor monitor) {
    UiBinderSubtypeToOwnerIndex prebuildOwnerIndex = new UiBinderSubtypeToOwnerIndex(
        UiBinderReferenceManager.INSTANCE.getSubtypeToOwnerIndex());
    final LinkedHashMap<ICompilationUnit, BuildContext> compilationUnitToBuildContext = new LinkedHashMap<ICompilationUnit, BuildContext>();

    for (BuildContext buildContext : files) {
      ICompilationUnit cu = JavaCore.createCompilationUnitFrom(buildContext.getFile());
      compilationUnitToBuildContext.put(cu, buildContext);
    }

    final Set<ICompilationUnit> validatedCompilationUnits = new HashSet<ICompilationUnit>();

    /*
     * ASTBatchParser processes the ICompilationUnits in batches based on the
     * available memory in the system. Note that we never cache the ASTs they
     * are only live for the duration of the callback below. Empirically, trying
     * to cache all ASTs for gwt-user project results in an OOM.
     */
    new ASTBatchParser().createASTs(
        compilationUnitToBuildContext.keySet().toArray(NO_UNITS), NO_STRINGS,
        new ASTRequestor() {
          @Override
          public void acceptAST(ICompilationUnit source, CompilationUnit ast) {
            BuildContext buildContext = compilationUnitToBuildContext.get(source);
            IFile file = buildContext.getFile();

            if (monitor != null) {
              // Return early if this is a canceled job. Note that the AST is
              // still being built as there is no way to signal an abort.
              if (monitor.isCanceled()) {
                return;
              }

              // Update the progress monitor.
              monitor.subTask(source.getElementName());
              monitor.worked(1);
            }

            try {
              ICompilationUnit cu = source;

              validatedCompilationUnits.add(cu);

              try {
                /*
                 * Generally, the compilation unit will be consistent (the Java
                 * Model matches the .java file on disk). However, in certain
                 * cases, such as when the user undos a member rename
                 * refactoring, the two are out of sync when the build starts.
                 * In these cases, we have to explicitly reconcile the
                 * compilation unit with its underlying resource and use the AST
                 * we get back for validation.
                 */
                if (!cu.isConsistent()) {
                  ast = cu.reconcile(AST.JLS3, true, null, null);
                  assert (cu.isConsistent());
                }
              } catch (JavaModelException e) {
                GWTPluginLog.logError(e);
                return;
              }

              // TODO: Merge this code with that of reconcile

              // Validate the Java AST and record any GWT problems we find
              JavaValidationResult result = validateCompilationUnit(ast);
              List<CategorizedProblem> problems = new ArrayList<CategorizedProblem>(
                  result.getProblems());

              RemoteServiceValidator rsv = new RemoteServiceValidator();
              ValidationResult validationResult = rsv.validate(ast);
              problems.addAll(validationResult.getProblems());

              ClientBundleValidator cbv = new ClientBundleValidator();
              ValidationResult cbvResult = cbv.validate((CompilationUnit) ast);
              problems.addAll(cbvResult.getProblems());

              ValidationResult uivResult;
              if (UiBinderConstants.UI_BINDER_ENABLED) {
                UiBinderJavaValidator uiv = new UiBinderJavaValidator(
                    (CompilationUnit) ast,
                    UiBinderReferenceManager.INSTANCE.getSubtypeToOwnerIndex(),
                    UiBinderReferenceManager.INSTANCE.getSubtypeToUiXmlIndex(),
                    UiBinderReferenceManager.INSTANCE.getUiXmlReferencedFieldIndex(),
                    UiBinderReferenceManager.INSTANCE.getReferenceManager());
                uivResult = uiv.validate();
                problems.addAll(uivResult.getProblems());
              }

              // Record the problems
              buildContext.recordNewProblems(problems.toArray(EMPTY_PROBLEMS));

              // Get all Java types references from JSNI blocks in this file
              List<String> typeDependencies = new ArrayList<String>();
              for (JsniJavaRef javaRef : result.getJavaRefs()) {
                if (!typeDependencies.contains(javaRef.dottedClassName())) {
                  typeDependencies.add(javaRef.dottedClassName());
                }
              }

              // Add the RPC dependencies
              typeDependencies.addAll(validationResult.getTypeDependencies());

              if (UiBinderConstants.UI_BINDER_ENABLED) {
                // Add the UiBinder dependencies
                typeDependencies.addAll(uivResult.getTypeDependencies());
              }

              // Record the JSNI dependencies so that changing any referenced
              // types
              // will automatically trigger a rebuild of this file
              buildContext.recordDependencies(typeDependencies.toArray(Empty.STRINGS));
            } catch (OperationCanceledException e) {
              // Thrown by Eclipse to abort long-running processes
              throw e;
            } catch (Exception e) {
              // Don't want to allow any unexpected exceptions to escape
              GWTPluginLog.logError(e, "Unexpected error while validating {0}",
                  file.getName());
            }
          }
        }, null);

    if (UiBinderConstants.UI_BINDER_ENABLED) {
      revalidateOwnerTypes(prebuildOwnerIndex, validatedCompilationUnits);
    }
  }

  /**
   * Re-validates the types which are current/former owner classes, and which
   * may need another validation pass (since they might not have been recognized
   * as owner classes on the first pass).
   * 
   * @param preBuildOwnerIndex the pre-build owner types
   * @param validatedCompilationUnits units already validated
   */
  private void revalidateOwnerTypes(
      UiBinderSubtypeToOwnerIndex preBuildOwnerIndex,
      Set<ICompilationUnit> validatedCompilationUnits) {
    UiBinderSubtypeToOwnerIndex postBuildOwnerIndex = UiBinderReferenceManager.INSTANCE.getSubtypeToOwnerIndex();

    // Compute the union of all current and former UiBinder+Owner pairs
    Set<UiBinderSubtypeAndOwner> subtypesAndOwners = new HashSet<UiBinderSubtypeAndOwner>(
        postBuildOwnerIndex.getAllUiBinderTypesAndOwners());
    subtypesAndOwners.addAll(preBuildOwnerIndex.getAllUiBinderTypesAndOwners());

    // Compute the set of owner type compilation units we need to re-validate
    Set<ICompilationUnit> cusToTouch = new HashSet<ICompilationUnit>();
    for (UiBinderSubtypeAndOwner entry : subtypesAndOwners) {
      // Only re-validate owner types for UiBinder subtypes we just validated
      if (validatedCompilationUnits.contains(entry.getUiBinderType().getCompilationUnit())) {
        IType ownerType = entry.findOwnerType();
        /*
         * Optimization: Don't need to re-validate owner classes which live in
         * the same source file as the UiBinder subtype, since we know we
         * recognized them on the original validation pass.
         */
        if (ownerType != null && !entry.hasCommonCompilationUnit()) {
          ICompilationUnit compilationUnit = ownerType.getCompilationUnit();
          // Binary types will have null compilation units
          if (compilationUnit != null && compilationUnit.exists()) {
            cusToTouch.add(compilationUnit);
          }
        }
      }
    }

    if (!cusToTouch.isEmpty()) {
      BuilderUtilities.revalidateCompilationUnits(cusToTouch,
          "Revalidating UiBinder owner classes");
    }
  }

}
