package com.gwtplugins.jettylauncher;

import java.io.File;
import java.lang.reflect.Method;
import java.net.BindException;

import com.google.gwt.core.ext.ServletContainer;
import com.google.gwt.core.ext.ServletContainerLauncher;
import com.google.gwt.core.ext.TreeLogger;

/**
 * Base class for the Jetty12Launchers.
 */
public abstract class AbstractJetty12Launcher extends ServletContainerLauncher {
	
   @Override
   public ServletContainer start(TreeLogger logger, int port, File appRootDir) throws BindException, Exception {
      // A custom Classloader to separate the classes of the webapp and app server from the rest of the system.
      Jetty12ClassLoader cl = new Jetty12ClassLoader(getJEEVersion());
      Thread.currentThread().setContextClassLoader(cl);
      //Load and run the Bootstrap in the context of the custom ClassLoader.
      Class<?> bsClass = cl.loadClass(getBootstrapClassname());
      Class<?>[] types = new Class[2];
      types[0] = int.class;
      types[1] = File.class;
      Method method = bsClass.getDeclaredMethod("start", types);
      Object[] params = new Object[2];
      params[0] = port;
      params[1] = appRootDir;
      ServletContainer cont = (ServletContainer) method.invoke(null, params);
      return cont;
   }
   
   /** 
    * The jee Version to load. If jee10 ist activated, the jee8 libraries will not be loaded.
    * @return
    */
   protected abstract JEEVersion getJEEVersion();

   /**
    * The name of the class that is used to separate the classloader from other eclipse classes.
    * @return
    */
   abstract protected String getBootstrapClassname();   
}
