import java.io.File;

public class GdtSuiteBuild extends AbstractGWTBuild
{
    public GdtSuiteBuild()
    {
        super("com.gwtplugins.gdt.eclipse.suite");
    }
    
    @Override
    public void compile() throws Exception
    {
        addSources(getProjectDir() + "/src");

        addClasspathItem(new File("plugins/com.gwtplugins.gdt.eclipse.core/lib/guava-11.0.1.jar").getAbsolutePath());
        addClasspathItem(new File("plugins/com.gwtplugins.gwt.eclipse.oophm/lib/gwt-dev-transport.jar").getAbsolutePath());
        addClasspathItem(new File("plugins/com.gwtplugins.gwt.eclipse.core/libs/gwt-user.jar").getAbsolutePath());
        addClasspathItem(new File("plugins/com.gwtplugins.gwt.eclipse.core/libs/gwt-dev.jar").getAbsolutePath());
        
        addSiblingJar("com.gwtplugins.gdt.eclipse.core");
        addSiblingJar("com.gwtplugins.gwt.eclipse.core");
        addSiblingJar("com.gwtplugins.gdt.eclipse.platform");
        addSiblingJar("com.gwtplugins.gwt.eclipse.oophm");

        addPluginJarToClasspath("javax.inject_");
        addPluginJarToClasspath("org.eclipse.core.commands_");
        addPluginJarToClasspath("org.eclipse.core.expressions_");
        addPluginJarToClasspath("org.eclipse.core.filesystem_");
        addPluginJarToClasspath("org.eclipse.core.jobs_");
        addPluginJarToClasspath("org.eclipse.core.runtime_");
        addPluginJarToClasspath("org.eclipse.core.resources_");
        addPluginJarToClasspath("org.eclipse.core.variables_");
        addPluginJarToClasspath("org.eclipse.e4.ui.workbench_");
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
        addPluginJarToClasspath("org.eclipse.jst.server.core_");
        addPluginJarToClasspath("org.eclipse.osgi_");
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
        addPluginJarToClasspath("org.eclipse.wst.jsdt.core_");
        addPluginJarToClasspath("org.eclipse.wst.jsdt.ui");
        addPluginJarToClasspath("org.eclipse.wst.server.core_");
        addPluginJarToClasspath("org.eclipse.wst.sse.core_");
        addPluginJarToClasspath("org.eclipse.wst.sse.ui_");
        addPluginJarToClasspath("org.eclipse.wst.xml.core_");

        super.compile();
    }

}
