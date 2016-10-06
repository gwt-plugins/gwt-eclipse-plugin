package com.google.gwt.eclipse.wtp.clients;

import org.eclipse.wst.server.core.internal.IStartup;

import com.google.gwt.eclipse.wtp.GwtWtpPlugin;
import com.google.gwt.eclipse.wtp.utils.GwtFacetUtils;

@SuppressWarnings("restriction")
public final class GwtRuntimeConfigurator implements IStartup {

  @Override
  public void startup() {
    GwtWtpPlugin.logMessage("GWT Facet startup");
    
    // remove old gwt facet
    GwtFacetUtils.removePreviousGwtFacet();
  }

}
