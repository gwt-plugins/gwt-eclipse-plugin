/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/

package com.google.gdt.eclipse.drive;

import com.google.common.annotations.VisibleForTesting;
import com.google.gdt.eclipse.core.AbstractGooglePlugin;
import com.google.gdt.eclipse.drive.images.ImageKeys;
import com.google.gdt.eclipse.drive.resources.PendingSaveManager;
import com.google.gdt.eclipse.drive.resources.WorkspaceUtils;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.osgi.framework.BundleContext;

import java.io.PrintStream;

import javax.annotation.concurrent.GuardedBy;

/**
 * The activator for the Drive plugin.
 */
@SuppressWarnings("restriction") // WorkbenchPlugin
public class DrivePlugin extends AbstractGooglePlugin {
  
  /**
   * An injectable factory for {@link ILog} objects.
   */
  @VisibleForTesting
  public interface ILogFactory {
    ILog get();
  }
  
  /**
   * An injectable factory for {@link ICommandService} objects.
   */
  @VisibleForTesting
  public interface ICommandServiceFactory {
    ICommandService get();
  }

  public static final String PLUGIN_ID = "com.google.gdt.eclipse.drive";
  
  private static final PrintStream alternativeLogStream = System.err;
  
  private static DrivePlugin plugin;

  public static DrivePlugin getDefault() {
    return plugin;
  }
  
  public static void log(int severity, String message, Throwable t) {
    if (plugin == null) {
      // We are running a test, or the plugin has been stopped.
      switch(severity) {
        case IStatus.OK:
          alternativeLogStream.print("OK: ");
          break;
        case IStatus.INFO:
          alternativeLogStream.print("INFO: ");
          break;
        case IStatus.WARNING:
          alternativeLogStream.print("WARNING: ");
          break;
        case IStatus.ERROR:
          alternativeLogStream.print("ERROR: ");
          break;
        case IStatus.CANCEL:
          alternativeLogStream.print("CANCEL: ");
          break;
        default:
          break;
      }
      alternativeLogStream.println(message);
      if (t != null) {
        t.printStackTrace(alternativeLogStream);
      }
    } else {
       plugin.logFactory.get().log(new Status(severity, PLUGIN_ID, message, t));
    }
  }
  
  public static void logError(String message, Throwable t) {
    log(IStatus.ERROR, message, t);
  }
  
  public static void logInfo(String message) {
    log(IStatus.INFO, message, null);
  }
  
  /**
   * Displays a nonmodal error dialog with a specified message in the active workbench window.
   * 
   * @param message the specified message
   */
  public static void displayUnloggedErrorDialog(final String message) {
    if (plugin.inTestMode) {
      System.err.println("Error dialog: " + message);
      return;
    }
    PlatformUI.getWorkbench().getDisplay().asyncExec(
        new Runnable() {
          @Override public void run() {
            Shell activeShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
            MessageDialog.openError(activeShell, "Error", message);
          }
       });
  }
  
  /**
   * Displays a nonmodal error dialog with a specified message and a reference to the Error Log in
   * the active workbench window. The message should consist of a single clause not ending with
   * punctuation; the message displayed in the error dialog consists of the specified message with
   * the text {@code "; see Error Log for details."} appended to the end.
   * 
   * @param message the specified message
   */
  public static void displayLoggedErrorDialog(final String message) {
    displayUnloggedErrorDialog(message + "; see Error Log for details.");
  }
  
  /**
   * Displays a nonmodal warning dialog with a specified title and specified message in the active
   * workbench window.
   * 
   * @param title the specified title
   * @param message the specified message
   */
  public static void displayWarningDialog(final String title, final String message) {
    PlatformUI.getWorkbench().getDisplay().asyncExec(
        new Runnable() {
          @Override public void run() {
            Shell activeShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
            MessageDialog.openWarning(activeShell, title, message);
          }
       });
  }
  
  @GuardedBy("this")
  private int pendingSaveAllCount;
  
  @GuardedBy("this")
  private String fileBeingUpdatedFromDrive;
  
  private PendingSaveManager pendingSaveManager;
  private ILogFactory logFactory;
  private ICommandServiceFactory commandServiceFactory;
  private boolean inTestMode;
  
  /**
   * Constructs a {@code DrivePlugin} with the standard production configuration. This constructor
   * is invoked reflectively during plugin startup.
   */
  public DrivePlugin() {
    this(
        new PendingSaveManager(),
        new ILogFactory() {
          @Override public ILog get() {
            return getDefault().getLog();
          }
        },
        new ICommandServiceFactory() {
          @Override public ICommandService get() {
            return (ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);
          }
        },
        false);
  }

  /**
   * Constructs a {@code DrivePlugin} configured for unit tests that do not depend on the workbench
   * environment.
   * 
   * @param pendingSaveManager the {@link PendingSaveManager} to be invoked by the unit test
   * @param logFactory a factory for the {@link ILog} to be invoked by the unit test
   * @param commandServiceFactory
   *     a factory for the {@link ICommandService} to be invoked by the unit test
   */
  @VisibleForTesting
  public DrivePlugin(
      PendingSaveManager pendingSaveManager, ILogFactory logFactory,
      ICommandServiceFactory commandServiceFactory) {
    this(pendingSaveManager, logFactory, commandServiceFactory, true);
  }
  
  private DrivePlugin(
      PendingSaveManager pendingSaveManager, ILogFactory logFactory,
      ICommandServiceFactory commandServiceFactory, boolean inTestMode) {
    this.pendingSaveManager = pendingSaveManager;
    this.logFactory = logFactory;
    this.commandServiceFactory = commandServiceFactory;
    this.inTestMode = inTestMode;
    pendingSaveAllCount = 0;
    fileBeingUpdatedFromDrive = null;
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    pendingSaveManager.initFromPreferences();
    finishStarting();
  }
  
  /**
   * Performs those duties of the {@link #start(BundleContext)} method that must be performed in
   * unit tests.
   */
  @VisibleForTesting void finishStarting() {
    plugin = this;
    ICommandService service = commandServiceFactory.get();
    service.addExecutionListener(
        new IExecutionListener(){
          @Override public void preExecute(String commandId, ExecutionEvent event) {
            if (commandId.equals(IWorkbenchCommandConstants.FILE_SAVE_ALL)) {
              synchronized (DrivePlugin.this) {
                pendingSaveAllCount++;
              }   
            }         
          }

          @Override public void postExecuteSuccess(String commandId, Object returnValue) {
            postExecute(commandId);
          }

          @Override public void postExecuteFailure(String commandId, ExecutionException exception) {
            postExecute(commandId);            
          }
          
          private void postExecute(String commandId) {
            if (commandId.equals(IWorkbenchCommandConstants.FILE_SAVE_ALL)) {
              boolean savingNeeded;
              synchronized (DrivePlugin.this) {
                pendingSaveAllCount--;
                savingNeeded = (pendingSaveAllCount == 0);
              }            
              if (savingNeeded) {
                saveAllAppsScriptProjects();
              }
            }
          }

          @Override public void notHandled(String commandId, NotHandledException exception) {
          }
        });
  }
  
  private static void saveAllAppsScriptProjects() {
    for (IProject project : WorkspaceUtils.allOpenAppsScriptProjects()) {
      // The following call on writeEclipseProjectToDrive results in a login prompt if the user is
      // not already logged in.
      DriveEclipseProjectMediator.getInstance().writeEclipseProjectToDrive(project);
    }
  }
  
  /**
   * Reports whether a {@link IWorkbenchCommandConstants.FILE_SAVE_ALL} action is in progress.
   * 
   * @return {@code true} if such an action is in progress, {@code false} otherwise
   */
  public synchronized boolean saveAllInProgress() {
    return pendingSaveAllCount > 0;
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    plugin = null;
    super.stop(context);
  }

  @Override
  protected void initializeImageRegistry(ImageRegistry reg) {
    super.initializeImageRegistry(reg);
    reg.put(
        ImageKeys.FOLDER_ICON, imageDescriptorFromSharedImageName(ISharedImages.IMG_OBJ_FOLDER));
    reg.put(ImageKeys.APPS_SCRIPT_PROJECT_ICON, imageDescriptorFromPath("icons/script_list.png"));
    reg.put(ImageKeys.UNSAVED_ICON, imageDescriptorFromPath("icons/not_saved.gif"));
  }
  
  @Override
  protected ImageDescriptor imageDescriptorFromPath(String imageFilePath) {
    return inTestMode ? null : super.imageDescriptorFromPath(imageFilePath);
  }
  
  private ImageDescriptor imageDescriptorFromSharedImageName(String sharedImageName) {
    return
        inTestMode ?
            null
            : WorkbenchPlugin.getDefault().getSharedImages().getImageDescriptor(sharedImageName);
  }
  
  public PendingSaveManager getPendingSaveManager() {
    return pendingSaveManager;
  }
  
  /**
   * Marks a specified file as in the process of being updated (either created, updated, or deleted)
   * from Drive. While a file is so marked, an {@code AppsScriptProjectResourceChangedListener} will
   * ignore the resulting resource-change event.
   * 
   * @param fileName the name of the specified file
   */
  public synchronized void startUpdatingFileFromDrive(String fileName) {
    if (fileBeingUpdatedFromDrive != null) {
      throw new IllegalStateException(
          "Attempt to start updating " + fileName + " while already updating " 
          + fileBeingUpdatedFromDrive);
    }
    fileBeingUpdatedFromDrive = fileName;
  }
  
  /**
   * Unmarks the file currently marked as in the process of being updated from Drive. It is
   * expected that this call will be paired with a preceding call on
   * {@link #startUpdatingFileFromDrive}.
   * 
   * @throws IllegalStateException
   *     if {@link #startUpdatingFileFromDrive} has not been called since the previous call
   *     (if any) on this method
   */
  public synchronized void endUpdatingFileFromDrive() {
    if (fileBeingUpdatedFromDrive == null) {
      throw new IllegalStateException("No file creation for import in progress");
    }
    fileBeingUpdatedFromDrive = null;
  }
  
  /**
   * Reports whether a specified file is currently marked as is the process of being updated from
   * Drive.
   * 
   * @param fileName the name of the specified file
   * @return whether the specified file is currently marked
   */
  public synchronized boolean isBeingUpdatedFromDrive(String fileName) {
    return fileName.equals(fileBeingUpdatedFromDrive);
  }
}
