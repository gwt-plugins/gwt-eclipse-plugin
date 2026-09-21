package com.gwtplugins.jettylauncher;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * A classloader to load the classes of the Jetty App Server, that are needed to start GWT
 */
public class Jetty12ClassLoader extends URLClassLoader
{
   private ClassLoader parentInt;
   
   public Jetty12ClassLoader(JEEVersion jeeversion) throws IOException
   {	   
      super(new URL[0], null);
      URL[] urls = getJettyURLs(jeeversion);
      for(URL url : urls)
      {
         addURL(url);
      }
      this.parentInt = Thread.currentThread().getContextClassLoader();
   }
   
   @Override
   public Class<?> loadClass(String name) throws ClassNotFoundException
   {
      Class<?> clazz = findLoadedClass(name);
      //load these classes by this classloader.
      if(clazz == null && 
         (name.startsWith("com.gwtplugins.jettylauncher")
            )
         )
      {
         String resName = name.replace('.', '/').concat(".class");
         URL res = parentInt.getResource(resName);
         try
         {
            InputStream in = res.openStream();
            byte[] def = readAll(in);
            clazz = defineClass(name, def, 0, def.length);
         }
         catch (IOException e)
         {
            throw new ClassNotFoundException("Unable to load class", e);
         }
      }
      if(clazz == null)
      {
         //load classes of this URLClassLoader or System classes
         try
         {
            clazz = super.loadClass(name);
         }
         catch(ClassNotFoundException ex)
         {}
      }
      if(clazz == null)
      {
         if(name.startsWith("com.google.gwt")
            || name.startsWith("java."))
         {
            //Load classes from parent classloader, that are not in this URLClassloader
            return parentInt.loadClass(name);
         }
      }
      if(clazz == null)
      {
         throw new ClassNotFoundException(name);
      }
      return clazz;
   }
   
   @Override
   public Enumeration<URL> getResources(String name) throws IOException
   {
      return super.getResources(name);
   }
   
   public byte[] readAll(InputStream in) throws IOException
   {
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      byte[] buffer = new byte[65536];
      int count = in.read(buffer);
      while (count != -1)
      {
         if (count > 0)
         {
            bout.write(buffer, 0, count);
         }
         count = in.read(buffer);
      }
      bout.close();
      return bout.toByteArray();
   }
   
   
   private URL[] getJettyURLs(JEEVersion jeeVersion) throws IOException
   {
      File jettyHome;
      String jhome = System.getProperty("JETTY_HOME");
      if(jhome == null)
      {
         jhome = System.getenv("JETTY_HOME");
      }
      if(jhome == null)
      {
         throw new IOException("JETTY_HOME not set");
      }      
      else
      {
         jettyHome = new File(jhome);
         if(jettyHome.exists() == false)
         {
            throw new IOException("JETTY_HOME not set");
         }
      }
      List<URL> urls = new ArrayList<>();
      File libs = new File(jettyHome, "lib");
      addJarsFrom(urls, libs, jeeVersion);
      libs = new File(jettyHome, "lib/logging");
      addJarsFrom(urls, libs);
      if(JEEVersion.JEE10.equals(jeeVersion))
      {
         libs = new File(jettyHome, "lib/ee10-annotations");
         addJarsFrom(urls, libs);
         libs = new File(jettyHome, "lib/ee10-apache-jsp");
         addJarsFrom(urls, libs);
         libs = new File(jettyHome, "lib/ee10-jaspi");
         addJarsFrom(urls, libs);
      }
      else if(JEEVersion.JEE8.equals(jeeVersion))
      {
         libs = new File(jettyHome, "lib/ee8-annotations");
         addJarsFrom(urls, libs);
         libs = new File(jettyHome, "lib/ee8-apache-jsp");
         addJarsFrom(urls, libs);
      }
      return urls.toArray(new URL[urls.size()]);
   }

   private void addJarsFrom(List<URL> urls, File dir) throws IOException
   {
      addJarsFrom(urls, dir, null);
   }
   
   private void addJarsFrom(List<URL> urls, File dir, JEEVersion jeeVersion) throws IOException
   {
      File[] files = dir.listFiles();
      for(File f : files)
      {
         if(f.isFile() && f.getName().endsWith(".jar"))
         {
          	if(JEEVersion.JEE8.equals(jeeVersion)
            		&& f.getName().startsWith("jetty-ee10"))
        	{
        		continue;
        	}
         	else if(JEEVersion.JEE10.equals(jeeVersion)
        		&& f.getName().startsWith("jetty-ee8"))
        	{
        		continue;
        	}
            URL url = f.toURI().toURL();
            urls.add(url);
            System.out.println("Added jar to classpath: " + f);
         }
      }
   }
}
