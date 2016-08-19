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
package com.google.gwt.eclipse.core.uibinder.problems;

import com.google.gdt.eclipse.core.markers.GdtProblemCategory;
import com.google.gdt.eclipse.core.markers.GdtProblemSeverity;
import com.google.gdt.eclipse.core.markers.IGdtProblemType;

/**
 * Defines UiBinder problems that can occur in UiBinder template files (ui.xml).
 */
public enum UiBinderTemplateProblemType implements IGdtProblemType {

  /**
   * The message when a CSS file is not found.
   */
  CSS_FILE_NOT_FOUND("Missing CSS file", "CSS file {0} is missing",
      GdtProblemSeverity.ERROR),

  /**
   * The message when a field reference's fragment to a CSS selector is
   * undefined.
   */
  CSS_SELECTOR_FRAGMENT_UNDEFINED("Undefined CSS selector",
      "CSS selector {0} is undefined", GdtProblemSeverity.ERROR),

  /**
   * The message when a field reference to CSS does not have any selector
   * specified. For example, "{style}".
   */
  CSS_SELECTOR_FRAGMENT_UNSPECIFIED("Incomplete CSS field reference",
      "Incomplete field reference, specify a CSS selector",
      GdtProblemSeverity.ERROR),

  /**
   * The message when a CSS selector is defined more than once.
   */
  DUPLICATE_CSS_SELECTOR("Duplicate CSS selector", "{0}",
      GdtProblemSeverity.ERROR),

  /**
   * The message when a ui:field is declared more than once.
   */
  DUPLICATE_FIELD("Duplicate ui:field",
      "Field {0} cannot appear multiple times in one template",
      GdtProblemSeverity.ERROR),

  FIELD_NOT_STATIC("Field not static", "Field {0} is not static",
      GdtProblemSeverity.ERROR),

  FIELD_NOT_VISIBLE("Field not accessible", "Field {0} is not accessible",
      GdtProblemSeverity.ERROR),

  FIELD_UNDEFINED("Field undefined", "Field {0} is undefined",
      GdtProblemSeverity.ERROR),

  /**
   * The message when a field reference's first fragment is undefined.
   */
  FIRST_FRAGMENT_UNDEFINED("Undefined field reference first fragment",
      "Field reference to {0} is undefined", GdtProblemSeverity.ERROR),

  IMPORT_MISSING_FIELD_ATTR("ui:import tag missing field attribute",
      "ui:import tag is missing the field attribute", GdtProblemSeverity.ERROR),
  /**
   * The message when a field reference's fragment to a Java method is
   * undefined.
   */
  METHOD_FRAGMENT_UNDEFINED("Undefined field reference method fragment",
      "Method {0} is undefined", GdtProblemSeverity.ERROR),

  /**
   * The message when an element's prefix is undefined.
   */
  NAMESPACE_PREFIX_UNDEFINED("Undefined namespace prefix",
      "Namespace prefix {0} is undefined", GdtProblemSeverity.ERROR),

  /**
   * The message for <ui:style type='...'> attributes that are not subtypes of
   * CssResource.
   */
  NOT_CSS_RESOURCE_SUBTYPE("ui:style type not a CssResource subclass",
      "Type {0} must be a subtype of CssResource", GdtProblemSeverity.ERROR),

  /**
   * The message when a UiBinder attribute using the URN import scheme refers to
   * an undefined package.
   */
  PACKAGE_UNDEFINED("Unresolved package name in import",
      "Package {0} cannot be resolved", GdtProblemSeverity.ERROR),

  /**
   * The message when a field reference refers to a non-terminal primitive, for
   * example {something.anInt.somethingElse}, where anInt returns an int.
   */
  PRIMITIVE_FRAGMENT_WITH_LEFTOVER_FRAGMENTS(

      "Field reference with non-terminal primitive",
      "Method {0} returns a primitive, but there are remaining fragments in the field reference",
      GdtProblemSeverity.ERROR),

  /**
   * The message when a resource (image, data) is not found.
   */
  RESOURCE_NOT_FOUND("Missing resource file", "Resource {0} is missing",
      GdtProblemSeverity.ERROR),

  /**
   * Message when a type cannot be accessed due to visibility
   */
  TYPE_NOT_ACCESSIBLE("Type not accessible", "Type {0} is not accessible",
      GdtProblemSeverity.ERROR),

  /**
   * The message when a ui:type attribute refers to an undefined type.
   */
  TYPE_UNDEFINED("Unresolved ui:type", "Type {0} cannot be resolved",
      GdtProblemSeverity.ERROR),

  /**
   * The message when an element refers to an undefined widget.
   */
  WIDGET_UNDEFINED("Unresolved widget", "Widget {0} cannot be resolved",
      GdtProblemSeverity.ERROR);

  private final GdtProblemSeverity defaultSeverity;

  private final String description;

  private final String message;

  private UiBinderTemplateProblemType(String description, String message,
      GdtProblemSeverity defaultSeverity) {
    this.description = description;
    this.message = message;
    this.defaultSeverity = defaultSeverity;
  }

  public GdtProblemCategory getCategory() {
    return GdtProblemCategory.UI_BINDER;
  }

  public GdtProblemSeverity getDefaultSeverity() {
    return defaultSeverity;
  }

  public String getDescription() {
    return description;
  }

  public String getMessage() {
    return message;
  }

  public int getProblemId() {
    return UIBINDER_TEMPLATE_OFFSET + this.ordinal() + 1;
  }

  @Override
  public String toString() {
    return description;
  }

}