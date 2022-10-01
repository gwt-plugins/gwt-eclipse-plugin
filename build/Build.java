import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

import de.exware.nobuto.Maven;
import de.exware.nobuto.Utilities;

public class Build extends de.exware.nobuto.JavaBuilder
{
    private static final String CLASSES_DIR = "out";
    private static final String TMP = "tmp";
    private static final String DISTRIBUTION_DIR = "dist";
    private static final String UPDATE_SITE = "update-site";
    
    private List<String> plugins = new ArrayList();
    private List<String> features = new ArrayList();
    
    public Build()
    {
    	plugins.add("com.gwtplugins.gdt.eclipse.apiclientlib");
    	plugins.add("com.gwtplugins.gdt.eclipse.platform");
    	plugins.add("com.gwtplugins.gdt.eclipse.core");
    	plugins.add("com.gwtplugins.gwt.eclipse.core");
    	plugins.add("com.gwtplugins.gwt.eclipse.oophm");
    	plugins.add("com.gwtplugins.gdt.eclipse.suite");
    	plugins.add("com.gwtplugins.gwt.eclipse.wtp");
    	plugins.add("com.gwtplugins.gwt.eclipse.wtp.maven");
    	plugins.add("com.gwtplugins.gdt.eclipse.suite.update");
    	plugins.add("com.gwtplugins.gdt.eclipse.maven");
    	plugins.add("com.gwtplugins.gwt.eclipse.gss");
    	features.add("com.gwtplugins.eclipse.suite.v3.feature");
    }
    
    @Override
    public void dist() throws Exception
    {
        clean();
        new File(UPDATE_SITE, "plugins").mkdirs();
        new File(UPDATE_SITE, "features").mkdirs();
        Utilities.copy("resources/content.xml", UPDATE_SITE, false);
        Utilities.copy("resources/artifacts.xml", UPDATE_SITE, false);
        for(int i=0;i<plugins.size();i++)
        {
            String pluginDir = "plugins/" + plugins.get(i);
            String plugin = plugins.get(i);
            buildSubproject(pluginDir);
            Utilities.copy(pluginDir + "/dist", UPDATE_SITE + "/plugins", false);
            String version = Utilities.readTextFile(pluginDir + "/version.txt");
            Utilities.replaceInFile(UPDATE_SITE + "/content.xml", "UTF-8", " (\\w*?)='" + plugin + "' (\\w*?)='.*?'", " $1='" + plugin + "' $2='" + version + "'");
            Utilities.replaceInFile(UPDATE_SITE + "/artifacts.xml", "UTF-8", " (\\w*?)='" + plugin + "' (\\w*?)='.*?'", " $1='" + plugin + "' $2='" + version + "'");
        }
        for(int i=0;i<features.size();i++)
        {
            String featureDir = "features/" + features.get(i);
            String feature = features.get(i);
            buildSubproject(featureDir);
            Utilities.copy(featureDir + "/dist", UPDATE_SITE + "/features", false);
            String version = Utilities.readTextFile(featureDir + "/version.txt");
            Utilities.replaceInFile(UPDATE_SITE + "/content.xml", "UTF-8", " (\\w*?)='" + feature + "' (\\w*?)='.*?'", " $1='" + feature + "' $2='" + version + "'");
            Utilities.replaceInFile(UPDATE_SITE + "/artifacts.xml", "UTF-8", " (\\w*?)='" + feature + "' (\\w*?)='.*?'", " $1='" + feature + "' $2='" + version + "'");
        }
    }

    public void clean() throws IOException
    {
        System.out.println("Cleaning up");
        Utilities.delete(CLASSES_DIR);
        Utilities.delete(DISTRIBUTION_DIR);
        Utilities.delete(TMP);
        Utilities.delete(UPDATE_SITE);
    }
}
