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
package com.google.gwt.eclipse.core.uibinder.validators;

import com.google.gwt.eclipse.core.uibinder.UiBinderException;
import com.google.gwt.eclipse.core.uibinder.model.reference.UiBinderXmlParser;
import com.google.gwt.eclipse.core.uibinder.problems.ReporterMessagePlacementStrategy;

import org.eclipse.jface.text.IDocument;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.validation.internal.core.ValidationException;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.eclipse.wst.validation.internal.provisional.core.IValidationContext;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.ui.internal.validation.MarkupValidator;

import java.io.FileNotFoundException;

/*
 * Subtypes the default XML validator.
 */
/**
 * An as-you-type validator for the .ui.xml content type.
 */
@SuppressWarnings("restriction")
public class UiXmlEditorValidator extends MarkupValidator {

  private IDocument document;

  /**
   * Keeps track of the connect/disconnects.
   * <p>
   * Super's {@link #validate(IValidationContext, IReporter)} method calls
   * connect/disconnect, which would cause {@link #document} to be set to null
   * when our {@link #validate(IValidationContext, IReporter)} logic runs. To
   * prevent this, we keep track of the number of connects and disconnects, only
   * clearing {@link #document} when this number is zero.
   */
  private int documentRefCount;

  @Override
  public void connect(IDocument document) {
    super.connect(document);

    this.document = document;
    documentRefCount++;
  }

  @Override
  public void disconnect(IDocument document) {
    super.disconnect(document);

    if (documentRefCount > 0) {
      documentRefCount--;
    }

    if (documentRefCount == 0) {
      this.document = null;
    }
  }

  @Override
  public void validate(IValidationContext helper, IReporter reporter)
      throws ValidationException {
    super.validate(helper, reporter);

    if (document == null) {
      return;
    }

    IDOMModel xmlModel = (IDOMModel) StructuredModelManager.getModelManager().getExistingModelForRead(
        document);
    if (xmlModel != null) {
      try {
        ReporterMessagePlacementStrategy validationResultPlacementStrategy = new ReporterMessagePlacementStrategy(
            this, reporter);
        // Both super and UiBinderXmlParser end up removing all messages. We
        // disallow ours from clearing, since that would remove all messages
        // added by super.
        validationResultPlacementStrategy.setClearAllowed(false);

        // We do not want to stash these references into the master reference
        // manager since this gets called as-user-types and the file is not
        // necessarily saved.
        UiBinderXmlParser.newInstance(xmlModel, null,
            validationResultPlacementStrategy).parse();
      } catch (FileNotFoundException e) {
        // Ignore since this is as-you-type, the resource changed parser will
        // log errors
      } catch (UiBinderException e) {
        // Ignore since this is as-you-type, the resource changed parser will
        // log errors
      } finally {
        xmlModel.releaseFromRead();
      }
    }
  }

}
