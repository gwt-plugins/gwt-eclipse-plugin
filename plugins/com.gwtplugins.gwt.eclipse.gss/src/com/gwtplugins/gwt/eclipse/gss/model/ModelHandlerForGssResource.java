package com.gwtplugins.gwt.eclipse.gss.model;

import org.eclipse.wst.css.core.internal.modelhandler.ModelHandlerForCSS;
import org.eclipse.wst.sse.core.internal.document.IDocumentLoader;
import org.eclipse.wst.sse.core.internal.provisional.IModelLoader;

/**
 * Entry point for generating structured models and documents from
 * CssResource-aware CSS files. This is the default handler for these files, set
 * via the "org.eclipse.wst.sse.core.modelHandler" extension point.
 */
@SuppressWarnings("restriction")
public class ModelHandlerForGssResource extends ModelHandlerForCSS {

  /*
   * Derived from ModelHandlerForCSS's implementation.
   */
  @Override
  public IDocumentLoader getDocumentLoader() {
    return new GssResourceAwareDocumentLoader();
  }

  /*
   * Derived from ModelHandlerForCSS's implementation.
   */
  @Override
  public IModelLoader getModelLoader() {
    return new GssResourceAwareModelLoader();
  }
}
