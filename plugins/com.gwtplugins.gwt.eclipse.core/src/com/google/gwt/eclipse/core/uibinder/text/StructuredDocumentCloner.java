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
package com.google.gwt.eclipse.core.uibinder.text;

import com.google.gdt.eclipse.core.formatter.IDocumentCloner;
import com.google.gwt.eclipse.core.GWTPluginLog;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.exceptions.ResourceInUse;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Clones an {@IStructuredDocument}, including its backing
 * model.
 */
@SuppressWarnings("restriction")
public class StructuredDocumentCloner implements IDocumentCloner {

  private static final AtomicInteger nextId = new AtomicInteger(0);

  private final IDocumentPartitionerFactory documentPartitionerFactory;

  /**
   * Tracks the (cloned) models backing the document clones we've created, so we
   * can release them later.
   */
  private final Map<IDocument, IStructuredModel> modelClones = new WeakHashMap<IDocument, IStructuredModel>();

  private final String partitioning;

  public StructuredDocumentCloner(String partitioning,
      IDocumentPartitionerFactory documentPartitionerFactory) {
    this.partitioning = partitioning;
    this.documentPartitionerFactory = documentPartitionerFactory;
  }

  public IStructuredDocument clone(IDocument original) {
    IModelManager modelManager = StructuredModelManager.getModelManager();
    String modelId = getModelId(original);
    if (modelId == null) {
      GWTPluginLog.logError("Could not get a model ID for the document to be formatted.");
      return null;
    }

    /*
     * The XML formatter requires a managed model (discovered by it crashing
     * when given an unmanaged model.) Unfortunately, we cannot create a managed
     * in-memory (i.e. non-file backed) model via IModelManager API (there is
     * one method that may work, but it is deprecated.) Instead, we copy the
     * existing model into a temp model with ID "temp".
     */
    try {
      IStructuredModel modelClone = modelManager.copyModelForEdit(modelId,
          "structuredDocumentClonerModel" + nextId.getAndIncrement());
      modelClone.setBaseLocation(getModelBaseLocation(original));

      IStructuredDocument documentClone = modelClone.getStructuredDocument();
      documentClone.set(original.get());

      // Create and connect the partitioner to the document
      IDocumentPartitioner tempPartitioner = documentPartitionerFactory.createDocumentPartitioner();
      ((IDocumentExtension3) documentClone).setDocumentPartitioner(
          partitioning, tempPartitioner);
      tempPartitioner.connect(documentClone);

      // Save the cloned model so we can release it later
      modelClones.put(documentClone, modelClone);

      return documentClone;
    } catch (ResourceInUse e1) {
      GWTPluginLog.logError(e1,
          "Could not copy the original model to be formatted.");
      return null;
    }
  }

  public void release(IDocument clone) {
    IStructuredModel model = modelClones.get(clone);
    if (model != null) {
      model.releaseFromEdit();
    }
  }

  private String getModelBaseLocation(IDocument document) {
    IModelManager modelManager = StructuredModelManager.getModelManager();
    IStructuredModel model = null;
    try {
      model = modelManager.getExistingModelForRead(document);
      return model.getBaseLocation();
    } finally {
      if (model != null) {
        model.releaseFromRead();
      }
    }
  }

  private String getModelId(IDocument document) {
    IModelManager modelManager = StructuredModelManager.getModelManager();
    IStructuredModel model = null;
    try {
      model = modelManager.getExistingModelForRead(document);
      return model.getId();
    } finally {
      if (model != null) {
        model.releaseFromRead();
      }
    }
  }

}
