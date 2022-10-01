import java.io.File;
import java.io.IOException;

import de.exware.nobuto.Utilities;

public class Build extends de.exware.nobuto.JavaBuilder
{
    private static final String CLASSES_DIR = "out";
    private static final String TMP = "tmp";
    private static final String DISTRIBUTION_DIR = "dist";
    private static final String ECLIPSE_DIR = "/usr/local/eclipse";

    @Override
    public void dist() throws Exception
    {
        File distDir = new File(DISTRIBUTION_DIR);
        distDir.mkdirs();
        File classesDir = new File(CLASSES_DIR);
        classesDir.mkdirs();
        File target = new File(TMP, "make-jar/libs");
        target.mkdirs();

        super.dist();
        Utilities.copy(new File("libs"), target, true);
        target = target.getParentFile();
        Utilities.copy(new File("out"), target, true);
        Utilities.copy(new File("plugin.properties"), target, true);
        Utilities.replaceInFile("META-INF/MANIFEST.MF", "UTF-8", "Bundle-Version: .*",
            "Bundle-Version: " + getVersion());
        jar(DISTRIBUTION_DIR + "/com.gwtplugins.gdt.eclipse.platform_" + getVersion() + ".jar", target.getPath(),
            "META-INF/MANIFEST.MF");
    }

    public void clean() throws IOException
    {
        System.out.println("Cleaning up");
        Utilities.delete(CLASSES_DIR);
        Utilities.delete(DISTRIBUTION_DIR);
        Utilities.delete(TMP);
    }

    @Override
    public void compile() throws Exception
    {
        addSources("src");

        addPluginJarToClasspath("javax.servlet_");
        addPluginJarToClasspath("org.eclipse.core.commands_");
        addPluginJarToClasspath("org.eclipse.core.jobs_");
        addPluginJarToClasspath("org.eclipse.core.runtime_");
        addPluginJarToClasspath("org.eclipse.core.resources_");
        addPluginJarToClasspath("org.eclipse.equinox.common_");
        addPluginJarToClasspath("org.eclipse.equinox.p2.operations_");
        addPluginJarToClasspath("org.eclipse.equinox.p2.ui_");
        addPluginJarToClasspath("org.eclipse.equinox.p2.ui.sdk_");
        addPluginJarToClasspath("org.eclipse.debug.core_");
        addPluginJarToClasspath("org.eclipse.debug.ui");
        addPluginJarToClasspath("org.eclipse.jdt.core_");
        addPluginJarToClasspath("org.eclipse.jdt.core.manipulation_");
        addPluginJarToClasspath("org.eclipse.jdt.debug.ui_");
        addPluginJarToClasspath("org.eclipse.jdt.ui_");
        addPluginJarToClasspath("org.eclipse.jetty.util_");
        addPluginJarToClasspath("org.eclipse.jetty.server_");
        addPluginJarToClasspath("org.eclipse.jetty.servlet_");
        addPluginJarToClasspath("org.eclipse.jface_");
        addPluginJarToClasspath("org.eclipse.jface.text");
        addPluginJarToClasspath("org.eclipse.jst.server.core_");
        addPluginJarToClasspath("org.eclipse.osgi_");
        addPluginJarToClasspath("org.eclipse.swt");
        addPluginJarToClasspath("org.eclipse.text");
        addPluginJarToClasspath("org.eclipse.ui.workbench.texteditor");
        addPluginJarToClasspath("org.eclipse.ui.workbench_");
        addPluginJarToClasspath("org.eclipse.wst.css.core_");
        addPluginJarToClasspath("org.eclipse.wst.server.core_");
        addPluginJarToClasspath("org.eclipse.wst.sse.core_");
        addPluginJarToClasspath("org.eclipse.wst.sse.ui_");
        addPluginJarToClasspath("org.mortbay.jetty.util_");

        super.compile();
    }

    private void addPluginJarToClasspath(String name)
    {
        File pluginsDir = new File(ECLIPSE_DIR, "plugins");
        File lib = findJarInPlugins(pluginsDir, name);
        addClasspathItem(lib.getAbsolutePath());
    }
}
