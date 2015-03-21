package com.google.gwt.eclipse.wtp.clients;

import org.eclipse.wst.server.core.internal.IStartup;

@SuppressWarnings("restriction")
public final class GwtRuntimeConfigurator implements IStartup {

  @Override
  public void startup() {
    System.out.println("GWT Facet startup");
  }

}
