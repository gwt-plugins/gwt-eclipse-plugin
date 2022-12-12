package sub;
import java.io.File;
import java.io.IOException;

import de.exware.nobuto.Utilities;

public class SuiteV3FeatureBuild extends AbstractGWTBuild
{
    public SuiteV3FeatureBuild()
    {
        super("com.gwtplugins.eclipse.suite.v3.feature");
    }
    
    @Override
    protected void copyEclipseFiles(File classesDir) throws IOException 
    {
    	super.copyEclipseFiles(classesDir);
        Utilities.replaceInFile(classesDir.getPath() + "/feature.xml", "UTF-8", "version=\"[\\w0-9\\.]+?\"(\\r\\n|\\n)",
                "version=\"" + getVersion() + "\"$1");                            
    }
    
    @Override
    public String getProjectDir()
    {
        return "features/" + getProjectName();
    }
}
