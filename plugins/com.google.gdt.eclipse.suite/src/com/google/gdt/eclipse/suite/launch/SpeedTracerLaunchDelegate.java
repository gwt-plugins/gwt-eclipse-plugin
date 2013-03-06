/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.google.gdt.eclipse.suite.launch;

import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.SWTUtilities;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.console.CustomMessageConsole;
import com.google.gdt.eclipse.core.console.MessageConsoleUtilities;
import com.google.gdt.eclipse.core.console.TerminateProcessAction;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationAttributeUtilities;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;
import com.google.gdt.eclipse.core.projects.ProjectChangeTimestampTracker;
import com.google.gdt.eclipse.suite.GdtPlugin;
import com.google.gdt.eclipse.suite.launch.processors.WarArgumentProcessor;
import com.google.gdt.eclipse.suite.launch.processors.WarArgumentProcessor.WarParser;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.compile.GWTCompileRunner;
import com.google.gwt.eclipse.core.compile.GWTCompileSettings;
import com.google.gwt.eclipse.core.launch.GWTLaunchConfiguration;
import com.google.gwt.eclipse.core.modules.IModule;
import com.google.gwt.eclipse.core.modules.ModuleUtils;
import com.google.gwt.eclipse.core.preferences.GWTPreferences;
import com.google.gwt.eclipse.core.properties.GWTProjectProperties;
import com.google.gwt.eclipse.core.speedtracer.SpeedTracerBrowserUtilities;
import com.google.gwt.eclipse.core.speedtracer.SpeedTracerLaunchConfiguration;
import com.google.gwt.eclipse.core.speedtracer.SymbolManifestGenerator;
import com.google.gwt.eclipse.oophm.model.ILaunchConfigurationFactory;
import com.google.gwt.eclipse.oophm.model.LaunchConfiguration;
import com.google.gwt.eclipse.oophm.model.WebAppDebugModel;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Performs the actual Speed Tracer launch, along with performing the operations
 * this depends on (e.g. GWT compile and symbol manifest generation)
 */
public class SpeedTracerLaunchDelegate extends JavaLaunchDelegate {

  private static final QualifiedName PREVIOUS_ST_BUILD_PROJECT_CHANGE_STAMP_KEY = new QualifiedName(
      SpeedTracerLaunchDelegate.class.getName(),
      "previousStBuildProjectChangeStamp");

  private static void abortLaunch(String errorMessage) throws CoreException {
    throw new CoreException(StatusUtilities.newErrorStatus(errorMessage,
        GdtPlugin.PLUGIN_ID));
  }

  private static void abortLaunchBecauseUserCancellation()
      throws OperationCanceledException {
    throw new OperationCanceledException("Speed Tracer launch canceled by user");
  }

  private static File createTempExtraDir() throws CoreException {
    File tempDir = null;
    try {
      tempDir = ResourceUtils.createTempDir("speedTracerGwtCompilationExtra",
          "");
    } catch (IOException e) {
      abortLaunch("Could not create a temporary directory.");
    }
    return tempDir;
  }

  private static boolean needsGenFiles(IPath warOutLocation) {
    IPath genFilesPath = GWTPreferences.computeSpeedTracerGeneratedFolderPath(warOutLocation);
    File genFilesFile = genFilesPath.toFile();
    return !ResourceUtils.folderExistsAndHasContents(genFilesFile);
  }
  
  private static boolean needsSymbolManifest(IPath warOutLocation) {
    File symbolsFolder = SymbolManifestGenerator.getSymbolMapsFolder(warOutLocation.toFile());
    return !ResourceUtils.folderExistsAndHasContents(symbolsFolder);
  }

  private static void printToCompilationConsole(IProject project, String msg) {
    PrintWriter printWriter = new PrintWriter(showGwtCompilationConsole(
      project).newMessageStream());
    printWriter.println(msg);
    printWriter.flush();
  }
  
  private static CustomMessageConsole showGwtCompilationConsole(IProject project) {
    CustomMessageConsole messageConsole = MessageConsoleUtilities.getMessageConsole(
        GWTCompileRunner.computeTaskName(project), null);
    messageConsole.activate();
    return messageConsole;
  }
  
  @Override
  public boolean buildForLaunch(ILaunchConfiguration config, String mode,
      IProgressMonitor originalMonitor) throws CoreException,
      OperationCanceledException {

    boolean performGwtCompile = LaunchConfigurationAttributeUtilities.getBoolean(
        config, SpeedTracerLaunchConfiguration.Attribute.PERFORM_GWT_COMPILE);
    if (!performGwtCompile) {
      return super.buildForLaunch(config, mode, originalMonitor);
    }

    SubMonitor subMonitor = SubMonitor.convert(originalMonitor);
    try {
      IJavaProject javaProject = getJavaProject(config);
      if (javaProject == null) {
        abortLaunch("The project is not a Java project.");
      }

      IProject project = javaProject.getProject();

      IPath warOutLocation = null;

      List<String> args = LaunchConfigurationProcessorUtilities.parseProgramArgs(config);
      WarParser parser = WarArgumentProcessor.WarParser.parse(args, javaProject);
      if (parser.isWarDirValid) {
        warOutLocation = new Path(parser.resolvedUnverifiedWarDir);
      }

      if (warOutLocation == null) {
        warOutLocation = WebAppUtilities.getWarOutLocationOrPrompt(project);

        if (warOutLocation == null) {
          throw new OperationCanceledException(
              "Speed Tracer launch canceled by the user");
        }
        
        // TODO: Copied from WebAppLaunchDelegate. We need to unify these two.
        WarArgumentProcessor warArgProcessor = new WarArgumentProcessor();
        warArgProcessor.setWarDirFromLaunchConfigCreation(warOutLocation.toOSString());

        ILaunchConfigurationWorkingCopy wc = config.getWorkingCopy();
        LaunchConfigurationProcessorUtilities.updateViaProcessor(
            warArgProcessor, wc);
        wc.doSave();
      }
      
      boolean needsGenFilesOrSymbolManifest = needsGenFiles(warOutLocation) || 
        needsSymbolManifest(warOutLocation);
      long curStamp = ProjectChangeTimestampTracker.getProjectTimestamp(project);
      if (curStamp == ProjectChangeTimestampTracker.getTimestampFromKey(
          project, PREVIOUS_ST_BUILD_PROJECT_CHANGE_STAMP_KEY)
          && !needsGenFilesOrSymbolManifest) {
        // No source change, do not re-build
        printToCompilationConsole(project, 
          "Skipping GWT compilation since no relevant changes have occurred since the last Speed Tracer session.");
        return super.buildForLaunch(config, mode, originalMonitor);
      }
      
      if (needsGenFilesOrSymbolManifest) {
        printToCompilationConsole(project, "Recompiling because generated files or symbol manifests for Speed Tracer are missing.");
      }
      
      File extraDir = createTempExtraDir();

      try {
        subMonitor.setTaskName("Performing GWT compile");
        List<String> modules = GWTLaunchConfiguration.getEntryPointModules(config);
        performGwtCompile(project, extraDir, modules, warOutLocation,
            subMonitor.newChild(70));
      } catch (OperationCanceledException e) {
        abortLaunchBecauseUserCancellation();
      } catch (Throwable e) {
        GdtPlugin.getLogger().logError(e, "Could not perform GWT compile");
        abortLaunch("Could not perform GWT compile, see logs for details.");
      }

      if (originalMonitor.isCanceled()) {
        abortLaunchBecauseUserCancellation();
      }

      try {
        // Generate the symbol manifest AFTER the compile, since the compile
        // wipes away ST artifacts
        subMonitor.setTaskName("Generating Speed Tracer symbol manifest");
        generateSymbolManifest(project, extraDir, warOutLocation);
      } catch (IOException e) {
        abortLaunch("Could not generate the symbol manifest file required by Speed Tracer.");
      }

      if (originalMonitor.isCanceled()) {
        abortLaunchBecauseUserCancellation();
      }

      extraDir.delete();
      
      /*
       * wtp publish
       */
      try {
        WebAppLaunchDelegate.maybePublishModulesToWarDirectory(config, subMonitor, javaProject, true);
      } catch (IOException e) {
        GdtPlugin.getLogger().logError(e, "Could not perform a WTP publish.");
      }

      // Build was successful, mark this last successful build
      project.setPersistentProperty(PREVIOUS_ST_BUILD_PROJECT_CHANGE_STAMP_KEY,
          String.valueOf(curStamp));

      final int amountOfWorkForSuperBuildForLaunch = 30;
      subMonitor.setWorkRemaining(amountOfWorkForSuperBuildForLaunch);
      return super.buildForLaunch(config, mode,
          subMonitor.newChild(amountOfWorkForSuperBuildForLaunch));

    } finally {
      if (originalMonitor != null) {
        originalMonitor.done();
      }
    }
  }

  @Override
  public void launch(ILaunchConfiguration configuration, String mode,
      ILaunch launch, IProgressMonitor monitor) throws CoreException {

    final String finalUrl = SpeedTracerLaunchConfiguration.getAbsoluteUrl(configuration);

    /*
     * LaunchConfigurations (in the OOPHM model) are added in one of two places:
     * (1) here and (2) as a callback from the GWT SDK's DevMode. For Speed
     * Tracer launches, the SpeedTracerLaunchConfiguration subclass must be used
     * (instead of just LaunchConfiguration.) Therefore, we call this method
     * _BEFORE_ we call through to super.launch() so there is no chance the GWT
     * SDK DevMode can add the launch configuration model object before us (if
     * it were to, it would add a regular LaunchConfiguration instance instead
     * of SpeedTracerLaunchConfiguration.)
     * 
     * TODO: This needs to be cleaned up. Speed Tracer launch support has been
     * added uncleanly to the existing DevMode view/model framework.
     */
    WebAppDebugModel.getInstance().addOrReturnExistingLaunchConfiguration(
        launch, null, new ILaunchConfigurationFactory() {
          public LaunchConfiguration newLaunchConfiguration(ILaunch launch,
              String name, WebAppDebugModel model) {
            return new com.google.gwt.eclipse.oophm.model.SpeedTracerLaunchConfiguration(
                launch, finalUrl, name, model);
          }
        });

    /*
     * Our super class, JavaLaunchDelegate, does not understand the profile
     * mode. Since we only use profile mode for UI purposes, pretend we are
     * launching as run mode.
     */
    super.launch(configuration, ILaunchManager.RUN_MODE, launch, monitor);

    try {
      
      SpeedTracerBrowserUtilities.createTrampolineFileAndAddToLaunch(finalUrl, launch);

    } catch (Throwable e) {
      Display.getDefault().asyncExec(new Runnable() {
        public void run() {
          MessageDialog.openWarning(
              SWTUtilities.getShell(),
              "Speed Tracer error",
              "Speed Tracer will not be opened automatically, please click on the Speed Tracer icon in the Chrome toolbar");
        }
      });
      GWTPluginLog.logWarning(e,
          "Could not create trampoline HTML file for Speed Tracer");
    }

    // Open Chrome
    SpeedTracerBrowserUtilities.openBrowser(finalUrl, launch);
  }

  private void generateSymbolManifest(IProject project, File extraDir,
      IPath warOutLocation)
      throws IOException, OperationCanceledException, CoreException {
    IModule[] modules = ModuleUtils.findAllModules(JavaCore.create(project),
        false);

    // TODO: only generate for modules selected in the launch config

    List<String> moduleNames = new ArrayList<String>();
    for (IModule module : modules) {
      moduleNames.add(module.getCompiledName());
    }

    SymbolManifestGenerator generator = new SymbolManifestGenerator(
        warOutLocation.toFile(), moduleNames, extraDir, project.getName());

    File warOutFolder = warOutLocation.toFile();
    File symbolManifestFile = SymbolManifestGenerator.getSymbolManifestFile(warOutFolder);
    String symbolManifestContents = generator.generate();

    ResourceUtils.writeToFile(symbolManifestFile, symbolManifestContents);
  }
  
  private void performGwtCompile(IProject project, File extraDir,
      List<String> modules, IPath warOutLocation, IProgressMonitor monitor) throws CoreException,
      IOException, InterruptedException {

    monitor.beginTask("Performing GWT compile...", 100);

    try {
      GWTCompileSettings compileSettings = GWTProjectProperties.getGwtCompileSettings(project);
      compileSettings.setEntryPointModules(!modules.isEmpty() ? modules
          : GWTProjectProperties.getEntryPointModules(project));
      compileSettings.setExtraArgs("-extra "
          + StringUtilities.quote(extraDir.getAbsolutePath())
          + " -gen " + StringUtilities.quote(GWTPreferences.computeSpeedTracerGeneratedFolderPath(warOutLocation).toOSString()));

      // Get a message console for GWT compiler output
      CustomMessageConsole messageConsole = showGwtCompilationConsole(project);
      OutputStream consoleOutputStream = messageConsole.newMessageStream();

      TerminateProcessAction terminateAction = new TerminateProcessAction();
      messageConsole.setTerminateAction(terminateAction);

      try {
        GWTCompileRunner.compileWithCancellationSupport(
            JavaCore.create(project), warOutLocation, compileSettings,
            consoleOutputStream, terminateAction, monitor, terminateAction);
      } finally {
        try {
          assert (consoleOutputStream != null);
          consoleOutputStream.close();
        } catch (IOException e) {
          // Ignore IOExceptions during stream close
        }
      }

      // refresh so that eclipse picks up on the gen files 
      project.refreshLocal(IResource.DEPTH_INFINITE, null);
      
    } finally {
      monitor.done();
    }
  }

}
