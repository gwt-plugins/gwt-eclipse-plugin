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
package com.google.gwt.eclipse.core.uibinder.resources;

import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.uibinder.UiBinderConstants;
import com.google.gwt.eclipse.core.uibinder.UiBinderException;
import com.google.gwt.eclipse.core.uibinder.formatter.UiBinderFormatter;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage.ImportsManager;

import java.io.InputStream;
import java.util.List;

/**
 * Generates code for UiBinder templates and owner classes.
 */
public abstract class UiBinderResourceCreator {

  private static final String GWT_TYPE_NAME = "com.google.gwt.core.client.GWT";

  protected static String createElement(String outerTagName,
      boolean addComments, String commentResourceName,
      boolean addSampleContent, String sampleContentResourceName)
      throws CoreException {
    StringBuilder sb = new StringBuilder();
    sb.append("<" + outerTagName + ">\n");
    if (addComments) {
      sb.append(getResourceWithNoReplacements(commentResourceName));
      sb.append('\n');
    }
    if (addSampleContent) {
      sb.append(getResourceWithNoReplacements(sampleContentResourceName));
      sb.append('\n');
    }
    sb.append("</" + outerTagName + ">");

    return sb.toString();
  }

  protected static String createStyleElement(boolean addComments,
      boolean addSampleContent) throws CoreException {
    StringBuilder sb = new StringBuilder();
    sb.append("<ui:style>\n");
    if (addComments) {
      sb.append(getResourceWithNoReplacements("uixml-css-comment.template"));
      sb.append('\n');
    }
    if (addSampleContent) {
      sb.append(getResourceWithNoReplacements("uixml-css-sample-content.template"));
      sb.append('\n');
    }
    sb.append("</ui:style>");

    return sb.toString();
  }

  protected static String createUiBinderStaticField(IType ownerClass,
      ImportsManager imports) {
    String uiBinderTypeName = ownerClass.getElementName() + "UiBinder";

    StringBuilder sb = new StringBuilder();
    sb.append("private static ");
    sb.append(uiBinderTypeName);
    sb.append(" uiBinder = ");
    sb.append(imports.addImport(GWT_TYPE_NAME));
    sb.append(".create(");
    sb.append(uiBinderTypeName);
    sb.append(".class);");

    return sb.toString();
  }

  protected static String createUiBinderSubtype(IType ownerClass,
      String uiRootTypeName, ImportsManager imports) {
    StringBuilder sb = new StringBuilder();

    sb.append("interface ");
    sb.append(ownerClass.getElementName());
    sb.append("UiBinder");
    sb.append(" extends ");
    sb.append(imports.addImport(UiBinderConstants.UI_BINDER_TYPE_NAME));
    sb.append("<");
    sb.append(imports.addImport(uiRootTypeName));
    sb.append(", ");
    sb.append(imports.addImport(ownerClass.getFullyQualifiedName()));
    sb.append(">");
    sb.append(" {}");

    return sb.toString();
  }

  protected static String createUiField(String fieldTypeName, String fieldName,
      ImportsManager imports) {
    StringBuilder sb = new StringBuilder();

    sb.append("@");
    sb.append(imports.addImport(UiBinderConstants.UI_FIELD_TYPE_NAME));
    sb.append(" ");
    sb.append(imports.addImport(fieldTypeName));
    sb.append(" ");
    sb.append(fieldName);
    sb.append(";");

    return sb.toString();
  }

  private static Object getResourceWithNoReplacements(String resourceName)
      throws CoreException {
    return ResourceUtils.getResourceAsString(UiBinderResourceCreator.class,
        resourceName);
  }

  public abstract void createOwnerClassMembers(IType ownerClass,
      ImportsManager imports, boolean addComments, boolean addSampleContent,
      IProgressMonitor monitor) throws JavaModelException;

  public IFile createUiXmlFile(IPath filePath, boolean addComments,
      boolean addSampleContent) throws CoreException {
    InputStream contents = createUiXmlSource(addComments, addSampleContent);
    IFile file = ResourceUtils.createFile(filePath, contents);
    try {
      UiBinderFormatter.format(file, true);
    } catch (UiBinderException e) {
      throw new CoreException(StatusUtilities.newErrorStatus(e,
          GWTPlugin.PLUGIN_ID));
    }

    return file;
  }

  public abstract String getOwnerSuperclass();

  public abstract List<String> getOwnerSuperinterfaces(boolean addSampleContent);

  protected abstract String createMarkupElement(boolean addComments,
      boolean addSampleContent) throws CoreException;

  protected abstract InputStream createUiXmlSource(boolean addComments,
      boolean addSampleContent) throws CoreException;

}
