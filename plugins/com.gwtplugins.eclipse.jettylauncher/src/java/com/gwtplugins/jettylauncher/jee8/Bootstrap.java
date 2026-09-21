package com.gwtplugins.jettylauncher.jee8;

import java.io.File;

import com.google.gwt.core.ext.ServletContainer;

/**
 * Bootstrap Class is loaded by Reflection by the Jetty12ClassLoader to separate the ClassLoader of the WebApp
 * from anything else on the classpath.
 */
public class Bootstrap
{
   public static ServletContainer start(int port, File appRootDir)
   {
      return new Jetty12ServletContainer(port, appRootDir);
   }
}
