import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.exware.nobuto.Utilities;

public class Build extends de.exware.nobuto.JavaBuilder
{
    private List<AbstractGWTBuild> subprojects = new ArrayList<>();
    
    public Build()
    {
        subprojects.add(new GdtAPIClientLibBuild());
        subprojects.add(new GdtPlatformBuild());
        subprojects.add(new GdtCoreBuild());
        subprojects.add(new GwtCoreBuild());
        subprojects.add(new GwtOophmBuild());
        subprojects.add(new GdtSuiteBuild());
        subprojects.add(new GwtWtpBuild());
        subprojects.add(new GwtWtpMavenBuild());
        subprojects.add(new GdtSuiteUpdateBuild());
        subprojects.add(new GdtMavenBuild());
        subprojects.add(new GwtGssBuild());
        subprojects.add(new SuiteV3FeatureBuild());
    }
    
    @Override
    public void dist() throws Exception
    {
        new File(Config.UPDATE_SITE, "plugins").mkdirs();
        new File(Config.UPDATE_SITE, "features").mkdirs();
        Utilities.copy("resources/content.xml", Config.UPDATE_SITE, false);
        Utilities.copy("resources/artifacts.xml", Config.UPDATE_SITE, false);
        
        for(int i=0;i<subprojects.size();i++)
        {
            AbstractGWTBuild builder = subprojects.get(i);
            System.out.println("Build Subproject: " + builder.getProjectName());
            
            Utilities.delete(Config.TMP);
            Utilities.delete(Config.CLASSES_DIR);
            String plugin = builder.getProjectName();
            builder.dist();
            String version = builder.getVersion();
            Utilities.replaceInFile(Config.UPDATE_SITE + "/content.xml", "UTF-8", " (\\w*?)='" + plugin + "' (\\w*?)='.*?'", " $1='" + plugin + "' $2='" + version + "'");
            Utilities.replaceInFile(Config.UPDATE_SITE + "/artifacts.xml", "UTF-8", " (\\w*?)='" + plugin + "' (\\w*?)='.*?'", " $1='" + plugin + "' $2='" + version + "'");
        }
    }

    public void clean() throws IOException
    {
        System.out.println("Cleaning up");
        Utilities.delete(Config.CLASSES_DIR);
        Utilities.delete(Config.DISTRIBUTION_DIR);
        Utilities.delete(Config.TMP);
        Utilities.delete(Config.UPDATE_SITE);
    }
}
