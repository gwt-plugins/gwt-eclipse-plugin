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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates code for UiBinder interfaces based on plain HTML.
 */
public class HtmlBasedUiBinderResourceCreator extends UiBinderResourceCreator {

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
    sb.append("\tsetElement(uiBinder.createAndBindUi(this));\n");

    if (addSampleContent) {
      if (addComments) {
        sb.append('\n');
        sb.append("\t// Can access @UiField after calling createAndBindUi\n");
      }
      sb.append("\tnameSpan.setInnerText(firstName);\n");
    }

    sb.append("}");

    return sb.toString();
  }

  @Override
  public void createOwnerClassMembers(IType ownerClass, ImportsManager imports,
      boolean addComments, boolean addSampleContent, IProgressMonitor monitor)
      throws JavaModelException {
    String uiBinderDecl = createUiBinderSubtype(ownerClass,
        "com.google.gwt.dom.client.Element", imports);
    ownerClass.createType(uiBinderDecl, null, false, monitor);

    String uiBinderField = createUiBinderStaticField(ownerClass, imports);
    ownerClass.createField(uiBinderField, null, false, monitor);

    String ctorSrc = createCtor(ownerClass, addSampleContent, addComments);
    IMethod ctor = ownerClass.createMethod(ctorSrc, null, false, monitor);

    if (addSampleContent) {
      String uiField = createUiField("com.google.gwt.dom.client.SpanElement",
          "nameSpan", imports);
      ownerClass.createField(uiField, ctor, false, monitor);
    }
  }

  @Override
  public String getOwnerSuperclass() {
    return "com.google.gwt.user.client.ui.UIObject";
  }

  @Override
  public List<String> getOwnerSuperinterfaces(boolean addSampleContent) {
    return Collections.emptyList();
  }

  @Override
  protected String createMarkupElement(boolean addComments,
      boolean addSampleContent) throws CoreException {
    return createElement("div", addComments,
        "uixml-markup-comment-html.template", addSampleContent,
        "uixml-markup-sample-content-html.template");
  }

  @Override
  protected InputStream createUiXmlSource(boolean addComments,
      boolean addSampleContent) throws CoreException {
    Map<String, String> replacements = new HashMap<String, String>();
    replacements.put("@UiBinderXmlNs@",
        UiBinderConstants.UI_BINDER_XML_NAMESPACE);
    replacements.put("@StyleElement@", createStyleElement(addComments,
        addSampleContent));
    replacements.put("@MarkupElement@", createMarkupElement(addComments,
        addSampleContent));
    return ResourceUtils.getResourceAsStreamAndFilterContents(getClass(),
        replacements, "uixml-html.template");
  }

}
