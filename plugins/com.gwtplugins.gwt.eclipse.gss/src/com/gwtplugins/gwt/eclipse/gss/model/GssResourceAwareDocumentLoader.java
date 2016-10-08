package com.gwtplugins.gwt.eclipse.gss.model;

import org.eclipse.wst.css.core.internal.encoding.CSSDocumentLoader;
import org.eclipse.wst.sse.core.internal.document.IDocumentLoader;
import org.eclipse.wst.sse.core.internal.ltk.parser.RegionParser;

/**
 * Constructs a structured model for CSS, but is CSS Resource-aware.
 * <p>
 * This is required to have WST use the CSS Resource-aware source parser
 * {@link GssResourceAwareSourceParser}.
 */
@SuppressWarnings("restriction")
public class GssResourceAwareDocumentLoader extends CSSDocumentLoader {

  /*
   * Derived from CSSDocumentLoader's implementation.
   */
  @Override
  public RegionParser getParser() {
    return new GssResourceAwareSourceParser();
  }

  /*
   * Derived from CSSDocumentLoader's implementation.
   */
  @Override
  public IDocumentLoader newInstance() {
    return new GssResourceAwareDocumentLoader();
  }

}
