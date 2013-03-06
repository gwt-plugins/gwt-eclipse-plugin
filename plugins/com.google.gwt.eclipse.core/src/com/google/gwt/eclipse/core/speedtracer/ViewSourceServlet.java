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
package com.google.gwt.eclipse.core.speedtracer;

import com.google.gdt.eclipse.core.JavaUtilities;
import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.SWTUtilities;
import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.java.Pair;
import com.google.gwt.eclipse.core.GWTPluginLog;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Receives requests to view a source file in Eclipse.
 */
@SuppressWarnings({"serial", "restriction"})
public class ViewSourceServlet extends HttpServlet {

  private static final String API_VERSION_1 = "1";

  private static final String MAX_SUPPORTED_API_VERSION = API_VERSION_1;

  private static Pair<Integer, String> logWarningAndReturnStatus(
      Integer statusCode, String message) {
    return new Pair<Integer, String>(statusCode, "{'message': '" + message
        + "'}");
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    try {
      Pair<Integer, String> result = handleGet(req, resp);
      Integer statusCode = result.getX();
      String message = result.getY();
      if (statusCode == HttpServletResponse.SC_OK) {
        resp.setStatus(statusCode);
      } else {
        if (message != null) {
          resp.sendError(statusCode, message);
          showErrorDialog(message);
        } else {
          resp.sendError(statusCode);
          showErrorDialog("Could not view the source file.");
        }
      }

    } catch (Throwable t) {
      GWTPluginLog.logError(t, "Could not view source file ("
          + req.getParameter("filePath") + ")");
      showErrorDialog("Could not view the source file, see log for details.");
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * @throws IOException if the file is not in the workspace or is not a JAR
   */
  private IFile getFile(String filePathString) throws IOException {
    Path filePath = new Path(filePathString);

    final IResource resource = ResourceUtils.getResource(filePath);
    if (resource == null) {
      throw new IOException(
          "Could not view source because the file is not in the workspace (\""
              + filePathString + "\").");
    }

    if (!(resource instanceof IFile)) {
      throw new IOException(
          "Could not view source because the path is not a file (\""
              + filePathString + "\").");
    }

    return (IFile) resource;
  }

  private IPackageFragmentRoot getPackageFragmentRoot(String jarPathString,
      String preferredProjectName)
      throws IOException {
    try {
      IFile jarIFile = getFile(jarPathString);

      // The JAR is in the workspace
      return JavaCore.createJarPackageFragmentRootFrom(jarIFile);
    } catch (IOException e) {
      // JAR must not be in the workspace (or is not a file), continue..
    }

    File jarFile = new File(jarPathString);

    // Iterate projects to find the external JAR
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    List<IProject> projects = new ArrayList<IProject>(
        Arrays.asList(root.getProjects()));
    IProject preferredProject = preferredProjectName != null
        ? root.getProject(preferredProjectName) : null;
    if (preferredProject != null && preferredProject.exists()) {
      // Search the preferred project first
      projects.remove(preferredProject);
      projects.add(0, preferredProject);
    }

    for (IProject project : projects) {
      IJavaProject javaProject = JavaCore.create(project);
      if (!javaProject.exists()) {
        continue;
      }

      try {
        IClasspathEntry[] classpathEntries = javaProject.getResolvedClasspath(true);
        for (IClasspathEntry classpathEntry : classpathEntries) {
          IPath path = classpathEntry.getPath();
          if (jarFile.equals(path.toFile())) {
            return javaProject.getPackageFragmentRoot(path.toOSString());
          }
        }

      } catch (JavaModelException e) {
        GWTPluginLog.logWarning(e, "Could not check " + project.getName()
            + " for JAR file");
        continue;
      }
    }

    return null;
  }

  /*
   * Taken from GotoLineAction.
   */
  /**
   * Jumps to the given line.
   * 
   * @param line the line to jump to
   */
  private void gotoLine(final ITextEditor editor, final int line) {
    Display.getDefault().syncExec(new Runnable() {
      public void run() {

        IDocumentProvider provider = editor.getDocumentProvider();
        IDocument document = provider.getDocument(editor.getEditorInput());
        try {

          int start = document.getLineOffset(line);
          editor.selectAndReveal(start, 0);

          IWorkbenchPage page = editor.getSite().getPage();
          page.activate(editor);

        } catch (BadLocationException x) {
          // ignore
        }
      }
    });
  }

  /**
   * @return the return status with the first object being the status code and
   *         the second an optional message
   * @throws IOException
   */
  private Pair<Integer, String> handleGet(HttpServletRequest req,
      HttpServletResponse resp) throws IOException {
    String apiVersion = req.getParameter("apiVersion");
    // TODO: general version compatability checking (this will come when it's
    // time for api version 2)
    if (!JavaUtilities.equalsWithNullCheck(apiVersion, API_VERSION_1)) {
      String message = "Could not view source because Speed Tracer is a newer than the Google Plugin for Eclipse, please check for updates to the Google Plugin for Eclipse.";
      GWTPluginLog.logWarning(message);
      return new Pair<Integer, String>(HttpServletResponse.SC_NOT_IMPLEMENTED,
          "{'maxSupportedApiVersion':'" + MAX_SUPPORTED_API_VERSION
              + "', 'message':" + message + "}");
    }

    String filePathParamValue = req.getParameter("filePath");
    if (StringUtilities.isEmpty(filePathParamValue)) {
      return logWarningAndReturnStatus(HttpServletResponse.SC_BAD_REQUEST,
          "Could not view source because the request is missing a filePath parameter.");
    }

    boolean isJar = false;
    String classpathRelativeFilePath = null;
    String fileUriString = filePathParamValue;
    // e.g. file:/path/to/SomeFile.java
    // e.g. jar:file:/path/to/gwt-user.jar!/com/google/gwt/SomeFile.java
    if (fileUriString.startsWith("jar:")) {
      int lastIndexOfExclamation = filePathParamValue.lastIndexOf('!');
      if (lastIndexOfExclamation == -1
          || lastIndexOfExclamation + 1 >= filePathParamValue.length()) {
        return logWarningAndReturnStatus(
            HttpServletResponse.SC_BAD_REQUEST,
            "Could not view source because the request refers to a JAR without specifying the file.");
      }

      classpathRelativeFilePath = filePathParamValue.substring(lastIndexOfExclamation + 1);
      fileUriString = filePathParamValue.substring("jar:".length(),
          lastIndexOfExclamation);
      isJar = true;
    }

    URI fileUri = URI.create(fileUriString);
    String scheme = fileUri.getScheme();
    if (!JavaUtilities.equalsWithNullCheck(scheme, "file")) {
      return logWarningAndReturnStatus(HttpServletResponse.SC_BAD_REQUEST,
          "Could not view source because the request refers to an unsupported type ("
              + filePathParamValue + ").");
    }

    String filePath = new File(fileUri).getPath();
    if (StringUtilities.isEmpty(filePath)) {
      return logWarningAndReturnStatus(HttpServletResponse.SC_BAD_REQUEST,
          "Could not view source because the request did not specify a file.");
    }

    IEditorPart part;
    if (isJar) {
      String projectName = req.getParameter(SymbolManifestGenerator.PROJECT_KEY);
      part = openEditorForFileInJar(filePath, classpathRelativeFilePath,
          projectName);
    } else {
      part = openEditorForFile(filePath);
    }

    String lineNumberString = req.getParameter("lineNumber");
    try {
      jumpToLineAndTakeFocus(part, Integer.parseInt(lineNumberString));
    } catch (NumberFormatException e) {
      GWTPluginLog.logWarning(e,
          "Could not jump to the particular line number because it is not an integer ("
              + lineNumberString + ")");
    }

    return new Pair<Integer, String>(HttpServletResponse.SC_OK, null);
  }

  /**
   * Must be called from UI thread.
   */
  private void jumpToLineAndTakeFocus(final IEditorPart editorPart,
      final int lineNumber) {

    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        if (lineNumber > 0) {
          if (editorPart instanceof ITextEditor) {
            ITextEditor editor = (ITextEditor) editorPart;
            gotoLine(editor, Math.max(0, lineNumber - 1));
          } else {
            GWTPluginLog.logError("Could not jump to line because the given editor part is not a text editor");
          }
        }

        // Nasty hack for getting SWT to take focus (this does not work on all
        // platforms, e.g. Linux)
        Shell shell = editorPart.getSite().getShell();
        if (shell == null) {
          GWTPluginLog.logError("Could not jump to line because the shell is null");
          return;
        }

        shell.setMinimized(false);
        shell.forceActive();
        shell.forceFocus();

        editorPart.setFocus();
      }
    });
  }

  private IEditorPart openEditorForFile(String filePathString) {
    IFile file;
    try {
      file = getFile(filePathString);
    } catch (IOException e) {
      GWTPluginLog.logError(e);
      return null;
    }

    final IFile finalFile = file;
    final IEditorPart[] part = new IEditorPart[1];
    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null) {
          GWTPluginLog.logWarning("Could not view source because the workbench window is null");
          return;
        }

        final IWorkbenchPage page = window.getActivePage();
        if (page == null) {
          GWTPluginLog.logWarning("Could not view source because the active page is null");
          return;
        }

        try {
          part[0] = IDE.openEditor(page, finalFile, true);
        } catch (PartInitException e) {
          GWTPluginLog.logError(e, "Could not view source");
          return;
        }
      }
    });

    return part[0];
  }

  private IEditorPart openEditorForFileInJar(String jarPathString,
      String classpathRelativeFilePathString, String preferredProjectName) {
    IPackageFragmentRoot jarPackageFragmentRoot;
    try {
      jarPackageFragmentRoot = getPackageFragmentRoot(jarPathString,
          preferredProjectName);
    } catch (IOException e) {
      GWTPluginLog.logError(e);
      return null;
    }

    if (jarPackageFragmentRoot == null || !jarPackageFragmentRoot.exists()) {
      GWTPluginLog.logError("Could not view source because the file at \""
          + jarPathString
          + "\" may not be a valid JAR, or may not contain java elements");
      return null;
    }

    IPath classpathRelativeFilePath = new Path(classpathRelativeFilePathString);
    IPath packagePath = classpathRelativeFilePath.removeLastSegments(1);
    String packageName = JavaUtilities.getPackageNameFromPath(packagePath);

    IPackageFragment packageFragment = jarPackageFragmentRoot.getPackageFragment(packageName);
    if (!packageFragment.exists()) {
      GWTPluginLog.logError("Could not view source because the package "
          + packageName + " inside " + jarPathString + " could not be found");
      return null;
    }

    String fileName = classpathRelativeFilePath.lastSegment();
    String classFileName;
    if (ResourceUtils.endsWith(fileName, ".java")) {
      classFileName = fileName.substring(0, fileName.length()
          - ".java".length())
          + ".class";
    } else if (ResourceUtils.endsWith(fileName, ".class")) {
      classFileName = fileName;
    } else {
      GWTPluginLog.logError("Could not view source because cannot handle file type of "
          + fileName + " inside a JAR");
      return null;
    }

    try {
      final IPackageFragment pf = packageFragment;
      final String finalClassFileName = classFileName;
      final IEditorPart[] part = new IEditorPart[1];
      Display.getDefault().syncExec(new Runnable() {
        public void run() {
          try {
            part[0] = EditorUtility.openInEditor(
                pf.getClassFile(finalClassFileName), true);
          } catch (Throwable e) {
            GWTPluginLog.logError(e, "Could not open java editor");
          }
        }
      });
      return part[0];
    } catch (Throwable e) {
      GWTPluginLog.logError(e, "Could not get files from the package "
          + packageFragment);
      return null;
    }
  }

  private void showErrorDialog(final String message) {
    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        MessageDialog.openError(SWTUtilities.getShell(),
            "Error viewing source", message);
      }
    });
  }
}
