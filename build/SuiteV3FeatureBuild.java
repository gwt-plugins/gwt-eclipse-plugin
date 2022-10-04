import de.exware.nobuto.Utilities;

public class SuiteV3FeatureBuild extends AbstractGWTBuild
{
    public SuiteV3FeatureBuild()
    {
        super("com.gwtplugins.eclipse.suite.v3.feature");
    }
    
    @Override
    public void dist() throws Exception
    {
        Utilities.replaceInFile(getProjectDir() + "/feature.xml", "UTF-8", "version=\"[\\w0-9\\.]+?\"(\\r\\n|\\n)",
            "version=\"" + getVersion() + "\"$1");                            
        super.dist();
    }
    
    @Override
    public void compile() throws Exception
    {
    }

    @Override
    public String getProjectDir()
    {
        return "features/" + getProjectName();
    }
}
