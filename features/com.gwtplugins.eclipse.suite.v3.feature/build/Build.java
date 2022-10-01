import java.io.File;
import java.io.IOException;

import de.exware.nobuto.Maven;
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
        File target = new File(TMP, "make-jar");
        target.mkdirs();

        super.dist();
        Utilities.copy(new File("out"), target, true);
        Utilities.copy(new File("feature.xml"), target, true);
        Utilities.copy(new File("feature.properties"), target, true);
        Utilities.replaceInFile(target.getPath() + "/feature.xml", "UTF-8", "version=\"3.0.0.qualifier\"",
            "version=\"" + getVersion() + "\"");
        jar(DISTRIBUTION_DIR + "/com.gwtplugins.eclipse.suite.v3.feature_" + getVersion() + ".jar", target.getPath(), null);
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

        super.compile();
    }

    private void addPluginJarToClasspath(String name)
    {
        File pluginsDir = new File(ECLIPSE_DIR, "plugins");
        File lib = findJarInPlugins(pluginsDir, name);
        addClasspathItem(lib.getAbsolutePath());
    }
}
