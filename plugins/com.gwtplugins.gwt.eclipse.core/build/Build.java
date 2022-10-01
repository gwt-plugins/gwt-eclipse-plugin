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
        Utilities.copy(new File("plugin.xml"), target, true);
        Utilities.copy(new File("plugin.properties"), target, true);
        Utilities.copy(new File("src/com/google/gwt/eclipse/core/GWTPlugin.properties"), new File(target, "com/google/gwt/eclipse/core/GWTPlugin.properties"), true);
        Utilities.replaceInFile("META-INF/MANIFEST.MF", "UTF-8", "Bundle-Version: .*",
            "Bundle-Version: " + getVersion());
        jar(DISTRIBUTION_DIR + "/com.gwtplugins.gwt.eclipse.core_" + getVersion() + ".jar", target.getPath(),
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
        String tmpDir = System.getProperty("java.io.tmpdir");

        addClasspathItem(new File("../com.gwtplugins.gdt.eclipse.core/lib/guava-11.0.1.jar").getAbsolutePath());
        addClasspathItem(new File("libs/gwt-dev.jar").getAbsolutePath());
        addClasspathItem(new File("libs/gwt-user.jar").getAbsolutePath());
        File lib = findJarInPlugins(new File("../com.gwtplugins.gdt.eclipse.platform/dist"), "com.gwtplugins.gdt.eclipse.platform_");
        addClasspathItem(lib.getAbsolutePath());
        lib = findJarInPlugins(new File("../com.gwtplugins.gdt.eclipse.core/dist"), "com.gwtplugins.gdt.eclipse.core");
        addClasspathItem(lib.getAbsolutePath());

        addPluginJarToClasspath("org.eclipse.core.commands_");
        addPluginJarToClasspath("org.eclipse.core.contenttype_");
        addPluginJarToClasspath("org.eclipse.core.expressions_");
        addPluginJarToClasspath("org.eclipse.core.filebuffers_");
        addPluginJarToClasspath("org.eclipse.core.filesystem_");
        addPluginJarToClasspath("org.eclipse.core.jobs_");
        addPluginJarToClasspath("org.eclipse.core.runtime_");
        addPluginJarToClasspath("org.eclipse.core.resources_");
        addPluginJarToClasspath("org.eclipse.core.variables_");
        addPluginJarToClasspath("org.eclipse.debug.ui_");
        addPluginJarToClasspath("org.eclipse.e4.ui.workbench_");
        addPluginJarToClasspath("org.eclipse.equinox.app_");
        addPluginJarToClasspath("org.eclipse.equinox.common_");
        addPluginJarToClasspath("org.eclipse.equinox.preferences_");
        addPluginJarToClasspath("org.eclipse.equinox.registry_");
        addPluginJarToClasspath("org.eclipse.debug.core_");
        addPluginJarToClasspath("org.eclipse.debug.ui");
        addPluginJarToClasspath("org.eclipse.jdt.core_");
        addPluginJarToClasspath("org.eclipse.jdt.core.manipulation_");
        addPluginJarToClasspath("jdimodel.jar");
        addPluginJarToClasspath("org.eclipse.jdt.debug.ui_");
        addPluginJarToClasspath("org.eclipse.jdt.junit_");
        addPluginJarToClasspath("org.eclipse.jdt.junit.core_");
        addPluginJarToClasspath("org.eclipse.jdt.launching_");
        addPluginJarToClasspath("org.eclipse.jdt.ui_");
        addPluginJarToClasspath("org.eclipse.jface_");
        addPluginJarToClasspath("org.eclipse.jface.text");
        addPluginJarToClasspath("org.eclipse.ltk.core.refactoring_");
        addPluginJarToClasspath("org.eclipse.osgi_");
        addPluginJarToClasspath("org.eclipse.search_");
        addPluginJarToClasspath("org.eclipse.swt");
        addPluginJarToClasspath("org.eclipse.text");
        addPluginJarToClasspath("org.eclipse.ui.browser_");
        addPluginJarToClasspath("org.eclipse.ui.console_");
        addPluginJarToClasspath("org.eclipse.ui.editors_");
        addPluginJarToClasspath("org.eclipse.ui.forms");
        addPluginJarToClasspath("org.eclipse.ui.ide");
        addPluginJarToClasspath("org.eclipse.ui.workbench.texteditor");
        addPluginJarToClasspath("org.eclipse.ui.workbench_");
        addPluginJarToClasspath("org.eclipse.wst.common.modulecore_");
        addPluginJarToClasspath("org.eclipse.wst.common.project.facet.core_");
        addPluginJarToClasspath("org.eclipse.wst.css.core_");
        addPluginJarToClasspath("org.eclipse.wst.css.ui");
        addPluginJarToClasspath("org.eclipse.wst.jsdt.core_");
        addPluginJarToClasspath("org.eclipse.wst.jsdt.ui");
        addPluginJarToClasspath("org.eclipse.wst.sse.core_");
        addPluginJarToClasspath("org.eclipse.wst.sse.ui_");
        addPluginJarToClasspath("org.eclipse.wst.validation_");
        addPluginJarToClasspath("org.eclipse.wst.xml.core_");
        addPluginJarToClasspath("org.eclipse.wst.xml.ui_");

        super.compile();
    }

    private void addPluginJarToClasspath(String name)
    {
        File pluginsDir = new File(ECLIPSE_DIR, "plugins");
        File lib = findJarInPlugins(pluginsDir, name);
        addClasspathItem(lib.getAbsolutePath());
    }
}
