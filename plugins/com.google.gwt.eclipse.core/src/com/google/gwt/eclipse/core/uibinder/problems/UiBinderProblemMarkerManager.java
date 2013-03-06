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

import com.google.gdt.eclipse.core.markers.GdtProblemSeverities;
import com.google.gdt.eclipse.core.markers.GdtProblemSeverity;
import com.google.gwt.eclipse.core.GWTPlugin;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import java.text.MessageFormat;

/**
 * Creates/sets UiBinder problem markers on the given resource. Before setting a
 * marker, it ensures an existing similar marker does not exist.
 */
public class UiBinderProblemMarkerManager {

  public static final String MARKER_ID = GWTPlugin.PLUGIN_ID
      + ".uiBinderProblemMarker";

  private final IDocument document;
  private final IResource resource;
  private final IValidationResultPlacementStrategy<?> strategy;

  public UiBinderProblemMarkerManager(IResource resource, IDocument document,
      IValidationResultPlacementStrategy<?> strategy) {
    this.resource = resource;
    this.document = document;
    this.strategy = strategy;
  }

  public void clear() {
    strategy.clearValidationResults(resource);
  }

  public void setCssFileNotFoundError(IRegion position, String cssFileName) {
    placeValidationResult(UiBinderTemplateProblemType.CSS_FILE_NOT_FOUND,
        position, cssFileName);
  }

  public void setCssSelectorFragmentUndefinedError(IRegion position,
      String fieldValue) {
    placeValidationResult(
        UiBinderTemplateProblemType.CSS_SELECTOR_FRAGMENT_UNDEFINED, position,
        fieldValue);
  }

  public void setCssSelectorFragmentUnspecifiedError(IRegion fieldRefRegion) {
    placeValidationResult(
        UiBinderTemplateProblemType.CSS_SELECTOR_FRAGMENT_UNSPECIFIED,
        fieldRefRegion);
  }

  public void setDuplicateCssSelectorError(IRegion position, String errorMessage) {
    placeValidationResult(UiBinderTemplateProblemType.DUPLICATE_CSS_SELECTOR,
        position, errorMessage);
  }

  public void setDuplicateFieldError(IRegion position, String fragment) {
    placeValidationResult(UiBinderTemplateProblemType.DUPLICATE_FIELD,
        position, fragment);
  }

  public void setFieldNotStaticError(IRegion position, String fieldValue) {
    placeValidationResult(UiBinderTemplateProblemType.FIELD_NOT_STATIC,
        position, fieldValue);
  }

  public void setFieldNotVisibleError(IRegion position, String fieldValue) {
    placeValidationResult(UiBinderTemplateProblemType.FIELD_NOT_VISIBLE,
        position, fieldValue);
  }
  
  public void setFieldUndefinedError(IRegion position, String fieldValue) {
    placeValidationResult(UiBinderTemplateProblemType.FIELD_UNDEFINED,
      position, fieldValue);
  }
  
  public void setFirstFragmentUndefinedError(IRegion position, String fieldValue) {
    placeValidationResult(UiBinderTemplateProblemType.FIRST_FRAGMENT_UNDEFINED,
        position, fieldValue);
  }
  
  public void setImportMissingFieldAttr(IRegion position, String widget) {
    placeValidationResult(UiBinderTemplateProblemType.IMPORT_MISSING_FIELD_ATTR,
      position, widget);
  }
  
  public void setMethodFragmentUndefinedError(IRegion position,
      String fieldValue) {
    placeValidationResult(
        UiBinderTemplateProblemType.METHOD_FRAGMENT_UNDEFINED, position,
        fieldValue);
  }

  public void setNamespacePrefixUndefinedError(IRegion position, String prefix) {
    placeValidationResult(
        UiBinderTemplateProblemType.NAMESPACE_PREFIX_UNDEFINED, position,
        prefix);
  }

  public void setNotCssResourceSubtypeError(IRegion position, String fqType) {
    placeValidationResult(UiBinderTemplateProblemType.NOT_CSS_RESOURCE_SUBTYPE,
        position, fqType);
  }

  public void setPackageUndefinedError(IRegion position, String packageName) {
    placeValidationResult(UiBinderTemplateProblemType.PACKAGE_UNDEFINED,
        position, packageName);
  }

  public void setPrimitiveFragmentWithLeftoverFragmentsError(IRegion position,
      String primitiveFragment) {
    placeValidationResult(
        UiBinderTemplateProblemType.PRIMITIVE_FRAGMENT_WITH_LEFTOVER_FRAGMENTS,
        position, primitiveFragment);
  }

  public void setResourceNotFoundError(IRegion position, String imagePath) {
    placeValidationResult(UiBinderTemplateProblemType.RESOURCE_NOT_FOUND,
        position, imagePath);
  }

  public void setTypeNotVisibleError(IRegion position, String fieldValue) {
    placeValidationResult(UiBinderTemplateProblemType.TYPE_NOT_ACCESSIBLE,
        position, fieldValue);
  }

  public void setTypeUndefinedError(IRegion position, String type) {
    placeValidationResult(UiBinderTemplateProblemType.TYPE_UNDEFINED, position,
        type);
  }
  
  public void setWidgetUndefinedError(IRegion position, String widget) {
    placeValidationResult(UiBinderTemplateProblemType.WIDGET_UNDEFINED,
        position, widget);
  }

  private void placeValidationResult(UiBinderTemplateProblemType problemType,
      IRegion position, String... messageArgs) {
    // Look up the problem severity in the workspace settings
    GdtProblemSeverity severity = GdtProblemSeverities.getInstance().getSeverity(
        problemType);

    if (severity != GdtProblemSeverity.IGNORE) {
      String msg = MessageFormat.format(problemType.getMessage(),
          (Object[]) messageArgs);
      strategy.placeValidationResult(resource, document, position, msg,
          severity.getMarkerSeverity());
    }
  }

}
