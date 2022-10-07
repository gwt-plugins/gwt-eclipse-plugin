

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import de.exware.nobuto.JavaBuilder;
import de.exware.nobuto.Maven;
import de.exware.nobuto.Utilities;

abstract public class AbstractGWTBuild extends JavaBuilder
{
    protected String projectname;
    private Maven maven = new Maven();

    public AbstractGWTBuild(String projectname)
    {
        this.projectname = projectname;
    }
    
    @Override
    public void dist() throws Exception
    {
        checkTools();
        File distDir = new File(Config.DISTRIBUTION_DIR);
        distDir.mkdirs();
        File classesDir = new File(Config.CLASSES_DIR);
        classesDir.mkdirs();
        File tmpmaven = new File(Config.TMP, "maven");
        tmpmaven.mkdirs();
        
        copyLibs();

        compile();
        
        File pluginProps = new File(getProjectDir() + "/plugin.properties");
        if(pluginProps.exists())
        {
            Utilities.copy(pluginProps, classesDir, true);
        }
        File pluginXML = new File(getProjectDir() + "/plugin.xml");
        if(pluginXML.exists())
        {
            Utilities.copy(pluginXML, classesDir, true);
        }
        
        copyIcons();
        
        File target = new File(Config.TMP, "make-jar");
        target.mkdirs();
        Utilities.copy("out", target, true);
        File manifestFile = new File(getProjectDir() + "/META-INF/MANIFEST.MF");
        String jarname = Config.DISTRIBUTION_DIR + "/" + getProjectName() + "_" + getVersion() + ".jar";
        if(manifestFile.exists())
        {
            Utilities.replaceInFile(manifestFile.getPath(), "UTF-8", "Bundle-Version: .*",
                "Bundle-Version: " + getVersion());
            jar(jarname, target.getPath(), manifestFile.getPath());
        }
        else
        {
            jar(jarname, target.getPath(), null);
        }
        Utilities.copy(jarname, Config.UPDATE_SITE + "/" + new File(getProjectDir()).getParentFile().getPath(), true);
    }
    
    @Override
    public void compile() throws Exception
    {
        addSources(getProjectDir() + "/src");

        addClasspathItem(new File("plugins/com.gwtplugins.gdt.eclipse.apiclientlib/lib/google-http-client-1.16.0-rc.jar").getAbsolutePath());
        addClasspathItem(new File("plugins/com.gwtplugins.gdt.eclipse.core/lib/guava-11.0.1.jar").getAbsolutePath());
        addClasspathItem(new File("plugins/com.gwtplugins.gdt.eclipse.apiclientlib/lib/jetty-util-6.1.26.jar").getAbsolutePath());
        addClasspathItem(new File("plugins/com.gwtplugins.gwt.eclipse.oophm/lib/gwt-dev-transport.jar").getAbsolutePath());
        addClasspathItem(new File("plugins/com.gwtplugins.gwt.eclipse.core/libs/gwt-dev.jar").getAbsolutePath());
        addClasspathItem(new File("plugins/com.gwtplugins.gwt.eclipse.core/libs/gwt-user.jar").getAbsolutePath());

        addMavenJarToClasspath("javax.servlet", "javax.servlet-api", "4.0.1");
        addMavenJarToClasspath("org.apache.ant", "ant", "1.10.9");
        addMavenJarToClasspath("org.apache.maven", "maven-artifact", "3.0");
        addMavenJarToClasspath("org.apache.maven", "maven-core", "3.0");
        addMavenJarToClasspath("org.apache.maven", "maven-model", "3.0");
        addMavenJarToClasspath("org.codehaus.plexus", "plexus-utils", "3.4.2");
        
        addEclipseJarToClasspath("javax.inject_1.0.0.v20091030.jar");
        addEclipseJarToClasspath("org.eclipse.core.commands_3.10.100.v20210722-1426.jar");
        addEclipseJarToClasspath("org.eclipse.core.contenttype_3.8.100.v20210910-0640.jar");
        addEclipseJarToClasspath("org.eclipse.core.expressions_3.8.100.v20210910-0640.jar");
        addEclipseJarToClasspath("org.eclipse.core.filebuffers_3.7.100.v20210909-1906.jar");
        addEclipseJarToClasspath("org.eclipse.core.filesystem_1.9.200.v20210912-1851.jar");
        addEclipseJarToClasspath("org.eclipse.core.jobs_3.12.0.v20210723-1034.jar");
        addEclipseJarToClasspath("org.eclipse.core.runtime_3.24.0.v20210910-0750.jar");
        addEclipseJarToClasspath("org.eclipse.core.resources_3.16.0.v20211001-2032.jar");
        addEclipseJarToClasspath("org.eclipse.core.variables_3.5.100.v20210721-1355.jar");
        addEclipseJarToClasspath("org.eclipse.debug.ui_3.15.200.v20211108-1752.jar");
        addEclipseJarToClasspath("org.eclipse.e4.ui.workbench_1.13.100.v20211019-0756.jar");
        addEclipseJarToClasspath("org.eclipse.equinox.app_1.6.100.v20211021-1418.jar");
        addEclipseJarToClasspath("org.eclipse.equinox.common_3.15.100.v20211021-1418.jar");
        addEclipseJarToClasspath("org.eclipse.equinox.preferences_3.9.100.v20211021-1418.jar");
        addEclipseJarToClasspath("org.eclipse.equinox.registry_3.11.100.v20211021-1418.jar");
        addEclipseJarToClasspath("org.eclipse.equinox.p2.operations_2.6.0.v20210315-2228.jar");
        addEclipseJarToClasspath("org.eclipse.equinox.p2.ui_2.7.300.v20211104-1311.jar");
        addEclipseJarToClasspath("org.eclipse.equinox.p2.ui.sdk_1.2.2.v20211104-1616.jar");
        addEclipseJarToClasspath("org.eclipse.debug.core_3.18.300.v20211117-1829.jar");
        addEclipseJarToClasspath("org.eclipse.debug.ui_3.15.200.v20211108-1752.jar");
        addEclipseJarToClasspath("org.eclipse.jdt.core_3.28.0.v20211117-1416.jar");
        addEclipseJarToClasspath("org.eclipse.jdt.core.manipulation_1.15.100.v20211115-1252.jar");
        addEclipseJarToClasspath("org.eclipse.jdt.debug_3.19.0.v20211112-1303.jar");
        addEclipseJarToClasspath("org.eclipse.jdt.debug.ui_3.12.500.v20211108-1545.jar");
        addEclipseJarToClasspath("org.eclipse.jdt.junit_3.13.0.v20210811-0812.jar");
        addEclipseJarToClasspath("org.eclipse.jdt.junit.core_3.11.100.v20211112-1026.jar");
        addEclipseJarToClasspath("org.eclipse.jdt.launching_3.19.400.v20211011-0920.jar");
        addEclipseJarToClasspath("org.eclipse.jdt.ui_3.25.0.v20211115-1252.jar");
        addEclipseJarToClasspath("org.eclipse.jetty.util_10.0.6.jar");
        addEclipseJarToClasspath("org.eclipse.jetty.server_10.0.6.jar");
        addEclipseJarToClasspath("org.eclipse.jetty.servlet_10.0.6.jar");
        addEclipseJarToClasspath("org.eclipse.jface_3.24.0.v20211110-1517.jar");
        addEclipseJarToClasspath("org.eclipse.jface.text_3.19.0.v20211026-2100.jar");
        addEclipseJarToClasspath("org.eclipse.jst.server.core_1.2.700.v202103192305.jar");
        addEclipseJarToClasspath("org.eclipse.ltk.core.refactoring_3.12.100.v20210926-1112.jar");
        addEclipseJarToClasspath("org.eclipse.m2e.core_1.18.3.20211018-0804.jar");
        addEclipseJarToClasspath("org.eclipse.m2e.jdt_1.18.3.20211112-0847.jar");
        addEclipseJarToClasspath("org.eclipse.m2e.wtp_1.4.4.20201128-1705.jar");
        addEclipseJarToClasspath("org.eclipse.mylyn.commons.ui_3.25.2.v20200813-0821.jar");
        addEclipseJarToClasspath("org.eclipse.osgi_3.17.100.v20211104-1730.jar");
        addEclipseJarToClasspath("org.eclipse.search_3.14.0.v20211108-0804.jar");
        addEclipseJarToClasspath("org.eclipse.swt.gtk.linux.x86_64_3.118.0.v20211123-0851.jar");
        addEclipseJarToClasspath("org.eclipse.text_3.12.0.v20210512-1644.jar");
        addEclipseJarToClasspath("org.eclipse.ui.browser_3.7.100.v20211105-1434.jar");
        addEclipseJarToClasspath("org.eclipse.ui.console_3.11.100.v20210721-1355.jar");
        addEclipseJarToClasspath("org.eclipse.ui.editors_3.14.300.v20210913-0815.jar");
        addEclipseJarToClasspath("org.eclipse.ui.forms_3.11.300.v20211022-1451.jar");
        addEclipseJarToClasspath("org.eclipse.ui.ide_3.18.400.v20211026-0701.jar");
        addEclipseJarToClasspath("org.eclipse.ui.workbench.texteditor_3.16.300.v20211119-1032.jar");
        addEclipseJarToClasspath("org.eclipse.ui.workbench_3.124.0.v20211116-0651.jar");
        addEclipseJarToClasspath("org.eclipse.update.configurator_3.4.800.v20210415-1314.jar");
        addEclipseJarToClasspath("org.eclipse.wst.common.frameworks_1.2.202.v202108200212.jar");
        addEclipseJarToClasspath("org.eclipse.wst.common.frameworks.ui_1.2.401.v202007142017.jar");
        addEclipseJarToClasspath("org.eclipse.wst.common.modulecore_1.3.200.v202108200212.jar");
        addEclipseJarToClasspath("org.eclipse.wst.common.project.facet.core_1.4.402.v202111212041.jar");
        addEclipseJarToClasspath("org.eclipse.wst.common.project.facet.ui_1.4.602.v202007142017.jar");
        addEclipseJarToClasspath("org.eclipse.wst.css.core_1.3.0.v202101180039.jar");
        addEclipseJarToClasspath("org.eclipse.wst.css.ui_1.2.0.v202101180039.jar");
        addEclipseJarToClasspath("org.eclipse.wst.jsdt.core_2.0.303.v202007221940.jar");
        addEclipseJarToClasspath("org.eclipse.wst.jsdt.ui_2.1.300.v202106021855.jar");
        addEclipseJarToClasspath("org.eclipse.wst.server.core_1.10.200.v202106020138.jar");
        addEclipseJarToClasspath("org.eclipse.wst.sse.core_1.2.700.v202107272335.jar");
        addEclipseJarToClasspath("org.eclipse.wst.sse.ui_1.7.300.v202111190506.jar");
        addEclipseJarToClasspath("org.eclipse.wst.validation_1.2.801.v202007142017.jar");
        addEclipseJarToClasspath("org.eclipse.wst.xml.core_1.2.400.v202107272335.jar");
        addEclipseJarToClasspath("org.eclipse.wst.xml.ui_1.2.600.v202102222242.jar");
        
        unjar(Config.TMP + "/eclipse/plugins/org.eclipse.jdt.debug_3.19.0.v20211112-1303.jar", new File(Config.TMP + "/eclipse/plugins"));
        addClasspathItem(Config.TMP + "/eclipse/plugins/jdimodel.jar");
        
        super.compile();
    }
    
    private void copyIcons() throws IOException
    {
        File iconsDir = new File(getProjectDir() + "/icons");
        if(iconsDir.exists())
        {
            File target = new File(Config.TMP, "make-jar/icons");
            target.mkdirs();
            Utilities.copy(iconsDir, target, true);
        }
    }

    private void copyLibs() throws IOException
    {
        File libsDir = new File(getProjectDir() + "/libs");
        if(libsDir.exists())
        {
            File target = new File(Config.TMP, "make-jar/libs");
            target.mkdirs();
            Utilities.copy(libsDir, target, true);
        }
        libsDir = new File(getProjectDir() + "/lib");
        if(libsDir.exists())
        {
            File target = new File(Config.TMP, "make-jar/lib");
            target.mkdirs();
            Utilities.copy(libsDir, target, true);
        }
    }

    protected void addSiblingJar(String name)
    {
        File pluginsDir = new File(Config.DISTRIBUTION_DIR);
        File lib = findJarInPlugins(pluginsDir, name);
        addClasspathItem(lib.getAbsolutePath());
    }

    protected void addPluginJarToClasspath(String name) throws IOException
    {
        File pluginsDir = new File(Config.ECLIPSE_DIR, "plugins");
        File lib = findJarInPlugins(pluginsDir, name);
        if(lib == null)
        {
            throw new IOException("Library " + name + " not found");
        }
        addClasspathItem(lib.getAbsolutePath());
    }

    protected void addMavenJarToClasspath(String groupID, String artifactId, String version) throws IOException
    {
        File lib = maven.get(groupID, artifactId, version, Config.TMP + "/maven");
        if(lib == null)
        {
            throw new IOException("Library " + artifactId + " not found");
        }
        addClasspathItem(lib.getPath());
    }

    protected void addEclipseJarToClasspath(String plugin) throws IOException
    {
        File file = new File(Config.TMP, "eclipse/plugins/" + plugin);
        if(file.exists() == false)
        {
            file.getParentFile().mkdirs();
            String purl = Config.ECLIPSE_URL + "/plugins/" + plugin;
            URL url = new URL(purl);
            BufferedInputStream in = new BufferedInputStream(url.openStream());
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
            Utilities.copy(in, out);
            in.close();
            out.close();
        }
        addClasspathItem(file.getPath());
    }

    public String getProjectName()
    {
        return projectname;
    }

    public String getProjectDir()
    {
        return "plugins/" + getProjectName();
    }
}