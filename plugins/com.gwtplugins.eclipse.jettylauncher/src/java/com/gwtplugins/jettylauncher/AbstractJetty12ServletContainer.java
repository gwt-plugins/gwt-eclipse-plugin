package com.gwtplugins.jettylauncher;

import java.io.File;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.InetAccessHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.google.gwt.core.ext.ServletContainer;
import com.google.gwt.core.ext.UnableToCompleteException;

public abstract class AbstractJetty12ServletContainer extends ServletContainer {

	protected int port;
	protected File appRootDir;

	public AbstractJetty12ServletContainer(int port, File appRootDir) 
	{
		super();
		this.port = port;
		this.appRootDir = appRootDir;
		try 
		{
			startServer(port);
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}

	/**
	 * Create a Thread Pool for the Jetty Server
	 * @return
	 */
	protected QueuedThreadPool createThreadPool()
	{
		// Eigenen ThreadPool um Anzahl der Threads kontrollieren zu können
		QueuedThreadPool pool = new QueuedThreadPool(50, 20) 
		{
			@Override
			public Thread newThread(Runnable runnable) {
				// Threads markieren
				Thread t = super.newThread(runnable);
				t.setName("Jetty - " + t.getName());
				return t;
			}
		};
		return pool;
	}
	
	/**
	 * Starts the Jetty Server on the given Port.
	 * @param port
	 * @throws Exception
	 */
	public void startServer(int port) throws Exception 
	{
    	QueuedThreadPool pool = createThreadPool();
		Server server = new Server(pool);
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(port);
		connector.setIdleTimeout(30000);
		server.addConnector(connector);
		InetAccessHandler ihandler = new InetAccessHandler();
		
		Handler.Sequence shandler = createHandler();

		ihandler.setHandler(shandler);
		server.setHandler(ihandler);
		server.start();
	}
	
	/**
	 * Create a WebAppContext
	 * @return
	 */
	abstract protected Handler.Sequence createHandler();
	
	@Override
	public int getPort() 
	{
	    return port;
	}

	@Override
	public void refresh() throws UnableToCompleteException 
	{
	}

	@Override
	public void stop() throws UnableToCompleteException 
	{
	}
}