package com.gwtplugins.jettylauncher.jee8;

import java.io.File;

import org.eclipse.jetty.ee8.servlet.DefaultServlet;
import org.eclipse.jetty.ee8.servlet.ServletHolder;
import org.eclipse.jetty.ee8.webapp.WebAppContext;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.Handler.Sequence;
import org.eclipse.jetty.server.handler.InetAccessHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.gwtplugins.jettylauncher.AbstractJetty12ServletContainer;

/**
 * Implementation of GWTs ServletContainer class, that starts a Jetty 12 from
 * JETTY_HOME
 */
class Jetty12ServletContainer extends AbstractJetty12ServletContainer 
{
	Jetty12ServletContainer(int port, File appRootDir) 
	{
		super(port, appRootDir);
	}

	@Override
	protected Handler.Sequence createHandler() 
	{
		WebAppContext webAppHandler = new WebAppContext(appRootDir.getAbsolutePath().replace('\\', '/'), "/") {
			@Override
			public boolean isServerClass(Class<?> clazz) {
				return false;
			}
		};
		// DefaultServlet to control Cache
		DefaultServlet defaultServlet = new DefaultServlet();
		ServletHolder holder = new ServletHolder(defaultServlet);
		holder.setInitParameter("useFileMappedBuffer", "false");
		holder.setInitParameter("cacheControl", "max-age=600, public"); // 10 Minutes Cache
		webAppHandler.addServlet(holder, "/");

		Handler.Sequence shandler = new Handler.Sequence();
		shandler.addHandler(webAppHandler);
		return shandler;
	}
	
}