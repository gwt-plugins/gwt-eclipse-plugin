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
package com.google.appengine.eclipse.core.validators.java;

import com.google.appengine.eclipse.core.AppEngineCorePluginLog;
import com.google.appengine.eclipse.core.markers.AppEngineJavaProblem;
import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.appengine.eclipse.core.properties.GaeProjectProperties;

import org.eclipse.core.internal.jobs.JobStatus;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.BuildContext;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.CompilationParticipant;
import org.eclipse.jdt.core.compiler.ReconcileContext;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.core.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Integrates into the JDT reconcile and build process to flag GAE-specific
 * errors and find Java references inside JSNI blocks.
 */
@SuppressWarnings("restriction")
public class JavaCompilationParticipant extends CompilationParticipant {

  private static final CategorizedProblem[] EMPTY_PROBLEMS = new CategorizedProblem[0];

  private static final Map<IProject, Job> validationJobs = new HashMap<IProject, Job>();

  public static void cleanBuildArtifacts(IProject project) {
    try {
      project.deleteMarkers(AppEngineJavaProblem.MARKER_ID, true,
          IResource.DEPTH_INFINITE);
    } catch (CoreException e) {
      AppEngineCorePluginLog.logError(e);
    }
  }

  public static List<? extends CategorizedProblem> validateCompilationUnit(
      ASTNode ast) {
    CompilationUnit root = (CompilationUnit) ast.getRoot();
    ICompilationUnit cu = (ICompilationUnit) root.getJavaElement();

    // If the compilation unit is not on the build classpath, return an empty
    // list of problems.
    if (!cu.getJavaProject().isOnClasspath(cu)) {
      return Collections.emptyList();
    }

    List<IPath> validationExclusionPatterns = GaeProjectProperties.getValidationExclusionPatterns(cu.getJavaProject().getProject());
    char[][] exclusionPatterns = null;
    if (!validationExclusionPatterns.isEmpty()) {
      exclusionPatterns = new char[validationExclusionPatterns.size()][];
      for (int i = 0; i < validationExclusionPatterns.size(); ++i) {
        exclusionPatterns[i] = validationExclusionPatterns.get(i).toString().toCharArray();
      }
    }

    // Get the source root relative path, since our exclusion filter does
    // not include the project name, but the compilation unit's path does.
    IPath sourceRelativePath = cu.getPath().removeFirstSegments(1);
    if (Util.isExcluded(sourceRelativePath, null, exclusionPatterns, false)) {
      return Collections.emptyList();
    }

    List<CategorizedProblem> problems = GoogleCloudSqlChecker.check(root, cu.getJavaProject());
    problems.addAll(GaeChecker.check(root, cu.getJavaProject()));
    
    return problems;
  }

  @Override
  public void buildStarting(final BuildContext[] files, boolean isBatch) {
    // We handle batch builds in a separate job to avoid blocking for a long
    // time in the case of batch (clean) builds.
    if (isBatch) {
      synchronized (this) {
        assert files.length > 0;
        IProject project = files[0].getFile().getProject();

        Job job = validationJobs.get(project);
        if (job != null) {
          job.cancel();
        }

        job = new Job("Validating GAE components") {
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
  }

  @Override
  public boolean isActive(IJavaProject project) {
    return GaeNature.isGaeProject(project.getProject());
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
        AppEngineCorePluginLog.logError("Could not get AST for " + cu.getPath());
        return;
      }

      // Add existing Java problems to the list of all problems
      ArrayList<CategorizedProblem> finalProblemSet = new ArrayList<CategorizedProblem>();
      CategorizedProblem[] currentProblems = context.getProblems(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER);
      if (currentProblems != null) {
        finalProblemSet.addAll(Arrays.asList(currentProblems));
      }

      // Find and add GAE-specific problems
      List<? extends CategorizedProblem> newProblems = validateCompilationUnit(ast);
      finalProblemSet.addAll(newProblems);

      context.putProblems(
          IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER,
          finalProblemSet.isEmpty() ? null
              : finalProblemSet.toArray(EMPTY_PROBLEMS));
    } catch (OperationCanceledException e) {
      // Thrown by Eclipse to abort long-running processes
      throw e;
    } catch (Exception e) {
      // Don't want to allow any unexpected exceptions to escape
      AppEngineCorePluginLog.logError(e,
          "Unexpected error while validating {0}", cu.getElementName());
    }
  }

  private void handleBuildStarting(BuildContext[] files,
      IProgressMonitor monitor) {
    for (BuildContext context : files) {
      IFile file = context.getFile();

      if (monitor != null) {
        // Return early if this is a canceled job. Note that the AST is
        // still being built as there is no way to signal an abort.
        if (monitor.isCanceled()) {
          return;
        }

        // Update the progress monitor.
        monitor.subTask(file.getName());
        monitor.worked(1);
      }

      try {
        ICompilationUnit cu = JavaCore.createCompilationUnitFrom(file);

        ASTNode ast = null;

        try {
          /*
           * Generally, the compilation unit will be consistent (the Java Model
           * matches the .java file on disk). However, in certain cases, such as
           * when the user undos a member rename refactoring, the two are out of
           * sync when the build starts. In these cases, we have to explicitly
           * reconcile the compilation unit with its underlying resource and use
           * the AST we get back for validation.
           */
          if (!cu.isConsistent()) {
            ast = cu.reconcile(AST.JLS3, true, null, null);
            assert (cu.isConsistent());
          } else {
            // Have JDT parse the compilation unit
            ASTParser parser = ASTParser.newParser(AST.JLS3);

            // TODO: Note I will resolve type bindings for now, but I might
            // be able to simply convert SimpleNames and QualifiedNames to
            // fully qualified names, thereby avoiding full binding resolution.
            parser.setResolveBindings(true);

            parser.setSource(cu);
            ast = parser.createAST(null);
          }
        } catch (JavaModelException e) {
          AppEngineCorePluginLog.logError(e);
          continue;
        }

        // Validate the Java AST and record any problems we find
        List<? extends CategorizedProblem> problems = validateCompilationUnit(ast);
        context.recordNewProblems(problems.toArray(EMPTY_PROBLEMS));
      } catch (OperationCanceledException e) {
        // Thrown by Eclipse to abort long-running processes
        throw e;
      } catch (Exception e) {
        // Don't want to allow any unexpected exceptions to escape
        AppEngineCorePluginLog.logError(e,
            "Unexpected error while validating {0}", file.getName());
      }
    }
  }
}
