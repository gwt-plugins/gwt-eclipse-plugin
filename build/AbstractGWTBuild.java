

import java.io.File;
import java.io.IOException;

import de.exware.nobuto.JavaBuilder;
import de.exware.nobuto.Utilities;

abstract public class AbstractGWTBuild extends JavaBuilder
{
    protected String projectname;

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
        
        copyLibs();

        compile();
        
        File pluginProps = new File(getProjectDir() + "/plugin.properties");
        if(pluginProps.exists())
        {
            Utilities.copy(pluginProps, classesDir, true);
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

    public String getProjectName()
    {
        return projectname;
    }

    public String getProjectDir()
    {
        return "plugins/" + getProjectName();
    }
}
