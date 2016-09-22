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
package com.google.gwt.eclipse.core.uibinder;

/**
 * Various constants for UiBinder.
 */
public final class UiBinderConstants {

  public static final boolean UI_BINDER_ENABLED = true;

  public static final String GWT_USER_LIBRARY_UI_NAMESPACE_PREFIX = "g";

  public static final String GWT_USER_LIBRARY_UI_PACKAGE_NAME = "com.google.gwt.user.client.ui";

  public static final String GWT_USER_LIBRARY_WIDGET_CLASS_NAME = "Widget";

  // Disabled due to: 
  // http://code.google.com/p/google-plugin-for-eclipse/issues/detail?id=11
  // public static final TypeHierarchyCache TYPE_HIERARCHY_CACHE = new
  // TypeHierarchyCache();

  /**
   * The non-prefixed tag name for <ui:data> that allows for implicitly creating
   * an DataResource.
   */
  public static final String UI_BINDER_DATA_ELEMENT_NAME = "data";

  /**
   * The fully qualified name of the DataResource interface.
   */
  public static final String UI_BINDER_DATA_RESOURCE_NAME = "com.google.gwt.resources.client.DataResource";

  /**
   * The non-prefixed tag name for the "UiBinder" root element in a UiBinder XML
   * template file.
   */
  public static final String UI_BINDER_ELEMENT_NAME = "UiBinder";

  /**
   * The non-prefixed attribute name for the attribute whose value defines the
   * owner class field where the element will be injected.
   */
  public static final String UI_BINDER_FIELD_ATTRIBUTE_NAME = "field";

  /**
   * The non-prefixed tag name for <ui:image> that allows for implicitly
   * creating an ImageResource.
   */
  public static final String UI_BINDER_IMAGE_ELEMENT_NAME = "image";

  /**
   * The fully qualified name of the ImageResource interface.
   */
  public static final String UI_BINDER_IMAGE_RESOURCE_NAME = "com.google.gwt.resources.client.ImageResource";

  /**
   * The tag name for <ui:import>
   */
  public static final String UI_BINDER_IMPORT_ELEMENT_NAME = "import";

  /**
   * The fully qualified name of the UiBinder interface.
   */
  public static final String UI_BINDER_INTERFACE_NAME = "com.google.gwt.uibinder.client.UiBinder";

  /**
   * The non-prefixed attribute name for the ui:ph attribute.
   */
  public static final String UI_BINDER_PH_ATTRIBUTE_NAME = "ph";

  /**
   * The non-prefixed attribute name for the attribute whose value defines the
   * file (and its optional path) for external resources.
   */
  public static final String UI_BINDER_SRC_ATTRIBUTE_NAME = "src";

  /**
   * The EL expression first fragment value for an implicit "field" attribute on
   * a <ui:style> element.
   */
  public static final String UI_BINDER_STYLE_ELEMENT_IMPLICIT_FIELD_VALUE = "style";

  /**
   * The non-prefixed tag name for the parent of inlined CSS blocks.
   */
  public static final String UI_BINDER_STYLE_ELEMENT_NAME = "style";

  /**
   * The non-prefixed attribute name (for the "with" element) whose value is a
   * fully qualified java type name.
   */
  public static final String UI_BINDER_TYPE_ATTRIBUTE_NAME = "type";

  public static final String UI_BINDER_TYPE_NAME = "com.google.gwt.uibinder.client.UiBinder";

  /**
   * The non-prefixed tag name for the "with" construct that allows for
   * referencing a java type.
   */
  public static final String UI_BINDER_WITH_ELEMENT_NAME = "with";

  /**
   * The content type ID for UiBinder XML files. This ID is assigned for content
   * that passes the {@link UiBinderXmlContentDescriber}.
   */
  public static final String UI_BINDER_XML_CONTENT_TYPE_ID = "com.google.gwt.eclipse.core.uibinder.content.xml";

  public static final String UI_BINDER_XML_EXTENSION = ".ui.xml";

  /**
   * The ID for the
   * {@link com.google.gwt.eclipse.core.uibinder.sse.model.ModelHandlerForUiBinderXml}
   * .
   */
  public static final String UI_BINDER_XML_MODEL_HANDLER_ID = "com.google.gwt.eclipse.core.uibinder.content.modelhandler";

  /**
   * The XML namespace used by UiBinder.
   */
  public static final String UI_BINDER_XML_NAMESPACE = "urn:ui:com.google.gwt.uibinder";

  /**
   * The fully qualified name of the UiField annotation which allows an owner
   * class's field to be injected with an instance declared in the UiBinder XML
   * file.
   */
  public static final String UI_FIELD_ANNOTATION_NAME = "com.google.gwt.uibinder.client.UiField";

  public static final String UI_FIELD_TYPE_NAME = "com.google.gwt.uibinder.client.UiField";

  public static final String UI_HANDLER_TYPE_NAME = "com.google.gwt.uibinder.client.UiHandler";

  /**
   * The fully qualified name of the UiTemplate annotation to define an
   * alternate path to the UiBinder XML template file.
   */
  public static final String UI_TEMPLATE_ANNOTATION_NAME = "com.google.gwt.uibinder.client.UiTemplate";

  /**
   * The beginning of a namespace URI (intentionally avoiding the word "prefix"
   * here) referring to UiBinder's import scheme.
   */
  public static final String URN_IMPORT_NAMESPACE_BEGINNING = "urn:import:";

  public static final String XMLNS_NAMESPACE = "http://www.w3.org/2000/xmlns/";

  public static final String XMLNS_PREFIX = "xmlns";

  private UiBinderConstants() {
  }
}
