/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
 *
 *  All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.appengine.eclipse.core.proxy;

import com.google.appengine.eclipse.core.AppEngineCorePlugin;
import com.google.appengine.eclipse.core.AppEngineCorePluginLog;
import com.google.appengine.eclipse.core.sdk.AppEngineBridge;
import com.google.appengine.tools.admin.AdminException;
import com.google.appengine.tools.admin.AppAdmin;
import com.google.appengine.tools.admin.AppAdminFactory;
import com.google.appengine.tools.admin.AppAdminFactory.ConnectOptions;
import com.google.appengine.tools.admin.Application;
import com.google.appengine.tools.admin.UpdateFailureEvent;
import com.google.appengine.tools.admin.UpdateListener;
import com.google.appengine.tools.admin.UpdateProgressEvent;
import com.google.appengine.tools.admin.UpdateSuccessEvent;
import com.google.appengine.tools.info.SdkInfo;
import com.google.appengine.tools.info.UpdateCheck;
import com.google.appengine.tools.info.Version;
import com.google.apphosting.runtime.security.WhiteList;
import com.google.apphosting.utils.config.AppEngineConfigException;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gdt.eclipse.core.deploy.DeploymentSet;
import com.google.gdt.eclipse.core.sdk.SdkUtils;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.xml.sax.SAXParseException;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.management.ReflectionException;

/**
 * The implementation of the bridging layer between the GAE Plugin and the App
 * Engine SDK. This class will be loaded in an isolated classloader along with
 * the App Engine SDK. The parent classloader of this isolated classloader will
 * be that of the GAE Plugin.
 */
public class AppEngineBridgeImpl implements AppEngineBridge {

  private static final class DeployUpdateListener implements UpdateListener {

    /**
     * Class for getting headers representing different stages of deployments
     * bassed on console messages from the gae sdk.
     */
    private static class MessageHeaders {

      // Headers should go in the order specified in this array.
      private static final
          PrefixHeaderPair[] prefixHeaderPairs = new PrefixHeaderPair[] {
              new PrefixHeaderPair("Preparing to deploy", null,
                  "Created staging directory", "Scanning files on local disk"),
              new PrefixHeaderPair(
                  "Deploying", null, "Uploading"), new PrefixHeaderPair(
                  "Verifying availability", "Verifying availability of",
                  "Will check again in 1 seconds."), new PrefixHeaderPair(
                  "Updating datastore", null, "Uploading index")};

      /*
       * The headers should go in the sequence specified in the array, so keep
       * track of which header we're currently looking for.
       */
      private int currentPrefixHeaderPair;

      PrefixHeaderPair getMessageHeader(String msg) {
        PrefixHeaderPair php = prefixHeaderPairs[currentPrefixHeaderPair];
        for (String prefix : php.msgPrefixes) {
          if (msg.startsWith(prefix)) {
            currentPrefixHeaderPair = (currentPrefixHeaderPair + 1)
                % prefixHeaderPairs.length;
            return php;
          }
        }
        return null;
      }
    }

    /**
     * Class for holding the different gae sdk messages that are associated with
     * different "headers", representing the stages of deployment.
     */
    private static class PrefixHeaderPair {
      // the header that should be displayed on the console
      final String header;

      // the prefixes of console messages that trigger this header
      final String[] msgPrefixes;

      // the header that should be displayed on the progress dialog, mainly for
      // displaying "verifying availability" on the console and displaying
      // "verifying availability of "backend""
      final String taskHeader;

      PrefixHeaderPair(
          String header, String taskHeader, String... msgPrefixes) {
        this.msgPrefixes = msgPrefixes;
        this.header = header;
        if (taskHeader == null) {
          this.taskHeader = header;
        } else {
          this.taskHeader = taskHeader;
        }
      }
    }

    /**
     * Attempts to reflectively call getDetails() on the event object received
     * by the onFailure or onSuccess callback. That method is only supported by
     * App Engine SDK 1.2.1 or later. If we are able to call getDetails we
     * return the details message; otherwise we return <code>null</code>.
     */
    private static String getDetailsIfSupported(Object updateEvent) {
      try {
        Method method = updateEvent.getClass().getDeclaredMethod("getDetails");
        return (String) method.invoke(updateEvent);
      } catch (SecurityException e) {
        AppEngineCorePluginLog.logError(e);
      } catch (IllegalArgumentException e) {
        AppEngineCorePluginLog.logError(e);
      } catch (IllegalAccessException e) {
        AppEngineCorePluginLog.logError(e);
      } catch (InvocationTargetException e) {
        AppEngineCorePluginLog.logError(e);
      } catch (NoSuchMethodException e) {
        // Expected on App Engine SDK 1.2.0; no need to log
      }
      return null;
    }

    /**
     * Reflectively checks to see if an exception is a JspCompilationException,
     * which is only supported by App Engine SDK 1.2.1 or later.
     */
    private static boolean isJspCompilationException(Throwable ex) {
      if (ex != null) {
        try {
          Class<?> jspCompilationExceptionClass = Class.forName(
              "com.google.appengine.tools.admin.JspCompilationException");
          return jspCompilationExceptionClass.isAssignableFrom(ex.getClass());
        } catch (ClassNotFoundException e) {
          // Expected on App Engine SDK 1.2.0; no need to log
        }
      }
      return false;
    }

    private int deployments; // how many units have been deployed so far
    private String deploymentUnitName;
    private final int deploymentUnitsCount; // the total number of deployments
    private final PrintWriter errorWriter;
    private MessageHeaders messageHeaders;
    private final IProgressMonitor monitor;
    private final PrintWriter outputWriter;

    private int percentDone = 0;

    private IStatus status = Status.OK_STATUS;

    private DeployUpdateListener(IProgressMonitor monitor,
        DeploymentSet deploymentSet, PrintWriter outputWriter,
        PrintWriter errorWriter) {
      this.monitor = monitor;
      this.outputWriter = outputWriter;
      this.errorWriter = errorWriter;
      this.deploymentUnitsCount = deploymentSet.getDeploymentUnitsCount();
      messageHeaders = new MessageHeaders();
    }

    public IStatus getStatus() {
      return status;
    }

    public void onFailure(UpdateFailureEvent event) {
      monitor.done();

      // Create status object and print error message to the writer
      status = new Status(IStatus.ERROR, AppEngineCorePlugin.PLUGIN_ID,
          event.getFailureMessage(), event.getCause());
      outputWriter.println(event.getFailureMessage());

      // Only print the details for JSP compilation errors
      if (isJspCompilationException(event.getCause())) {
        String details = getDetailsIfSupported(event);
        if (details != null) {
          outputWriter.println(details);
        }
      }
    }

    public void onProgress(UpdateProgressEvent event) {

      // Update the progress monitor
      int worked = event.getPercentageComplete() - percentDone;
      percentDone += worked;
      // scale progress to the total number of things we're deploying
      monitor.worked(worked / deploymentUnitsCount);

      String msg = event.getMessage();
      PrefixHeaderPair php = messageHeaders.getMessageHeader(msg);

      if (php != null) {
        monitor.setTaskName(php.taskHeader + " " + deploymentUnitName);
        outputWriter.println("\n" + php.header + ":");
      }
      outputWriter.println("\t" + msg);

      // Check for user cancellation
      if (monitor.isCanceled()) {
        event.cancel();
        status = new Status(
            IStatus.CANCEL, AppEngineCorePlugin.PLUGIN_ID, "User cancelled");
        outputWriter.println(status.getMessage());
      }
    }

    public void onSuccess(UpdateSuccessEvent event) {
      deployments++;
      percentDone = 0;
      if (deployments == deploymentUnitsCount) {
        monitor.done();
      }

      String details = getDetailsIfSupported(event);
      if (details != null) {
        // Note that unlike in onFailure, we're writing to the log file here,
        // not to the console. This is so we don't clutter our deployment
        // console with a bunch of info or warning messages from JSP
        // compilation, if we deployed successfully.
        errorWriter.println(details);
      }

      outputWriter.println("\nDeployment completed successfully");
    }

    /**
     * Println's the given string to this DeployUpdateListener's output writer.
     */
    public void println(String s) {
      outputWriter.println(s);
    }

    public void setDeploymentUnitName(String name) {
      this.deploymentUnitName = name;
    }
  }

  private enum Lib {
    TOOLS("getOptionalToolsLib", "getOptionalToolsLibs"),
    USER("getOptionalUserLib", "getOptionalUserLibs");

    private final String getOptionalLib;
    private final String getOptionalLibs;

    private Lib(String o1, String o2) {
      getOptionalLib = o1;
      getOptionalLibs = o2;
    }

    public String getOptionalLib() {
      return getOptionalLib;
    }

    public String getOptionalLibs() {
      return getOptionalLibs;
    }
  }

  private static class VersionComparator implements Comparator<String> {
    public int compare(String version1, String version2)
        throws NumberFormatException {
      return SdkUtils.compareVersionStrings(version1, version2);
    }
  }

  private static final String APPENGINE_API_JAR_NAME_REGEX = "appengine-api-.*sdk.*\\.jar";

  private static final IPath APPENGINE_TOOLS_API_JAR_PATH = new Path(
      "lib/appengine-tools-api.jar");

  // FIXME: Change this to the proper value once App Engine starts reporting
  // version information correctly.
  private static final String MIN_SUPPORTED_VERSION = "0.0.0";

  private static final VersionComparator
      VERSION_COMPARATOR = new VersionComparator();

  // package protected for testing
  static AppAdmin createAppAdmin(final DeployOptions options)
      throws IOException {
    AppAdminFactory appAdminFactory = new AppAdminFactory();

    if (options.getJavaExecutableOSPath() != null) {
      appAdminFactory.setJavaExecutable(
          new File(options.getJavaExecutableOSPath()));
    }

    if (options.getJavaCompilerExecutableOSPath() != null) {
      appAdminFactory.setJavaCompiler(
          new File(options.getJavaCompilerExecutableOSPath()));
    }

    Application app = Application.readApplication(
        options.getDeployFolderOSPath());

    PrintWriter errorWriter = new PrintWriter(options.getErrorStream(), true);
    ConnectOptions appEngineConnectOptions = new ConnectOptions();
    appEngineConnectOptions.setOauthToken(options.getOAuth2Token());

    String appengineServer = System.getenv("APPENGINE_SERVER");
    if (appengineServer != null) {
      appEngineConnectOptions.setServer(appengineServer);
    }
    appAdminFactory.setJarSplittingEnabled(true);

    AppAdmin appAdmin = appAdminFactory.createAppAdmin(
        appEngineConnectOptions, app, errorWriter);

    return appAdmin;
  }

  private static void getLibs(File dir, List<File> list) {
    for (File f : dir.listFiles()) {
      if (f.isDirectory()) {
        getLibs(f, list);
      } else {
        if (f.getName().endsWith(".jar")) {
          list.add(f);
        }
      }
    }
  }

  public AppEngineBridgeImpl() throws CoreException {
    if (!hasSdkInfo() || !isCompatibleVersion()) {
      throw new CoreException(StatusUtilities.newErrorStatus((
          "App Engine SDK version must be greater than " + MIN_SUPPORTED_VERSION
          + " in order to work with this plugin."),
          AppEngineCorePlugin.PLUGIN_ID));
    }
  }

  public IStatus deploy(final IProgressMonitor monitor, DeployOptions options)
      throws IOException {

    /*
     * Note that we're using the same consoleOutputStream for both the output of
     * the update check, and deploy output. This should be safe, because both of
     * these operations are synchronous, so the same thread is being used.
     *
     * TODO: Should we do the update check in a separate thread? If the update
     * check hangs, then the deployment will not occur.
     */
    UpdateCheck updateCheck = new UpdateCheck(SdkInfo.getDefaultServer());
    if (updateCheck.allowedToCheckForUpdates()) {
      updateCheck.maybePrintNagScreen(
          new PrintStream(options.getOutputStream(), true));
    }

    AppAdmin appAdmin = null;
    try {
      appAdmin = createAppAdmin(options);
    } catch (AppEngineConfigException e) {

      /*
       * When reading an Application, the bundled tools jar will be (should be)
       * whatever the current version is, so it knows to look for the
       * backends.xml file. But if the SDK version for this project is old, then
       * there won't be a backends.xsd file to validate the backends.xml file.
       * This shows up as a SAXParseException wrapped in a
       * AppEngineConfigException. Deployment should still work if the SDK for
       * this project is old and there is no backends.xml file.
       */

      if (e.getCause() instanceof SAXParseException) {
        String msg = e.getCause().getMessage();

        // have to check what the message says to distinguish a file-not-found
        // problem from some other xml problem.
        if (msg.contains("Failed to read schema document")
            && msg.contains("backends.xsd")) {
          return new Status(IStatus.ERROR, AppEngineCorePlugin.PLUGIN_ID,
              "Deploying a project with backends requires App Engine SDK 1.5.0 or greater.",
              e);
        } else {
          throw e;
        }
      } else {
        throw e;
      }
    }

    DeploymentSet deploymentSet = options.getDeploymentSet();

    DeployUpdateListener deployUpdateListener = new DeployUpdateListener(
        monitor, deploymentSet,
        new PrintWriter(options.getOutputStream(), true),
        new PrintWriter(options.getErrorStream(), true));

    try {

      if (deploymentSet.getDeployFrontend()) {
        deployUpdateListener.setDeploymentUnitName("frontend");
        deployUpdateListener.println(
            "\n------------ Deploying frontend ------------");
        appAdmin.update(deployUpdateListener);
      }

      for (String backendName : deploymentSet.getBackendNames()) {
        deployUpdateListener.setDeploymentUnitName(backendName);
        deployUpdateListener.println("\n------------ Deploying backend \""
            + backendName + "\" ------------");
        appAdmin.updateBackend(backendName, deployUpdateListener);
      }

      return deployUpdateListener.getStatus();
    } catch (AdminException e) {
      return new Status(IStatus.ERROR, AppEngineCorePlugin.PLUGIN_ID,
          e.getLocalizedMessage(), e);
    }
  }

  public String getAppId(IPath warPath) throws IOException {
    Application app = Application.readApplication(warPath.toOSString());
    return app.getAppId();
  }

  public String getAppVersion(IPath warPath) throws IOException {
    Application app = Application.readApplication(warPath.toOSString());
    return app.getVersion();
  }

  public List<File> getBuildclasspathFiles() throws ReflectionException {
    return getBuildclasspathFiles(true);
  }

  public List<File> getBuildclasspathFiles(boolean getDatanucleusFiles) throws ReflectionException {
    List<File> classpathFiles = new ArrayList<File>();
    classpathFiles.addAll(getSharedLibFiles());

    /*
     * TODO: This should NOT be a build entry; it should only be added at runtime. However, to make
     * this work, we need to re-enable IRuntimeEntryResolvers. Just to get things going, we'll put
     * appengine-tools-api.jar on the build path.
     */
    classpathFiles.add(new Path(SdkInfo.getSdkRoot().getAbsolutePath()).append(
        APPENGINE_TOOLS_API_JAR_PATH).toFile());
    classpathFiles.addAll(getLatestUserLibFiles(getDatanucleusFiles));
    return classpathFiles;
  }

  public List<File> getLatestUserLibFiles(boolean getDatanucleusFiles) throws ReflectionException {
    if (SdkUtils.compareVersionStrings(getSdkVersion(), MIN_VERSION_FOR_OPT_DATANUCLEUS_LIB) < 0) {
      return getUserLibFiles();
    }
    List<File> userLibFiles = new ArrayList<File>();
    for (String libName : getUserLibNames()) {
      if (getDatanucleusFiles || !libName.equals("datanucleus")) {
        userLibFiles.addAll(getUserLibFiles(libName, getLatestVersion(libName)));
      }
    }
    File appengineApiJarFile = null;
    for (File jarFile : getUserLibFiles()) {
      if (jarFile.getName().matches(APPENGINE_API_JAR_NAME_REGEX)) {
        appengineApiJarFile = jarFile;
        break;
      }
    }
    if (appengineApiJarFile != null) {
      userLibFiles.add(appengineApiJarFile);
    } else {
      AppEngineCorePluginLog.logError("Unable to find appengine-api-1.0-sdk");
    }
    return userLibFiles;
  }

  public String getLatestVersion(String libName) throws ReflectionException {
    List<String> versions = getUserLibVersions(libName);
    String maxVersion = versions.get(0);
    for (String version : versions) {
      if (version.compareTo(maxVersion) > 0) {
        maxVersion = version;
      }
    }
    return maxVersion;
  }

  public String getSdkVersion() {
    return SdkInfo.getLocalVersion().getRelease();
  }

  public List<File> getSharedLibFiles() {
    return SdkInfo.getSharedLibFiles();
  }

  public List<File> getToolsLibFiles() {
    List<File> toolLibFiles = new ArrayList<File>();
    // FIXME: We should really be getting this list straight from SdkInfo
    getLibs(new File(SdkInfo.getSdkRoot(), "lib" + File.separator + "tools"),
        toolLibFiles);
    return toolLibFiles;
  }

  public List<File> getToolsLibFiles(String libName, String version) throws ReflectionException {
    return getLibFiles(libName, version, Lib.TOOLS);
  }

  public List<String> getToolsLibNames() throws ReflectionException {
    return getLibNames(Lib.TOOLS);
  }

  public List<String> getToolsLibVersions(String libName) throws ReflectionException {
    return getLibVersions(libName, Lib.TOOLS);
  }

  public List<File> getUserLibFiles() {
    return SdkInfo.getUserLibFiles();
  }

  public List<File> getUserLibFiles(String libName, String version) throws ReflectionException {
    return getLibFiles(libName, version, Lib.USER);
  }

  public List<String> getUserLibNames() throws ReflectionException {
    return getLibNames(Lib.USER);
  }

  public List<String> getUserLibVersions(String libName) throws ReflectionException {
    return getLibVersions(libName, Lib.USER);
  }

  public Set<String> getWhiteList() {
    return WhiteList.getWhiteList();
  }

  private List<File> getLibFiles(String libName, String version, Lib lib)
      throws ReflectionException {
    Method getFilesForVersion =
        getMethod(getOptionalLibClass(), "getFilesForVersion", String.class);
    Method getOptionalLib = getMethod(SdkInfo.class, lib.getOptionalLib(), String.class);
    Object optionalLib = invoke(getOptionalLib, null, libName);
    if (optionalLib == null) {
      return null;
    }
    return (List<File>) invoke(getFilesForVersion, optionalLib, version);
  }

  private List<String> getLibNames(Lib lib) throws ReflectionException {
    Method getName = getMethod(getOptionalLibClass(), "getName");
    Method getOptionalLibs = getMethod(SdkInfo.class, lib.getOptionalLibs());
    Collection<?> optionalLibs = (Collection<?>) invoke(getOptionalLibs, null);
    List<String> libs = new ArrayList<String>();
    for (Object optionalLib : optionalLibs) {
      libs.add((String) invoke(getName, optionalLib));
    }
    return libs;
  }

  private List<String> getLibVersions(String libName, Lib lib) throws ReflectionException {
      Method getVersions = getMethod(getOptionalLibClass(), "getVersions");
      Method getOptionalLib = getMethod(SdkInfo.class, lib.getOptionalLib(), String.class);
      Object x = invoke(getOptionalLib, null, libName);
      return (List<String>) invoke(getVersions, x);
  }

  private Method getMethod(Class<?> clazz, String method, Class<?>... parameterTypes)
      throws ReflectionException {
    try {
      return clazz.getMethod(method, parameterTypes);
    } catch (SecurityException e) {
      throw new ReflectionException(e, "Unable to load method " + method);
    } catch (NoSuchMethodException e) {
      throw new ReflectionException(e, "Unable to find method " + method);
    }
  }

  private Class<?> getOptionalLibClass() throws ReflectionException {
    try {
      return SdkInfo.class.getClassLoader()
          .loadClass("com.google.appengine.tools.info.OptionalLib");
    } catch (ClassNotFoundException e) {
      throw new ReflectionException(
          e, "Unable to load Optional Lib Class. Check if GAE SDK is up to date.");
    }
  }

  private boolean hasSdkInfo() {
    try {
      Class.forName("com.google.appengine.tools.info.SdkInfo", false, getClass().getClassLoader());
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  private Object invoke(Method method, Object obj, Object... args) throws ReflectionException {
    String errorMessage = "Unable to invoke method: " + method.toString();
    try {
      return method.invoke(obj, args);
    } catch (IllegalArgumentException e) {
      throw new ReflectionException(e, errorMessage);
    } catch (IllegalAccessException e) {
      throw new ReflectionException(e, errorMessage);
    } catch (InvocationTargetException e) {
      throw new ReflectionException(e, errorMessage);
    }
  }

  private boolean isCompatibleVersion() {
    Version sdkVersion = SdkInfo.getLocalVersion();

    return VERSION_COMPARATOR.compare(
        sdkVersion.getRelease(), MIN_SUPPORTED_VERSION) >= 0;
  }
}
