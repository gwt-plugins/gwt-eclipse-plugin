
public class GdtPlatformBuild extends AbstractGWTBuild
{
    public GdtPlatformBuild()
    {
        super("com.gwtplugins.gdt.eclipse.platform");
    }
    
    @Override
    public void compile() throws Exception
    {
        addSources(getProjectDir() + "/src");

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

}
