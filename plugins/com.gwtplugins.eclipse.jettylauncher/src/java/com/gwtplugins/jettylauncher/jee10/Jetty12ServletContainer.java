package com.gwtplugins.jettylauncher.jee10;

import java.io.File;

import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.server.Handler;
import com.gwtplugins.jettylauncher.AbstractJetty12ServletContainer;

class Jetty12ServletContainer extends AbstractJetty12ServletContainer
{
    Jetty12ServletContainer(int port, File appRootDir)
    {
	    super(port, appRootDir);
    }
   
    @Override
    protected Handler.Sequence createHandler() 
    {
		WebAppContext webAppHandler = new WebAppContext(appRootDir.getAbsolutePath().replace('\\', '/'), "/");
//      webAppHandler.setDefaultsDescriptor(JavaxWebAppStarter.class.getResource("/webxml/webdefault-ee10.xml").toString());

		// DefaultServlet hinzufügen um Cache für statische Dateien zu kontrollieren
		DefaultServlet defaultServlet = new DefaultServlet();
		ServletHolder holder = new ServletHolder(defaultServlet);
		holder.setInitParameter("useFileMappedBuffer", "false");
		holder.setInitParameter("cacheControl", "max-age=600, public"); // 10 Minuten Cachen
		webAppHandler.addServlet(holder, "/");
		Handler.Sequence shandler = new Handler.Sequence();
		shandler.addHandler(webAppHandler);
		return shandler;
    }
}