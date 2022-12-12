import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.exware.nobuto.Utilities;
import de.exware.nobuto.eclipse.EclipseUpdateSite;
import de.exware.nobuto.eclipse.Repository;
import de.exware.nobuto.eclipse.Unit;
import de.exware.nobuto.java.JavaBuilder;
import sub.AbstractGWTBuild;
import sub.Config;
import sub.GdtAPIClientLibBuild;
import sub.GdtCoreBuild;
import sub.GdtMavenBuild;
import sub.GdtPlatformBuild;
import sub.GdtSuiteBuild;
import sub.GdtSuiteUpdateBuild;
import sub.GwtCoreBuild;
import sub.GwtGssBuild;
import sub.GwtOophmBuild;
import sub.GwtWtpBuild;
import sub.GwtWtpMavenBuild;
import sub.SuiteV3FeatureBuild;

public class Build extends JavaBuilder
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

        Repository repo = EclipseUpdateSite.createRepository();
        Unit unitGroup = repo.addUnit("com.gwtplugins.eclipse.suite.v3.feature.feature.group", getVersion());
        unitGroup.addProperty("org.eclipse.equinox.p2.name", "GWT Eclipse Plugin");

        for (int i = 0; i < subprojects.size(); i++)
        {
            AbstractGWTBuild builder = subprojects.get(i);
            String version = builder.getVersion();
            String projectname = builder.getProjectname();
            System.out.println("Build Subproject: " + projectname);
            
            unitGroup.addRequired("org.eclipse.equinox.p2.iu"
                , projectname, version);
            if(builder.getType().equals("feature"))
            {
                repo.addFeatureUnit(projectname, version);
                repo.addArtifact("org.eclipse.update.feature", projectname, version);
            }
            else
            {
                repo.addPluginUnit(projectname, version);
                repo.addArtifact("osgi.bundle", projectname, version);
            }

            Utilities.delete(Config.TMP + "/make-jar");
            builder.dist();
        }

        Unit unit = repo.addCategoryUnit("com.gwtplugins.eclipse", getVersion(), "GWT Eclipse Plugin",
            "An Eclipse GWT plugin");
        unit.addRequired("org.eclipse.equinox.p2.iu", "com.gwtplugins.eclipse.suite.v3.feature.feature.group", getVersion());
        
        addRequiredPlugins(unitGroup);
        
        repo.write(new File(Config.UPDATE_SITE));
    }

    private void addRequiredPlugins(Unit unitGroup)
    {
    	unitGroup.addRequired("osgi.bundle", "jakarta.servlet-api", "0.0.0");
    	unitGroup.addRequired("osgi.bundle", "org.eclipse.m2e.wtp", "0.0.0");
    	unitGroup.addRequired("osgi.bundle", "org.eclipse.jst.common.project.facet.ui", "0.0.0");
    	unitGroup.addRequired("osgi.bundle", "org.eclipse.wst.css.core", "0.0.0");
    	unitGroup.addRequired("osgi.bundle", "org.eclipse.wst.sse.ui", "0.0.0");
    	unitGroup.addRequired("osgi.bundle", "org.eclipse.wst.xml.ui", "0.0.0");
    	unitGroup.addRequired("osgi.bundle", "org.eclipse.wst.css.ui", "0.0.0");
    	unitGroup.addRequired("osgi.bundle", "org.eclipse.wst.jsdt.core", "0.0.0");
    	unitGroup.addRequired("osgi.bundle", "org.eclipse.wst.jsdt.manipulation", "0.0.0");
    	unitGroup.addRequired("osgi.bundle", "org.eclipse.wst.jsdt.ui", "0.0.0");
    	unitGroup.addRequired("osgi.bundle", "org.eclipse.wst.server.ui", "0.0.0");
    }
    
    public void distOld() throws Exception
    {
        new File(Config.UPDATE_SITE, "plugins").mkdirs();
        new File(Config.UPDATE_SITE, "features").mkdirs();
        Utilities.copy("resources/content.xml", Config.UPDATE_SITE, false);
        Utilities.copy("resources/artifacts.xml", Config.UPDATE_SITE, false);
        
        for(int i=0;i<subprojects.size();i++)
        {
            AbstractGWTBuild builder = subprojects.get(i);
            System.out.println("Build Subproject: " + builder.getProjectName());
            
            Utilities.delete(Config.TMP +  "/make-jar");
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
