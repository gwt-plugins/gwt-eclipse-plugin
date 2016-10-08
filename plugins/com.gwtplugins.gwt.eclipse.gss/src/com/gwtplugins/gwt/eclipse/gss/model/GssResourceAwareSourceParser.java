package com.gwtplugins.gwt.eclipse.gss.model;

import org.eclipse.wst.css.core.internal.parser.CSSSourceParser;
import org.eclipse.wst.css.core.internal.parser.ICSSTokenizer;
import org.eclipse.wst.sse.core.internal.ltk.parser.RegionParser;

/**
 * Constructs a list of structured document regions from the tokenizer. This
 * version is CSS Resource-aware, meaning it can handle the custom CSS Resource
 * at-rules.
 * <p>
 * This is required for the {@link GssResourceAwareTokenizer} to be used.
 */
@SuppressWarnings("restriction")
public class GssResourceAwareSourceParser extends CSSSourceParser {
  private ICSSTokenizer tokenizer;

  /*
   * Derived from CSSSourceParser's implementation.
   */
  @Override
  public ICSSTokenizer getTokenizer() {
    if (tokenizer == null) {
      tokenizer = new GssResourceAwareTokenizer();
    }

    return tokenizer;
  }

  /*
   * Derived from CSSSourceParser's implementation.
   */
  @Override
  public RegionParser newInstance() {
    return new GssResourceAwareSourceParser();
  }

}
