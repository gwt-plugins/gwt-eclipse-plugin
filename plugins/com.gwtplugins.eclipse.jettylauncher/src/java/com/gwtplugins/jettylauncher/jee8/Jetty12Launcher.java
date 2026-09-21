package com.gwtplugins.jettylauncher.jee8;

import com.gwtplugins.jettylauncher.AbstractJetty12Launcher;
import com.gwtplugins.jettylauncher.JEEVersion;

/**
 * Launches the Jetty 12 referenced by JETTY_HOME Environment with JEE8 (javax.servlet) API. 
 * <BR>
 * The full startup follows this order:
 * <ul>
 * <li>Create a Jetty12ClassLoader (which initializes its classpath with the libs found in JETTY_HOME.
 * <li>Use the classloader to load the Bootstrap Class
 * <li>invoke the start method of Bootstrap by reflection
 * <li>The start method will then create the Jetty12ServletContainer which is returned by this method.
 */
public class Jetty12Launcher extends AbstractJetty12Launcher
{

   @Override
   protected String getBootstrapClassname() {
	   return "com.gwtplugins.jettylauncher.jee8.Bootstrap";
   }

   @Override
   protected JEEVersion getJEEVersion() {
	  return JEEVersion.JEE8;
   }
}