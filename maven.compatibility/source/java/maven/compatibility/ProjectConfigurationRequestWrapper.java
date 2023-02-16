package maven.compatibility;

import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;

public class ProjectConfigurationRequestWrapper
{
    public static IProject getProject(ProjectConfigurationRequest request)
    {
        IProject project = null;
        try
        {
            project = ProjectConfigurationRequestWrapperModern.getProject(request);
        }
        catch(Exception ex)
        {
            project = ProjectConfigurationRequestWrapperOutdated.getProject(request);
        }
        return project;
    }

    public static Model getModel(ProjectConfigurationRequest request)
    {
        Model model = null;
        try
        {
            model = ProjectConfigurationRequestWrapperModern.getModel(request);
        }
        catch(Exception ex)
        {
            model = ProjectConfigurationRequestWrapperOutdated.getModel(request);
        }
        return model;
    }

    public static MavenProject getMavenProject(ProjectConfigurationRequest request)
    {
        MavenProject model = null;
        try
        {
            model = ProjectConfigurationRequestWrapperModern.getMavenProject(request);
        }
        catch(Exception ex)
        {
            model = ProjectConfigurationRequestWrapperOutdated.getMavenProject(request);
        }
        return model;
    }
}
