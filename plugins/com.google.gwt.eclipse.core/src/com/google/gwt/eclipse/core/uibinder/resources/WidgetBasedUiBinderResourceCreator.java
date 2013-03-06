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
import com.google.gwt.eclipse.core.uibinder.UiBinderConstants;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage.ImportsManager;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates code for UiBinder interfaces based on GWT widgets.
 */
public class WidgetBasedUiBinderResourceCreator extends UiBinderResourceCreator {

  private static String createCtor(IType ownerClass, boolean addSampleContent,
      boolean addComments) {
    StringBuilder sb = new StringBuilder();

    sb.append("public ");
    sb.append(ownerClass.getElementName());
    sb.append("(");

    if (addSampleContent) {
      sb.append("String firstName");
    }

    sb.append(") {\n");
    sb.append("\tinitWidget(uiBinder.createAndBindUi(this));\n");

    if (addSampleContent) {
      if (addComments) {
        sb.append('\n');
        sb.append("\t// Can access @UiField after calling createAndBindUi\n");
      }
      sb.append("\tbutton.setText(firstName);\n");
    }

    sb.append("}");

    return sb.toString();
  }

  private static String createDefaultCtor(IType ownerClass, boolean addComments) {
    StringBuilder sb = new StringBuilder();

    if (addComments) {
      sb.append("/**\n");
      sb.append(" * Because this class has a default constructor, it can\n");
      sb.append(" * be used as a binder template. In other words, it can be used in other\n");
      sb.append(" * *.ui.xml files as follows:\n");
      sb.append(" * <ui:UiBinder xmlns:ui=\"urn:ui:com.google.gwt.uibinder\"\n");
      sb.append("  *   xmlns:g=\"urn:import:**user's package**\">\n");
      sb.append(" *  <g:**UserClassName**>Hello!</g:**UserClassName>\n");
      sb.append(" * </ui:UiBinder>\n");
      sb.append(" * Note that depending on the widget that is used, it may be necessary to\n");
      sb.append(" * implement HasHTML instead of HasText.\n");
      sb.append(" */\n");
    }
    sb.append("public ");
    sb.append(ownerClass.getElementName());
    sb.append("(");

    sb.append(") {\n");
    sb.append("\tinitWidget(uiBinder.createAndBindUi(this));\n");
    sb.append("}");

    return sb.toString();
  }

  private static String createEventHandler(ImportsManager imports) {
    StringBuilder sb = new StringBuilder();

    sb.append("@");
    sb.append(imports.addImport(UiBinderConstants.UI_HANDLER_TYPE_NAME));
    sb.append("(\"button\")\n");
    sb.append("void onClick(");
    sb.append(imports.addImport("com.google.gwt.event.dom.client.ClickEvent"));
    sb.append(" e) {\n");
    sb.append(imports.addImport("com.google.gwt.user.client.Window"));
    sb.append(".alert(\"Hello!\");\n");
    sb.append("}");

    return sb.toString();
  }

  @Override
  public void createOwnerClassMembers(IType ownerClass, ImportsManager imports,
      boolean addComments, boolean addSampleContent, IProgressMonitor monitor)
      throws JavaModelException {
    String uiBinderDecl = createUiBinderSubtype(ownerClass,
        "com.google.gwt.user.client.ui.Widget", imports);
    ownerClass.createType(uiBinderDecl, null, false, monitor);

    String uiBinderField = createUiBinderStaticField(ownerClass, imports);
    ownerClass.createField(uiBinderField, null, false, monitor);

    String defaultCtorSrc = createDefaultCtor(ownerClass, addComments);
    ownerClass.createMethod(defaultCtorSrc, null, false, monitor);

    if (addSampleContent) {
      String ctorSrc = createCtor(ownerClass, addSampleContent, addComments);
      IMethod ctor = ownerClass.createMethod(ctorSrc, null, false, monitor);

      String uiField = createUiField("com.google.gwt.user.client.ui.Button",
          "button", imports);
      ownerClass.createField(uiField, ctor, false, monitor);

      String eventHandler = createEventHandler(imports);
      ownerClass.createMethod(eventHandler, null, false, monitor);

      String getterSrc = createGetter(ownerClass);
      ownerClass.createMethod(getterSrc, null, false, monitor);

      String setterSrc = createSetter(ownerClass, addComments);
      ownerClass.createMethod(setterSrc, null, false, monitor);
    }
  }

  @Override
  public String getOwnerSuperclass() {
    return "com.google.gwt.user.client.ui.Composite";
  }

  @Override
  public List<String> getOwnerSuperinterfaces(boolean addSampleContent) {
    if (addSampleContent) {
      return Arrays.asList("com.google.gwt.user.client.ui.HasText");
    }
    return Collections.emptyList();
  }

  @Override
  protected String createMarkupElement(boolean addComments,
      boolean addSampleContent) throws CoreException {
    return createElement(UiBinderConstants.GWT_USER_LIBRARY_UI_NAMESPACE_PREFIX
        + ":HTMLPanel", addComments, "uixml-markup-comment-widgets.template",
        addSampleContent, "uixml-markup-sample-content-widgets.template");
  }

  @Override
  protected InputStream createUiXmlSource(boolean addComments,
      boolean addSampleContent) throws CoreException {
    Map<String, String> replacements = new HashMap<String, String>();
    replacements.put("@UiBinderXmlNs@",
        UiBinderConstants.UI_BINDER_XML_NAMESPACE);
    replacements.put("@GwtWidgetsLibNsPrefix@",
        UiBinderConstants.GWT_USER_LIBRARY_UI_NAMESPACE_PREFIX);
    replacements.put("@GwtWidgetsLibNs@",
        UiBinderConstants.GWT_USER_LIBRARY_UI_PACKAGE_NAME);
    replacements.put("@StyleElement@", createStyleElement(addComments,
        addSampleContent));
    replacements.put("@MarkupElement@", createMarkupElement(addComments,
        addSampleContent));
    return ResourceUtils.getResourceAsStreamAndFilterContents(getClass(),
        replacements, "uixml-widgets.template");
  }

  private String createGetter(IType ownerClass) {
    StringBuilder sb = new StringBuilder();

    sb.append("public void setText(String text) {\n");
    sb.append("\tbutton.setText(text);\n");
    sb.append("}");

    return sb.toString();
  }

  private String createSetter(IType ownerClass, boolean addComments) {
    StringBuilder sb = new StringBuilder();

    if (addComments) {
      sb.append("/**\n");
      sb.append(" * Gets invoked when the default constructor is called\n");
      sb.append(" * and a string is provided in the ui.xml file.\n");
      sb.append(" */\n");
    }

    sb.append("public String getText() {\n");
    sb.append("\treturn button.getText();\n");
    sb.append("}");

    return sb.toString();
  }
}
