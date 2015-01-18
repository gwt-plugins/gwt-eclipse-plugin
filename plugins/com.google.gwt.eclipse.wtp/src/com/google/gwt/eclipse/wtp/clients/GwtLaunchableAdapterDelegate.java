package com.google.gwt.eclipse.wtp.clients;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IModuleArtifact;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IURLProvider;
import org.eclipse.wst.server.core.model.LaunchableAdapterDelegate;
import org.eclipse.wst.server.core.util.HttpLaunchable;

import java.net.URL;

/**
 * {@link LaunchableAdapterDelegate} for opening Browser upon server launch.
 */
public final class GwtLaunchableAdapterDelegate extends LaunchableAdapterDelegate {

  @Override
  public Object getLaunchable(IServer server, IModuleArtifact moduleArtifact) throws CoreException {



    URL url =
        ((IURLProvider) server.loadAdapter(IURLProvider.class, null))
            .getModuleRootURL(moduleArtifact.getModule());
    return new HttpLaunchable(url);
  }
}
