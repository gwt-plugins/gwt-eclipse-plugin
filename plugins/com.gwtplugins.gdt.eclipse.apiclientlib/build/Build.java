import java.io.File;
import java.io.IOException;

import de.exware.nobuto.Utilities;

public class Build extends de.exware.nobuto.JavaBuilder
{
    private static final String CLASSES_DIR = "out";
    private static final String TMP = "tmp";
    private static final String DISTRIBUTION_DIR = "dist";

    @Override
    public void dist() throws Exception
    {
        clean();
        new File(DISTRIBUTION_DIR).mkdirs();

        File target = new File(TMP, "make-jar/lib");
        target.mkdirs();
        Utilities.copy("lib", target.getPath(), true);
        Utilities.copy(new File("plugin.properties"), target, true);
        Utilities.replaceInFile("META-INF/MANIFEST.MF", "UTF-8", "Bundle-Version: .*", "Bundle-Version: " + getVersion());
        jar(DISTRIBUTION_DIR + "/com.gwtplugins.gdt.eclipse.apiclientlib_" + getVersion() + ".jar", target.getParent(), "META-INF/MANIFEST.MF");
    }

    public void clean() throws IOException
    {
        System.out.println("Cleaning upx");
        Utilities.delete(CLASSES_DIR);
        Utilities.delete(DISTRIBUTION_DIR);
        Utilities.delete(TMP);
    }

}
