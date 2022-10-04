import java.io.File;

public class GdtMavenBuild extends AbstractGWTBuild
{
    public GdtMavenBuild()
    {
        super("com.gwtplugins.gdt.eclipse.maven");
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

        addPluginJarToClasspath("maven-artifact-3");
        addPluginJarToClasspath("maven-core-3");
        addPluginJarToClasspath("maven-model-3");
        addPluginJarToClasspath("plexus-utils");
        addPluginJarToClasspath("org.eclipse.core.commands_");
        addPluginJarToClasspath("org.eclipse.core.filesystem_");
        addPluginJarToClasspath("org.eclipse.core.jobs_");
        addPluginJarToClasspath("org.eclipse.core.runtime_");
        addPluginJarToClasspath("org.eclipse.core.resources_");
        addPluginJarToClasspath("org.eclipse.core.variables_");
        addPluginJarToClasspath("org.eclipse.debug.core_");
        addPluginJarToClasspath("org.eclipse.debug.ui");
        addPluginJarToClasspath("org.eclipse.equinox.common_");
        addPluginJarToClasspath("org.eclipse.equinox.preferences_");
        addPluginJarToClasspath("org.eclipse.equinox.registry_");
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
        addPluginJarToClasspath("org.eclipse.m2e.core_");
        addPluginJarToClasspath("org.eclipse.osgi_");
        addPluginJarToClasspath("org.eclipse.swt");
        addPluginJarToClasspath("org.eclipse.text");
        addPluginJarToClasspath("org.eclipse.ui.console_");
        addPluginJarToClasspath("org.eclipse.ui.editors_");
        addPluginJarToClasspath("org.eclipse.ui.ide");
        addPluginJarToClasspath("org.eclipse.ui.workbench.texteditor");
        addPluginJarToClasspath("org.eclipse.ui.workbench_");
        addPluginJarToClasspath("org.eclipse.wst.common.modulecore_");
        addPluginJarToClasspath("org.eclipse.wst.common.project.facet.core_");
        addPluginJarToClasspath("org.eclipse.wst.css.core_");
        addPluginJarToClasspath("org.eclipse.wst.sse.core_");
        addPluginJarToClasspath("org.eclipse.wst.sse.ui_");
        addPluginJarToClasspath("org.eclipse.wst.xml.core_");

        super.compile();
    }

}
