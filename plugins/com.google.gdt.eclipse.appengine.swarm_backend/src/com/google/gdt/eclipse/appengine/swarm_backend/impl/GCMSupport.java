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

package com.google.gdt.eclipse.appengine.swarm_backend.impl;

import com.google.gdt.eclipse.appengine.swarm.util.XmlUtil;
import com.google.gdt.eclipse.appengine.swarm.wizards.helpers.SwarmServiceCreator;
import com.google.gdt.eclipse.appengine.swarm.wizards.helpers.SwarmServiceCreator.EndpointPackageInfo;
import com.google.gdt.eclipse.appengine.swarm_backend.AppEngineSwarmBackendPlugin;
import com.google.gdt.eclipse.appengine.swarm_backend.resources.GenerationUtils;
import com.google.gdt.eclipse.core.DynamicWebProjectUtilities;
import com.google.gdt.eclipse.core.properties.WebAppProjectProperties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

/**
 * This class adds GCM support to the android and its connected app engine
 * project.
 */
public class GCMSupport {
  private static final String GCM_SERVER_JAR = "gcm-server.jar";

  private static final String GCM_JAR = "gcm.jar";

  // TODO(rdayal): Is this directory always the the libs directory for Android,
  // or is it changeable?
  private static final String LIBS_DIR = "libs/";
  private static final String JSON_SIMPLE_LIB = "json_simple-1.1.jar";

  // This matches the name of the API in MessageEndpoint.java.template
  private static final String MESSAGE_ENDPOINT_API_NAME = "messageEndpoint";

  private static final List<String> APP_ENGINE_TEMPLATES = Arrays.asList("DeviceInfo",
      "MessageData", "MessageEndpoint");
  // TODO(appu): HACK come up with better workaround, this specific array is
  // used in BackendGenerator.java to skip including an entity in endpoint
  // generation
  protected static final List<String> APP_ENGINE_ENTITIES_TO_SKIP_ENDPOINT_GENERATION = Arrays.asList("MessageData");

  private final String apiKey;
  private final String projectNumber;

  private IProject androidProject;
  private IProject appEngineProject;
  private IJavaProject androidJavaProject;
  private IJavaProject appEngineJavaProject;
  private IPath webInfLibPath;
  private IPath webContentPath;

  public GCMSupport(IProject androidProject, IProject appEngineProject, String projectNumber,
      String apiKey) throws CoreException {
    this.androidProject = androidProject;
    this.appEngineProject = appEngineProject;
    androidJavaProject = JavaCore.create(androidProject);
    appEngineJavaProject = JavaCore.create(appEngineProject);
    this.apiKey = (apiKey == null) ? "" : apiKey;
    this.projectNumber = (projectNumber == null) ? "" : projectNumber;
    if (DynamicWebProjectUtilities.isDynamicWebProject(appEngineProject)) {
      // defaults to 'WebContent'
      webContentPath = DynamicWebProjectUtilities.getWebContentFolder(appEngineProject);
    } else {
      // defaults to 'war'
      webContentPath = WebAppProjectProperties.getWarSrcDir(appEngineProject);
    }
    webInfLibPath = webContentPath.append("WEB-INF").append("lib");
  }

  /**
   * Adds GCM support to the Android half of the connected projects.
   *
   * Note that the Android project will need to be refreshed after this call, as
   * some of the resources are created outside of the Eclipse workspace.
   */
  public void addAndroidSupport(String androidPackageName, SubMonitor monitor) throws IOException,
      CoreException, ParserConfigurationException, SAXException,
      TransformerFactoryConfigurationError, TransformerException, InterruptedException {

    monitor.subTask("Adding GCM Libraries and Sample Code to Android project");

    addAndroidGCMJar(androidJavaProject);

    monitor.worked(10);
    if (monitor.isCanceled()) {
      throw new OperationCanceledException();
    }

    addAndroidTemplates(androidPackageName);

    monitor.worked(10);
    if (monitor.isCanceled()) {
      throw new OperationCanceledException();
    }

    new XmlUtil().updateAndroidManifestXmlForGCM(androidProject);

    if (monitor.isCanceled()) {
      throw new OperationCanceledException();
    }

    if (monitor.isCanceled()) {
      throw new OperationCanceledException();
    }
  }

  /**
   * Adds GCM support to the App Engine half of the connected projects.
   *
   * Note that the App Engine project will have to be refreshed after this call.
   */
  public void addAppEngineSupport(SubMonitor monitor, String appEnginePackageName)
      throws IOException, CoreException {

    monitor.subTask("Adding GCM Libraries and Sample Code to App Engine project");

    addAppEngineGCMJar(appEngineJavaProject);

    monitor.worked(10);
    if (monitor.isCanceled()) {
      throw new OperationCanceledException();
    }

    EndpointPackageInfo endpointPackageInfo = SwarmServiceCreator.getEndpointPackageInfo(appEnginePackageName);

    addAppEngineTemplates(appEnginePackageName, endpointPackageInfo);
  }

  void createWebContentFile(String fileName) throws IOException {
    InputStream inputstream = GenerationUtils.getResource(fileName);
    File file = new File(
        appEngineProject.getLocation().append(webContentPath).append(fileName).toOSString());
    GenerationUtils.createFile(file, inputstream);
  }

  void createWebContentFile(String fileName, String subdirName) throws IOException {
    InputStream inputstream = GenerationUtils.getResource(fileName);
    File subdir = new File(appEngineProject.getLocation().append(webContentPath).append(
        subdirName).toOSString());
    subdir.mkdirs();
    File file = new File(subdir, fileName);
    GenerationUtils.createFile(file, inputstream);
  }

  /**
   * Copy gcm.jar to Android project.
   */
  private void addAndroidGCMJar(IJavaProject androidJavaProject) throws IOException {
    InputStream gcmJar = GenerationUtils.getResource(GCM_JAR);
    File gcmLibFile = new File(androidProject.getLocation().append(LIBS_DIR + GCM_JAR).toOSString());
    GenerationUtils.createFile(gcmLibFile, gcmJar);
  }

  /**
   * Adds GCM templates to Android project.
   */
  private void addAndroidTemplates(String androidProjPackageName) throws IOException, CoreException {
    IPackageFragment pkgFragment = GenerationUtils.findpackageFragment(androidJavaProject,
        androidProjPackageName);
    if (pkgFragment == null) {
      for (IPackageFragmentRoot root : androidJavaProject.getAllPackageFragmentRoots()) {
        if (GenerationUtils.SRC_PACKAGE_FRAGMENT_ROOT.equals(root.getElementName())) {
          pkgFragment = root.createPackageFragment(androidProjPackageName, true,
              new NullProgressMonitor());
          break;
        }
      }
    }

    // Create Class GCMIntentService.java
    String gcmClassName = "GCMIntentService.java.template";
    String gcmReceiver = GenerationUtils.getResourceAsString(gcmClassName);
    gcmReceiver = gcmReceiver.replace("@PackageName@", androidProjPackageName);

    // We need to figure out what API name GPE generated in the endpoint class
    // that was created for
    // for the DeviceInfo entity
    String deviceInfoApiName = SwarmServiceCreator.getApiNameFromEntityName("DeviceInfo");
    gcmReceiver = gcmReceiver.replace("@Deviceinfoendpoint@",
        getImportPathForClass("Deviceinfoendpoint", deviceInfoApiName));
    gcmReceiver = gcmReceiver.replace("@DeviceInfo@",
        getImportPathForClass("DeviceInfo", deviceInfoApiName));
    gcmReceiver = gcmReceiver.replace("@ProjectNumber@", projectNumber);

    pkgFragment.createCompilationUnit("GCMIntentService.java", gcmReceiver, true,
        new NullProgressMonitor());

    // Create Class CloudEndpointUtils.java
    String cloudEndpointUtilsClassName = "CloudEndpointUtils.java.template";
    String cloudEndpointUtils = GenerationUtils.getResourceAsString(cloudEndpointUtilsClassName);
    cloudEndpointUtils = cloudEndpointUtils.replace("@PackageName@", androidProjPackageName);
    pkgFragment.createCompilationUnit("CloudEndpointUtils.java", cloudEndpointUtils, true,
        new NullProgressMonitor());

    // Create Class RegisterActivity.java
    String registerActivityContents = GenerationUtils.getResourceAsString("RegisterActivity.java.template");
    registerActivityContents = registerActivityContents.replace("@PackageName@",
        androidProjPackageName);

    registerActivityContents = registerActivityContents.replace("@MessageEndpoint@",
        getImportPathForClass("MessageEndpoint", MESSAGE_ENDPOINT_API_NAME));
    registerActivityContents = registerActivityContents.replace("@CollectionResponseMessageData@",
        getImportPathForClass("CollectionResponseMessageData", MESSAGE_ENDPOINT_API_NAME));
    registerActivityContents = registerActivityContents.replace("@MessageData@",
        getImportPathForClass("MessageData", MESSAGE_ENDPOINT_API_NAME));
    pkgFragment.createCompilationUnit("RegisterActivity.java", registerActivityContents, true,
        new NullProgressMonitor());

    // Add layout file for RegisterActivity
    // TODO(rdayal): Refactor; quite similar to what we have in
    // AndroidProjectCreator.
    IFolder layoutfolder = androidProject.getFolder("res").getFolder("layout");
    IFile layoutFile = layoutfolder.getFile("activity_register.xml");
    if (!layoutFile.exists()) {
      InputStream inputstream = GenerationUtils.getResource("activity_register.xml.layout");
      GenerationUtils.createFile(layoutFile.getLocation().toFile(), inputstream);
    }

  }

  /**
   * Copy gcm-server.jar to App Engine project's WEB-INF/lib and add to its classpath.
   */
  private void addAppEngineGCMJar(IJavaProject appEngineJavaProject) throws IOException,
      JavaModelException {
    InputStream gcmServerJar = GenerationUtils.getResource(GCM_SERVER_JAR);
    File gcmServerLibFile = new File(appEngineProject.getLocation().append(webInfLibPath).append(
        GCM_SERVER_JAR).toOSString());
    GenerationUtils.createFile(gcmServerLibFile, gcmServerJar);
    IClasspathEntry[] appEngineEntries = appEngineJavaProject.getRawClasspath();
    List<IClasspathEntry> appEngineEntriesList = new ArrayList<IClasspathEntry>(
        Arrays.asList(appEngineEntries));
    String gcsServerFileName = webInfLibPath.append(GCM_SERVER_JAR).toOSString();
    appEngineEntriesList.add(JavaCore.newLibraryEntry(
        appEngineProject.getFile(gcsServerFileName).getFullPath(), null, null));
    appEngineJavaProject.setRawClasspath(appEngineEntriesList.toArray(new IClasspathEntry[0]), null);

    // Needed on the runtime classpath; it's a dependency of gcm-server.jar
    InputStream jsonSimpleLibJar = GenerationUtils.getResource(JSON_SIMPLE_LIB);
    File jsonSimpleLibJarFile = new File(
        appEngineProject.getLocation().append(webInfLibPath).append(JSON_SIMPLE_LIB).toOSString());
    GenerationUtils.createFile(jsonSimpleLibJarFile, jsonSimpleLibJar);
  }

  /**
   * Adds GCM templates to App Engine project.
   */
  private void addAppEngineTemplates(String packageName, EndpointPackageInfo endpointPackageInfo)
      throws IOException, CoreException {
    IPackageFragment pkgFragment = GenerationUtils.findpackageFragment(appEngineJavaProject,
        packageName);

    for (String template : APP_ENGINE_TEMPLATES) {
      String javaFile = template + ".java";
      String templateFile = javaFile + ".template";
      String fileData = GenerationUtils.getResourceAsString(templateFile);
      fileData = fileData.replace("@PackageName@", packageName);
      // if we're working with MessageEndpoint we have to inject the API key
      if (template.equals("MessageEndpoint")) {
        fileData = fileData.replace("@ApiKey@", apiKey);

        fileData = fileData.replace("@EndpointOwnerDomain@", endpointPackageInfo.getOwnerDomain());
        fileData = fileData.replace("@EndpointPackagePath@", endpointPackageInfo.getPackagePath());
      }
      pkgFragment.createCompilationUnit(javaFile, fileData, true, new NullProgressMonitor());
    }

    // Create index.html
    createWebContentFile("index.html");

    // Add jQuery
    createWebContentFile("jquery-1.9.0.min.js", "js");

    // Add bootstrap files
    createWebContentFile("bootstrap.min.css", "css");
    createWebContentFile("bootstrap.min.js", "js");
    createWebContentFile("glyphicons-halflings-white.png", "img");
    createWebContentFile("glyphicons-halflings.png", "img");
  }

  private String getImportPathForClass(String className, String apiName) {

    // default if we have no idea what it is
    final String defaultImportPath = apiName + '.' + className;

    try {
      for (IPackageFragmentRoot packageFramentRoot : androidJavaProject.getPackageFragmentRoots()) {

        if (packageFramentRoot.getPath() != null
            && packageFramentRoot.getPath().toPortableString().contains(apiName)) {

          for (IJavaElement child : packageFramentRoot.getChildren()) {
            if (child.getElementType() != IJavaElement.PACKAGE_FRAGMENT) {
              continue;
            }

            IPackageFragment curFragment = (IPackageFragment) child;
            ICompilationUnit cu = curFragment.getCompilationUnit(className + ".java");
            if (cu.exists()) {
              return curFragment.getElementName() + '.' + className;
            }
          }
        }
      }
    } catch (JavaModelException e) {
      AppEngineSwarmBackendPlugin.log(e);
    }

    return defaultImportPath;
  }
}
