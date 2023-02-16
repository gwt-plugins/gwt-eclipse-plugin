package maven.compatibility;

import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;

public class ProjectConfigurationRequestWrapperOutdated
{
    public static IProject getProject(ProjectConfigurationRequest request)
    {
        IProject project = request.getProject();
        return project;
    }

    public static Model getModel(ProjectConfigurationRequest request)
    {
        Model model = request.getMavenProject().getModel();
        return model;
    }
    
    public static MavenProject getMavenProject(ProjectConfigurationRequest request)
    {
        MavenProject project = request.getMavenProject();
        return project;
    }
}
