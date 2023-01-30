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
package com.google.gwt.eclipse.core.clientbundle;

import com.google.gdt.eclipse.core.JavaUtilities;
import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.TypeCreator;
import com.google.gdt.eclipse.core.java.JavaModelSearch;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.util.GwtProblemsTreeLogger;
import com.google.gwt.resources.css.ExtractClassNamesVisitor;
import com.google.gwt.resources.css.GenerateCssAst;
import com.google.gwt.resources.css.ast.CssStylesheet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage.ImportsManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * CssResource subtype to be added to a ClientBundle interface.
 */
@SuppressWarnings("restriction")
public class GeneratedCssResource extends ClientBundleResource {

  private static class CssParseResult {

    private final String fileName;

    private final GwtProblemsTreeLogger logger;

    private IStatus status;

    private final CssStylesheet stylesheet;

    public CssParseResult(String fileName, CssStylesheet stylesheet,
        GwtProblemsTreeLogger logger) {
      this.fileName = fileName;
      this.stylesheet = stylesheet;
      this.logger = logger;
    }

    public IStatus getStatus() {
      if (status == null) {
        createStatus();
      }
      return status;
    }

    public CssStylesheet getStylesheet() {
      return stylesheet;
    }

    private void createStatus() {
      StringBuilder sb = new StringBuilder();
      int severity = IStatus.OK;

      if (logger.hasProblems()) {
        sb.append("There were problems parsing '" + fileName + "'. ");

        List<String> errors = logger.getErrors();
        if (errors.size() > 0) {
          severity = IStatus.ERROR;
          sb.append("\n\nERRORS:\n");
          sb.append(StringUtilities.join(errors, "\n"));
        } else {
          sb.append("A CssResource subtype was still generated.");
        }

        List<String> warnings = logger.getWarnings();
        if (warnings.size() > 0) {
          // Errors take precedence over warnings
          if (severity != IStatus.ERROR) {
            severity = IStatus.WARNING;
          }
          sb.append("\n\nWARNINGS:\n");
          sb.append(StringUtilities.join(warnings, "\n"));
        }
      }

      // Always an error if no stylesheet AST was generated
      if (stylesheet == null) {
        severity = IStatus.ERROR;
      }

      status = new Status(severity, GWTPlugin.PLUGIN_ID, sb.toString());
    }
  }

  private static String[] getExistingTopLevelTypeNames(IPackageFragment pckg)
      throws JavaModelException {
    List<String> typeNames = new ArrayList<String>();
    for (IType type : JavaModelSearch.findAllTopLevelTypes(pckg)) {
      typeNames.add(type.getElementName());
    }
    return typeNames.toArray(new String[0]);
  }

  private IType customCssResourceType;

  public GeneratedCssResource(IFile file, String methodName) {
    super(file, methodName, ClientBundleUtilities.CSS_RESOURCE_TYPE_NAME);
  }

  @Override
  public void addToClientBundle(IType clientBundle, ImportsManager imports,
      boolean addComments, IProgressMonitor monitor) throws CoreException {
    createType(clientBundle.getPackageFragment(), addComments);
    super.addToClientBundle(clientBundle, imports, addComments, monitor);
  }

  @Override
  public MethodDeclaration createMethodDeclaration(IType clientBundle,
      ASTRewrite astRewrite, ImportRewrite importRewrite, boolean addComments)
      throws CoreException {
    createType(clientBundle.getPackageFragment(), addComments);
    return super.createMethodDeclaration(clientBundle, astRewrite,
        importRewrite, addComments);
  }

  @Override
  protected String getReturnTypeName() {
    return customCssResourceType.getFullyQualifiedName('.');
  }

  private String computeCssClassMethodSource(IType type, String cssClass) {
    StringBuilder sb = new StringBuilder();

    String methodName = cssClass;
    if (!JavaUtilities.isGoodMethodName(methodName)) {
      methodName = JavaUtilities.sanitizeMethodName(methodName);

      // Refer to the CSS class name with @ClassName
      sb.append("@ClassName(\"");
      sb.append(cssClass);
      sb.append("\")");
      sb.append(StubUtility.getLineDelimiterUsed(type.getJavaProject()));
    }

    // Method returns a String representing the (obfuscated) class name
    sb.append("String ");
    sb.append(methodName);
    sb.append("();");

    return sb.toString();
  }

  private void createType(IPackageFragment pckg, boolean addComments)
      throws CoreException {
    IJavaProject javaProject = pckg.getJavaProject();
    final IProgressMonitor monitor = new NullProgressMonitor();

    // Method name should already have been sanitized and validated, so all we
    // should have to do to get a type name is just capitalize it
    String simpleName = StringUtilities.capitalize(getMethodName());

    // See if the type name is already used
    String qualifiedName = JavaModelUtil.concatenateName(pckg.getElementName(),
        simpleName);
    IType existingType = JavaModelSearch.findType(javaProject, qualifiedName);
    if (existingType != null) {
      if (ClientBundleUtilities.isCssResource(javaProject, existingType)) {
        // If the existing type is a CssResource, we'll assume that it wraps
        // this CSS file and use it for our ClientBundle accessor return type
        // instead of trying to generate another CssResource here.
        customCssResourceType = existingType;
        return;
      } else {
        // If it's not a CssResource, then we'll need to generate a CssResource
        // ourself, but we can't use the name. So, let's compute a similar name
        // that is not already in use.
        simpleName = StringUtilities.computeUniqueName(
            getExistingTopLevelTypeNames(pckg), simpleName);
      }
    }

    // Parse the CSS and see if there were problems
    CssParseResult result = parseCss();
    final IStatus status = result.getStatus();

    // Bail out when errors occur
    if (status.getSeverity() == IStatus.ERROR) {
      throw new CoreException(status);
    }

    // For warnings, just display them in a dialog (on the UI thread of course)
    // TODO: would nice if we could aggregate these and show them all at the end
    if (status.getSeverity() == IStatus.WARNING) {
      Display.getDefault().syncExec(new Runnable() {
        @Override
        public void run() {
          MessageDialog.openWarning(null, "CSS Parsing", status.getMessage());
        }
      });
    }

    // Extract the CSS class names
    final Set<String> cssClassNames = ExtractClassNamesVisitor.exec(result.getStylesheet());

    TypeCreator gen = new TypeCreator(pckg, simpleName,
        TypeCreator.ElementType.INTERFACE,
        new String[] {ClientBundleUtilities.CSS_RESOURCE_TYPE_NAME},
        addComments) {
      @Override
      protected void createTypeMembers(IType newType, ImportRewrite imports)
          throws CoreException {
        // Create an accessor method for each CSS class
        for (String cssClass : cssClassNames) {
          newType.createMethod(computeCssClassMethodSource(newType, cssClass),
              null, true, monitor);
        }
      }
    };
    customCssResourceType = gen.createType();
  }

  private CssParseResult parseCss() {
    CssStylesheet cssAst = null;
    GwtProblemsTreeLogger logger = new GwtProblemsTreeLogger();

    IFile cssFile = getFile();
    try {
      // Have GWT parse the CSS file
      URL cssUrl = cssFile.getLocationURI().toURL();
      cssAst = GenerateCssAst.exec(logger, new URL[] {cssUrl});
    } catch (MalformedURLException e) {
      GWTPluginLog.logError(e);
    } catch (UnableToCompleteException e) {
      // Ignore this, since we'll get the error in the TreeLogger
    }

    return new CssParseResult(cssFile.getName(), cssAst, logger);
  }

}
