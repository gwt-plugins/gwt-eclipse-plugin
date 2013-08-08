/*******************************************************************************
 * Copyright 2012 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.appengine.swarm.wizards.helpers;

import com.google.gdt.eclipse.appengine.swarm.AppEngineSwarmPlugin;
import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.jobs.UnzipRunnable;
import com.google.gdt.eclipse.managedapis.ManagedApiJsonClasses.ApiDependencies;
import com.google.gdt.eclipse.managedapis.ManagedApiPlugin;
import com.google.gdt.eclipse.managedapis.ManagedApiUtils;
import com.google.gdt.eclipse.managedapis.impl.ApiPlatformType;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The class responsible for generating Api library given service class. Acts as
 * interface between library involved in generation and GPE.
 */
public class SwarmApiCreator {

  private static final String JAVA = "JAVA";
  private static final String REST = "REST";
  private static final String RPC = "RPC";
  private static final String DEFAULT_APP_ID = "myapp";
  private static final String DEFAULT_API_NAME = "myapi";
  private static final String RPC_SUFFIX = "-rpc";
  private static final String REST_SUFFIX = "-rest";

  private final String appId;
  private ApiDependencies apiDependencies;

  private static final String DISCOVERY_API_ROOT = "https://webapis-discovery.appspot.com/_ah/api";
  private static final String CLIENT_LIB_GENERATOR = "https://developers.google.com/resources/api-libraries/generate";
  // This is used for creating a temp zip file, which can be extracted into the
  // ApiLibs directory of the App Engine project.
  private static final String TMP_ZIP_FILE_PREFIX = "tmp";
  private static final String TMP_ZIP_FILE_SUFFIX = ".zip";

  public SwarmApiCreator(String appId) {
    if (!"".equals(appId)) {
      this.appId = appId;
    } else {
      this.appId = DEFAULT_APP_ID;
    }
  }

  @SuppressWarnings("unchecked")
  public void createClientLibFromApiConfig(IProject project, String apiConfig, File outputFolder,
      SubMonitor monitor, ApiPlatformType platformType, String serviceClassName, ClassLoader loader)
      throws IOException, IllegalArgumentException, InvocationTargetException, CoreException,
      ClassNotFoundException, SecurityException, IllegalAccessException, NoSuchMethodException {
    generateAndWriteDiscovery(project, apiConfig, serviceClassName, RPC, loader);
    String discoveryDocRest = generateAndWriteDiscovery(project, apiConfig, serviceClassName, REST,
        loader);
    monitor.worked(20);
    File tempFile = File.createTempFile(TMP_ZIP_FILE_PREFIX, TMP_ZIP_FILE_SUFFIX);

    monitor.subTask("Generating Client Libraries. This may take a few minutes.");

    @SuppressWarnings({"rawtypes"})
    Class<Enum> languageEnum = (Class<Enum>) loader.loadClass("com.google.api.server.spi.tools.ClientLibGenerator$Language");
    Class<?> clientLibGenerator = loader.loadClass("com.google.api.server.spi.tools.CloudClientLibGenerator");

    String clientLibGenApiUrl = CLIENT_LIB_GENERATOR;
    Object clientLibGeneratorInstance = clientLibGenerator.getMethod("using", String.class).invoke(
        null, clientLibGenApiUrl);

    ArrayList<Object> methodArgs = new ArrayList<Object>();
    methodArgs.add(discoveryDocRest);
    methodArgs.add(Enum.valueOf(languageEnum, JAVA));
    methodArgs.add(ManagedApiPlugin.API_CLIENT_LANG_VERSION);
    methodArgs.add(tempFile);

    Method clientLibGeneratorMethod = clientLibGenerator.getMethod("generateClientLib",
        String.class, languageEnum, String.class, File.class);

    clientLibGeneratorMethod.invoke(clientLibGeneratorInstance, methodArgs.toArray());
    monitor.worked(20);

    if (monitor.isCanceled()) {
      throw new OperationCanceledException();
    }

    new UnzipRunnable(tempFile, outputFolder).run(monitor.newChild(1));
    tempFile.delete();
    monitor.worked(10);

    if (monitor.isCanceled()) {
      throw new OperationCanceledException();
    }

    apiDependencies = ManagedApiUtils.findAndReadDependencyFile(outputFolder);
    if (apiDependencies == null) {
      throw new CoreException(new Status(IStatus.ERROR, AppEngineSwarmPlugin.PLUGIN_ID,
          "apiDependencies not initialized."));
    }
    monitor.worked(5);

    if (monitor.isCanceled()) {
      throw new OperationCanceledException();
    }

    for (ApiDependencies.File f : apiDependencies.getFiles()) {
      // Detect fileName from the relative path.
      String fileName = f.getPath();
      if (fileName.contains("/")) {
        fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
      }
      if (fileName.contains(SwarmServiceCreator.SOURCE_JAR_NAME_SUBSTRING)
          && fileName.endsWith(SwarmServiceCreator.JAR_FILE_EXTENSION)) {
        // We are expecting only a single source jar to be extracted.
        extractSourceFolder(outputFolder, fileName, serviceClassName.toLowerCase());
        break;
      }

      if (monitor.isCanceled()) {
        throw new OperationCanceledException();
      }
    }
    monitor.worked(10);
    if (platformType != null) {
      removeUnwantedFiles(outputFolder, platformType);
    }
    monitor.worked(10);

    if (monitor.isCanceled()) {
      throw new OperationCanceledException();
    }
  }

  /**
   * Generates API config file contents given service class and application id.
   * Then uses it for generating discovery doc and finally client library zip,
   * which is unzipped and copied to outputFolder.
   */
  public void createSwarmApi(ArrayList<Class<?>> serviceClassList, IProject project,
      File outputFolder, ApiPlatformType platformType, boolean generateLibs, ClassLoader loader,
      SubMonitor monitor) throws IOException, IllegalArgumentException, SecurityException,
      IllegalAccessException, InvocationTargetException, NoSuchMethodException,
      ClassNotFoundException, CoreException, InstantiationException {

    monitor.subTask("Generating API Configuration File");
    Class<?> serviceContext = loader.loadClass("com.google.api.server.spi.ServiceContext");

    ArrayList<Object> methodArgs = new ArrayList<Object>();
    methodArgs.add(appId);
    methodArgs.add(DEFAULT_API_NAME);

    Method serviceContextCreateMethod = serviceContext.getMethod("create", String.class,
        String.class);

    Object serviceContextInstance = serviceContextCreateMethod.invoke(null, methodArgs.toArray());

    Class<?> annotationApiConfigGenerator = loader.loadClass("com.google.api.server.spi.tools.AnnotationApiConfigGenerator");
    Method generateConfigMethod = annotationApiConfigGenerator.getMethod("generateConfig",
        serviceContext, serviceClassList.toArray(new Class<?>[0]).getClass());

    @SuppressWarnings("unchecked")
    Map<String, String> apiString = (Map<String, String>) generateConfigMethod.invoke(
        annotationApiConfigGenerator.newInstance(), new Object[] {
            serviceContextInstance, serviceClassList.toArray(new Class<?>[0])});

    File apiConfigFile = null;
    for (String apiConfigKey : apiString.keySet()) {
      String apiConfig = apiString.get(apiConfigKey);
      String apiConfigName = apiConfigKey.substring(0,
          apiConfigKey.indexOf(SwarmServiceCreator.API_FILE_EXTENSION));
      apiConfigFile = SwarmServiceCreator.createConfigFile(project,
          WebAppUtilities.getWebInfSrc(project).getFile(apiConfigKey).getProjectRelativePath().toString(),
          new NullProgressMonitor());
      ResourceUtils.writeToFile(apiConfigFile, apiConfig);
      if (!generateLibs) {
        continue;
      }

      if (monitor.isCanceled()) {
        throw new OperationCanceledException();
      }

      monitor.worked(1);
      String clientLibPath = outputFolder.getAbsolutePath() + File.separator
          + SwarmServiceCreator.SWARM_LIB_NAME_PREFIX + apiConfigName;
      File clientLibFolder = new File(clientLibPath);
      clientLibFolder.mkdir();
      createClientLibFromApiConfig(project, apiConfig, clientLibFolder, monitor.newChild(1),
          platformType, apiConfigName, loader);
    }
  }

  public void removeUnwantedFiles(File targetFolder, ApiPlatformType platformType)
      throws CoreException {
    List<String> filesToRemoveList = new ArrayList<String>();

    // TODO: Should look for this via the descriptor file.
    filesToRemoveList.add("pom.xml");

    List<String> unneededDeps = ManagedApiUtils.computeDependenciesToRemove(apiDependencies,
        platformType);
    filesToRemoveList.addAll(unneededDeps);
    ResourceUtils.deleteFiles(targetFolder, filesToRemoveList);
  }

  private boolean extractSourceFolder(File folder, String fileName, String sourceFolderName)
      throws IllegalArgumentException, InvocationTargetException, CoreException, IOException {
    if (!folder.exists()) {
      return false;
    }
    for (File resource : folder.listFiles()) {
      if (resource.isDirectory()) {
        if (extractSourceFolder(resource, fileName, sourceFolderName)) {
          return true;
        }
      }
      if (!resource.getName().equals(fileName)) {
        continue;
      }
      File sourceFolder = new File(folder.getAbsolutePath(), sourceFolderName + "-"
          + SwarmServiceCreator.GENERATED_SOURCE_NAME_SUBSTRING);
      sourceFolder.mkdir();
      new UnzipRunnable(resource, sourceFolder).run(new NullProgressMonitor());
      return true;
    }
    return false;
  }

  private String generateAndWriteDiscovery(IProject project, String apiConfig,
      String fileNamePrefix, String format, ClassLoader loader) throws IOException, CoreException,
      IllegalArgumentException, SecurityException, IllegalAccessException,
      InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
    @SuppressWarnings({"unchecked", "rawtypes"})
    Class<Enum> formatEnum = (Class<Enum>) loader.loadClass("com.google.api.server.spi.tools.DiscoveryDocGenerator$Format");
    Class<?> discoveryDocGenerator = loader.loadClass("com.google.api.server.spi.tools.CloudDiscoveryDocGenerator");
    @SuppressWarnings("unchecked")
    String discoveryDoc = (String) discoveryDocGenerator.getMethod("generateDiscoveryDoc",
        String.class, formatEnum).invoke(
        discoveryDocGenerator.getMethod("using", String.class).invoke(null, DISCOVERY_API_ROOT),
        apiConfig, Enum.valueOf(formatEnum, format));
    String discoveryFileName = fileNamePrefix + (format.equals(REST) ? REST_SUFFIX : RPC_SUFFIX)
        + SwarmServiceCreator.DISCOVERY_FILE_EXTENSION;
    File discoveryFile = SwarmServiceCreator.createConfigFile(project, 
        WebAppUtilities.getWebInfSrc(project).getFile(discoveryFileName)
            .getProjectRelativePath().toString(),
        new NullProgressMonitor());
    ResourceUtils.writeToFile(discoveryFile, discoveryDoc);
    return discoveryDoc;
  }
}
