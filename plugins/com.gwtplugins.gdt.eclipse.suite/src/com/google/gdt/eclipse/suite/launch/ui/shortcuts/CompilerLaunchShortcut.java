/**
 *
 */
package com.google.gdt.eclipse.suite.launch.ui.shortcuts;

import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.launch.CompilerLaunchConfiguration;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationUtilities;
import com.google.gdt.eclipse.suite.propertytesters.LaunchTargetTester;
import com.google.gwt.eclipse.core.launch.ModuleClasspathProvider;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import java.util.ArrayList;
import java.util.List;

public class CompilerLaunchShortcut implements ILaunchShortcut {

  @Override
  public void launch(IEditorPart editor, String mode) {
    IResource resource = ResourceUtils.getEditorInput(editor);

    if (resource != null) {
      launch(resource, mode);
    }
  }

  @Override
  public void launch(ISelection selection, String mode) {
    IResource resource = ResourceUtils.getSelectionResource(selection);

    if (resource != null) {
      launch(resource, mode);
    }
  }

  private void launch(IResource resource, String mode) {
    // assert that by the time we're in here, the PropertyTester agrees that we
    // should be here.
    assert (new LaunchTargetTester().test(resource, null, new Object[0], null));

    // Resolve to the actual resource (if it is linked)
    resource = ResourceUtils.resolveTargetResource(resource);

    try {
      ILaunchConfiguration config = findOrCreateLaunchConfiguration(resource);

      assert (config != null);

      DebugUITools.launch(config, mode);

    } catch (CoreException e) {
      CorePluginLog.logError(e);
    } catch (OperationCanceledException e) {
      // Abort launch
    }
  }

  private ILaunchConfiguration findOrCreateLaunchConfiguration(IResource resource)
      throws CoreException, OperationCanceledException {
    ILaunchConfiguration config = findLaunchConfiguration(resource);

    if (config == null) {
      config = createNewLaunchConfiguration(resource);
    }

    return config;
  }

  protected ILaunchConfiguration findLaunchConfiguration(IResource resource) throws CoreException {
    ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType typeid = launchManager.getLaunchConfigurationType(CompilerLaunchConfiguration.TYPE_ID);
    ILaunchConfiguration[] configs = launchManager.getLaunchConfigurations(typeid);

    return searchMatchingUrlAndProject(resource.getProject(), configs);
  }

  private ILaunchConfiguration searchMatchingUrlAndProject(IProject project, ILaunchConfiguration[] configs)
      throws CoreException {
    List<ILaunchConfiguration> candidates = new ArrayList<ILaunchConfiguration>();

    for (ILaunchConfiguration config : configs) {
      if (LaunchConfigurationUtilities.getProjectName(config).equals(project.getName())) {
        candidates.add(config);
      }
    }

    if (candidates.isEmpty()) {
      return null;
    } else if (candidates.size() == 1) {
      return candidates.get(0);
    } else {
      return LaunchConfigurationUtilities.chooseConfiguration(candidates, getShell());
    }
  }

  private Shell getShell() {
    return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
  }

  private ILaunchConfiguration createNewLaunchConfiguration(IResource resource)
      throws CoreException, OperationCanceledException {
    String initialName = resource.getProject().getName();

    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    String launchConfigName = manager.generateLaunchConfigurationName(initialName);
    IProject project = resource.getProject();

    ILaunchConfigurationWorkingCopy wc = createLaunchConfigWorkingCopy(launchConfigName, project);

    ILaunchConfiguration toReturn = wc.doSave();

    return toReturn;
  }

  private ILaunchConfigurationWorkingCopy createLaunchConfigWorkingCopy(String launchConfigName, IProject project)
      throws CoreException {
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType type = manager.getLaunchConfigurationType(CompilerLaunchConfiguration.TYPE_ID);

    final ILaunchConfigurationWorkingCopy config = type.newInstance(null, launchConfigName);

    // project name
    LaunchConfigurationUtilities.setProjectName(config, project.getName());

    // classpath
    config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER,
        ModuleClasspathProvider.computeProviderId(project));

    // Link the launch configuration to the project.
    // This will cause the launch config to be deleted automatically if the project is deleted.
    config.setMappedResources(new IResource[] { project });

    return config;
  }

}
