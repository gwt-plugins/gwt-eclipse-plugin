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

import com.google.appengine.eclipse.core.resources.GaeProject;
import com.google.gdt.eclipse.appengine.swarm.AppEngineSwarmPlugin;
import com.google.gdt.eclipse.appengine.swarm.util.ConnectedProjectHandler;
import com.google.gdt.eclipse.appengine.swarm.util.SwarmAnnotationUtils;
import com.google.gdt.eclipse.appengine.swarm.util.SwarmType;
import com.google.gdt.eclipse.appengine.swarm.util.XmlUtil;
import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.resources.ProjectResources;
import com.google.gdt.eclipse.managedapis.EclipseProject;
import com.google.gdt.eclipse.managedapis.ManagedApi;
import com.google.gdt.eclipse.managedapis.ManagedApiPlugin;
import com.google.gdt.eclipse.managedapis.ManagedApiUtils;
import com.google.gdt.eclipse.managedapis.impl.ApiPlatformType;
import com.google.gdt.eclipse.managedapis.impl.EclipseJavaProject;
import com.google.gdt.eclipse.managedapis.impl.ManagedApiImpl;
import com.google.gdt.eclipse.managedapis.impl.ProguardConfig;
import com.google.gdt.eclipse.managedapis.impl.ProguardState;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

/**
 * The main class responsible for generating Swarm service classes and
 * triggering lib creation / web.xml modifications.
 * 
 */
@SuppressWarnings("nls")
public class SwarmServiceCreator {

  /**
   * Helper class to track of ownerDomain and packagePath information for a
   * given Cloud Endpoint.
   */
  public static class EndpointPackageInfo {

    private final String ownerDomain;
    private final String packagePath;

    public EndpointPackageInfo(String ownerDomain, String packagePath) {
      this.ownerDomain = ownerDomain;
      this.packagePath = packagePath;
    }

    public String getOwnerDomain() {
      return ownerDomain;
    }

    public String getPackagePath() {
      return packagePath;
    }

    /**
     * Returns ownerDomain.packagePath, unless ownerDomain is empty. In that
     * case, just ownerDomain is returned.
     */
    @Override
    public String toString() {
      return (packagePath.length() == 0 ? ownerDomain : ownerDomain + "." + packagePath);
    }

  }

  public static final EndpointPackageInfo DEFAULT_PACKAGE_INFO = new EndpointPackageInfo(
      "mycompany.com", "services");

  public static final String DISCOVERY_FILE_EXTENSION = ".discovery";
  public static final String SERVICE_CLASS_SUFFIX = "Endpoint";
  // The downloaded client-lib JARs having source files will have the following
  // as substring of their filename.
  public static final String SOURCE_JAR_NAME_SUBSTRING = "source";
  public static final String JAR_FILE_EXTENSION = ".jar";

  public static final String SWARM_LIB_NAME_PREFIX = "lib";
  public static final String API_FILE_EXTENSION = ".api";
  private static final String META_INF_PATH = "src/META-INF/";
  public static final String GENERATED_SOURCE_NAME_SUBSTRING = "generated-source";

  
  /**
   * Given a root package, generates the Cloud Endpoint ownerDomain and
   * packagePath information.
   * 
   * If rootPackage is com.foo.bar, then the ownerDomain will be "foo.com", and
   * the packagePath will be "bar".
   * 
   * As another example, if the rootPackage is "com.foo.bar.baz", the
   * ownerDomain is still "foo.com", and the packgePath is "bar.baz".
   * 
   * In the case where the rootPackage has only two components, packagePath is
   * the empty string.
   * 
   * If rootPackage has less than two components, then
   * <code>DEFAULT_PACKAGE_INFO</code> will be returned.
   * 
   * @param rootPackage the root package. Cannot be null.
   * @return
   */
  public static EndpointPackageInfo getEndpointPackageInfo(String rootPackage) {

    assert (rootPackage != null);

    String[] packageComponents = rootPackage.split("\\.");

    String ownerDomain = "";
    String packagePath = "";

    if (packageComponents.length < 2) {
      return DEFAULT_PACKAGE_INFO;
    } else {
      ownerDomain = packageComponents[1] + "." + packageComponents[0];
      for (int i = 2; i < packageComponents.length; i++) {
        packagePath += packageComponents[i];
        if (i != packageComponents.length - 1) {
          packagePath += ".";
        }
      }
    }

    return new EndpointPackageInfo(ownerDomain, packagePath);
  }

  /**
   * Copies the generated libraries and dependencies from the App Engine project
   * to connected Android project. Also updated the android project's classpath
   * by removing all the previous files inside ApiLibs folder, deleting it,
   * copying from ApiLibs folder in App Engine project and then add the jars in
   * the copied folder.
   * 
   * @throws InvocationTargetException
   * @throws IllegalArgumentException
   */
  public static void copyToConnectedAndroidProject(File srcFolder, String folderName,
      IProject androidProject, SubMonitor monitor) throws CoreException, IOException,
      IllegalArgumentException, InvocationTargetException {
    IJavaProject androidJavaProject = JavaCore.create(androidProject);
    List<IClasspathEntry> rawClasspathList = new ArrayList<IClasspathEntry>();
    rawClasspathList.addAll(Arrays.asList(androidJavaProject.getRawClasspath()));
    File androidEndpointLibsFolder = new File(
        androidProject.getLocation().append(folderName).toOSString());
    File androidLibsFolder = new File(androidProject.getLocation().append("libs").toOSString());

    List<String> binaryDepsToadd = ManagedApiUtils.computeBinaryDepsToAdd(
        ManagedApiUtils.findAndReadDependencyFile(androidEndpointLibsFolder),
        ApiPlatformType.getAndroidPlatformType(androidProject));

    if (monitor.isCanceled()) {
      throw new OperationCanceledException();
    }

    // Remove existing jars
    updateAndroidProject(androidProject, androidEndpointLibsFolder, androidLibsFolder,
        rawClasspathList, binaryDepsToadd, false);

    if (monitor.isCanceled()) {
      throw new OperationCanceledException();
    }

    ResourceUtils.deleteFileRecursively(androidEndpointLibsFolder);

    if (monitor.isCanceled()) {
      throw new OperationCanceledException();
    }

    ResourceUtils.copyFolder(srcFolder, androidEndpointLibsFolder);

    if (monitor.isCanceled()) {
      throw new OperationCanceledException();
    }

    /*
     * Now that we've copied the updated endpoint information over to the
     * possibly newly-created endpoint libs folder, re-read the deps file.
     */
    binaryDepsToadd = ManagedApiUtils.computeBinaryDepsToAdd(
        ManagedApiUtils.findAndReadDependencyFile(androidEndpointLibsFolder),
        ApiPlatformType.getAndroidPlatformType(androidProject));

    // Now copy the appropriate jars over the endpoint libs folder
    updateAndroidProject(androidProject, androidEndpointLibsFolder, androidLibsFolder,
        rawClasspathList, binaryDepsToadd, true);

    if (monitor.isCanceled()) {
      throw new OperationCanceledException();
    }

    // Update the classpath
    androidJavaProject.setRawClasspath(
        rawClasspathList.toArray(new IClasspathEntry[rawClasspathList.size()]), monitor.newChild(1));
  }

  public static File createConfigFile(IProject project, String apiConfigFileName,
      IProgressMonitor monitor) throws CoreException, IOException {
    IFile f = project.getFile(apiConfigFileName);
    File apiConfigFile = project.getLocation().append(apiConfigFileName).toFile();
    if (f.exists()) {
      return apiConfigFile;
    }
    ResourceUtils.createFolderStructure(project, new Path(apiConfigFileName).removeLastSegments(1));
    apiConfigFile.createNewFile();
    return apiConfigFile;
  }

  /**
   * Given the name of an entity, return the name of the API that GPE will
   * generate if generating a Cloud Endpoint for the entity.
   */
  public static String getApiNameFromEntityName(String entityName) {
    return getServiceNameFromEntityName(entityName).toLowerCase();
  }

  /**
   * Given the name of an entity, return the name of the Endpoint class that GPE
   * will generate for it.
   */
  public static String getServiceNameFromEntityName(String entityName) {
    return entityName + SERVICE_CLASS_SUFFIX;
  }

  /**
   * Adds all the jar files in <code>androidEndpointLibsfolder</code>, given in
   * <code>libJarsList</code> to <code>androidLibsFolder</code> if
   * <code>addFiles</code> is true. If <code>addFiles</code> is false, then
   * removes all the jar files in <code>androidEndpointLibsfolder</code> from
   * the <code>androidLibsFolder</code>. Also, based on <code>addFiles</code>,
   * adds or removes source folder in <code>androidEndpointLibsfolder</code>
   * from <code>rawClasspathList</code>.
   * 
   * @throws InvocationTargetException
   * @throws IllegalArgumentException
   * @throws IOException
   */
  private static void updateAndroidProject(IProject project, File androidEndpointLibsfolder,
      File androidLibsFolder, List<IClasspathEntry> rawClasspathList, List<String> libJarsList,
      boolean addFiles) throws CoreException, IllegalArgumentException, InvocationTargetException,
      IOException {
    String projectFolderName = project.getLocation().lastSegment();
    if (!androidEndpointLibsfolder.exists()) {
      return;
    }
    // If its folder, call the updateClassPath recursively.
    // If its file, check if it is .jar and not a source JAR (which has "source"
    // in its name) before adding / removing from libs folder.
    for (File resource : androidEndpointLibsfolder.listFiles()) {
      if (resource.isDirectory()) {
        if (resource.getName().endsWith(GENERATED_SOURCE_NAME_SUBSTRING)) {
          if (!addFiles) {
            String workspaceRelativePath = resource.getAbsolutePath();
            workspaceRelativePath = workspaceRelativePath.replace(File.separator
                + projectFolderName + File.separator, File.separator + project.getName()
                + File.separator);
            workspaceRelativePath = workspaceRelativePath.substring(workspaceRelativePath.indexOf(File.separator
                + project.getName() + File.separator));
            IClasspathEntry entry = JavaCore.newSourceEntry(new Path(workspaceRelativePath));
            if (rawClasspathList.contains(entry)) {
              rawClasspathList.remove(entry);
            }
          } else {
            String workspaceRelativePath = resource.getAbsolutePath();
            workspaceRelativePath = workspaceRelativePath.replace(File.separator
                + projectFolderName + File.separator, File.separator + project.getName()
                + File.separator);
            workspaceRelativePath = workspaceRelativePath.substring(workspaceRelativePath.indexOf(File.separator
                + project.getName() + File.separator));
            IClasspathEntry entry = JavaCore.newSourceEntry(new Path(workspaceRelativePath));
            if (!rawClasspathList.contains(entry)) {
              rawClasspathList.add(entry);
            }
          }
        } else {
          updateAndroidProject(project, resource, androidLibsFolder, rawClasspathList, libJarsList,
              addFiles);
        }
        continue;
      } else if (libJarsList == null || !libJarsList.contains(resource.getName())) {
        continue;
      }
      if (libJarsList != null) {
        libJarsList.remove(resource.getName());
      }
      File libFile = new File(androidLibsFolder.getAbsolutePath() + File.separator
          + resource.getName());
      if (addFiles) {
        ResourceUtils.copyFile(resource, libFile);
      } else {
        if (libFile.exists()) {
          libFile.delete();
        }
      }
    }
  }

  private List<IType> entityList;
  private String appId;
  private IType current;
  private String primaryKeyType;
  private String factoryClassPath;
  private SwarmType swarmType;
  private IProject project;

  private IProject androidProject;

  private ClassLoader loader;

  private String idGetterName;

  public SwarmServiceCreator() {
  }

  /**
   * Checks if a class with the given classname exists in the project.
   * 
   * @throws JavaModelException
   */
  private boolean classExists(String className) throws JavaModelException {
    for (IPackageFragment pkgFragment : JavaCore.create(project).getPackageFragments()) {
      if (pkgFragment.getKind() != IPackageFragmentRoot.K_SOURCE) {
        continue;
      }
      for (ICompilationUnit cu : pkgFragment.getCompilationUnits()) {
        if (cu.getElementName().equals(className)) {
          factoryClassPath = pkgFragment.getElementName();
          return true;
        }
      }
    }
    return false;
  }

  public boolean create(boolean generateLibs, IProgressMonitor progressMonitor) {

    boolean taskComplete = false;
    try {
      XmlUtil xmlUtil = new XmlUtil();

      // create service for all the entities
      if (entityList != null) {
        for (IType anEntity : entityList) {
          current = anEntity;
          if (project == null) {
            project = current.getJavaProject().getProject();
          }

          swarmType = SwarmAnnotationUtils.getSwarmType(current);
          if (swarmType == SwarmType.ENTITY) {
            createPersistenceXmlFile(new NullProgressMonitor());
          } else if (swarmType == null) {
            continue;
          }

          if (swarmType != SwarmType.API) {
            primaryKeyType = SwarmAnnotationUtils.getPrimaryKeyType(current);
            idGetterName = SwarmAnnotationUtils.getId(current);
            createManagerFactoryClass(new NullProgressMonitor());
            createServiceClass(new NullProgressMonitor());
          }
        }
      }

      entityList = new ArrayList<IType>();
      for (IPackageFragment pkgFragment : JavaCore.create(project).getPackageFragments()) {
        if (pkgFragment.getKind() != IPackageFragmentRoot.K_SOURCE) {
          continue;
        }
        for (ICompilationUnit cu : pkgFragment.getCompilationUnits()) {
          SwarmAnnotationUtils.collectTypes(entityList, cu, SwarmType.API);
        }
      }
      final int entityListSize = entityList.size();
      deleteAllConfigs(API_FILE_EXTENSION);
      if (generateLibs) {
        deleteAllConfigs(DISCOVERY_FILE_EXTENSION);
      }
      if (entityListSize == 0) {
        xmlUtil.updateWebXml(new ArrayList<String>(), project);
        return true;
      }

      SubMonitor monitor = SubMonitor.convert(progressMonitor, "Generating Cloud Endpoint Library",
          100);
      // Just in case progressMonitor was a SubMonitor itself..
      monitor.subTask("Generating Cloud Endpoint Library");

      if (monitor.isCanceled()) {
        throw new OperationCanceledException();
      }

      androidProject = ConnectedProjectHandler.getConnectedProject(project);
      ApiPlatformType androidPlatformType = ApiPlatformType.getAndroidPlatformType(androidProject);

      initializeClassloader(new NullProgressMonitor());
      SwarmApiCreator apiCreator = new SwarmApiCreator(appId);
      ArrayList<Class<?>> serviceClassList = new ArrayList<Class<?>>();
      ArrayList<String> fullyQualifiedServiceClassList = new ArrayList<String>();
      for (IType anEntity : entityList) {
        current = anEntity;
        String serviceClassName = current.getElementName();
        try {
          serviceClassList.add(getClassObject(serviceClassName));
          fullyQualifiedServiceClassList.add(current.getFullyQualifiedName());
        } catch (ClassNotFoundException e) {
          // TODO(appu) : the call to getClassObject can trigger this exception,
          // it was in the old code and would prevent classes from being added
          // to the web.xml file. I haven't seen it manifest itself but maybe we
          // should trigger a refresh before getting to this section of code.
        }
      }
      xmlUtil.updateWebXml(fullyQualifiedServiceClassList, project);

      File outputFolder = ResourceUtils.createTempDir(".Web Api", "Temp Dir");
      outputFolder.deleteOnExit();

      if (monitor.isCanceled()) {
        throw new OperationCanceledException();
      }

      apiCreator.createSwarmApi(serviceClassList, project, outputFolder, androidPlatformType,
          generateLibs, loader, monitor.newChild(70));

      if (monitor.isCanceled()) {
        throw new OperationCanceledException();
      }

      if (generateLibs) {
        if (androidProject != null) {
          copyToConnectedAndroidProject(outputFolder, ManagedApiPlugin.SWARM_LIB_FOLDER_NAME,
              androidProject, monitor.newChild(10));
        } else {
          File appEngineFolder = createEmptySwarmLib(monitor.newChild(10));
          ResourceUtils.copyFolder(outputFolder, appEngineFolder);
        }
      }

      if (monitor.isCanceled()) {
        throw new OperationCanceledException();
      }

      // This is imperfect, but it's good enough..
      monitor.worked(2);

      if (androidProject != null) {
        xmlUtil.updateAndroidManifestXml(androidProject);
        androidProject.refreshLocal(IResource.DEPTH_INFINITE, monitor.newChild(1));
        if (generateLibs) {
          updateProguardInfo();
        }
      }

      if (monitor.isCanceled()) {
        throw new OperationCanceledException();
      }

      project.refreshLocal(IResource.DEPTH_INFINITE, monitor.newChild(1));
      taskComplete = true;
    } catch (JavaModelException e) {
      AppEngineSwarmPlugin.log(e);
    } catch (CoreException e) {
      AppEngineSwarmPlugin.log(e);
    } catch (MalformedTreeException e) {
      AppEngineSwarmPlugin.log(e);
    } catch (MalformedURLException e) {
      AppEngineSwarmPlugin.log(e);
    } catch (ClassNotFoundException e) {
      AppEngineSwarmPlugin.log(e);
    } catch (FileNotFoundException e) {
      AppEngineSwarmPlugin.log(e);
    } catch (ParserConfigurationException e) {
      AppEngineSwarmPlugin.log(e);
    } catch (SAXException e) {
      AppEngineSwarmPlugin.log(e);
    } catch (IOException e) {
      AppEngineSwarmPlugin.log(e);
    } catch (TransformerConfigurationException e) {
      AppEngineSwarmPlugin.log(e);
    } catch (TransformerFactoryConfigurationError e) {
      AppEngineSwarmPlugin.log(e);
    } catch (TransformerException e) {
      AppEngineSwarmPlugin.log(e);
    } catch (BadLocationException e) {
      AppEngineSwarmPlugin.log(e);
    } catch (IllegalArgumentException e) {
      AppEngineSwarmPlugin.log(e);
    } catch (SecurityException e) {
      AppEngineSwarmPlugin.log(e);
    } catch (InvocationTargetException e) {
      AppEngineSwarmPlugin.log(e);
    } catch (IllegalAccessException e) {
      AppEngineSwarmPlugin.log(e);
    } catch (NoSuchMethodException e) {
      AppEngineSwarmPlugin.log(e);
    } catch (InstantiationException e) {
      AppEngineSwarmPlugin.log(e);
    } finally {
      if (progressMonitor != null) {
        progressMonitor.done();
      }
    }
    return taskComplete;
  }

  private File createEmptySwarmLib(SubMonitor monitor) throws CoreException {
    IFolder f = project.getFolder(ManagedApiPlugin.SWARM_LIB_FOLDER_NAME);
    File libFolder = project.getLocation().append(ManagedApiPlugin.SWARM_LIB_FOLDER_NAME).toFile();
    if (f.exists()) {
      ResourceUtils.deleteFileRecursively(libFolder);
    }
    // Refresh Eclipse's IFolder so it knows the folder is deleted.
    f.refreshLocal(IResource.DEPTH_INFINITE, monitor.newChild(1));
    ResourceUtils.createFolderStructure(project, new Path(ManagedApiPlugin.SWARM_LIB_FOLDER_NAME));
    return libFolder;
  }

  private void createManagerFactoryClass(IProgressMonitor monitor) throws JavaModelException,
      IOException, MalformedTreeException, BadLocationException, CoreException {
    IPackageFragment pack = current.getPackageFragment();
    if (pack.getElementName().endsWith(".client") || pack.getElementName().endsWith(".server")
        || pack.getElementName().endsWith(".shared")) {
      IPath packPath = pack.getPath().removeLastSegments(1);
      pack = current.getJavaProject().findPackageFragment(packPath);
    }
    if (swarmType == SwarmType.ENTITY && classExists("EMF.java")
        || swarmType == SwarmType.PERSISTENCE_CAPABLE && classExists("PMF.java")) {
      return;
    }
    factoryClassPath = pack.getElementName();

    String className;
    String fileDataString = ResourceUtils.getResourceAsString(SwarmServiceCreator.class,
        "SwarmFactoryClass.java.template");
    fileDataString = fileDataString.replaceAll("@PackageName@", factoryClassPath);

    if (swarmType == SwarmType.ENTITY) {
      className = "EMF.java";
      fileDataString = fileDataString.replaceAll("@PackageName2@",
          "javax.persistence.EntityManagerFactory");
      fileDataString = fileDataString.replaceAll("@PackageName3@", "javax.persistence.Persistence");
      fileDataString = fileDataString.replaceAll("@SwarmType@", "Entity");
      fileDataString = fileDataString.replaceAll("@SwarmType2@", "EMF");
      fileDataString = fileDataString.replaceAll("@Instance@", "emfInstance");
      fileDataString = fileDataString.replaceAll("@FactoryCreate@",
          "Persistence.createEntityManagerFactory");

    } else {
      className = "PMF.java";
      fileDataString = fileDataString.replaceAll("@PackageName2@", "javax.jdo.JDOHelper");
      fileDataString = fileDataString.replaceAll("@PackageName3@",
          "javax.jdo.PersistenceManagerFactory");
      fileDataString = fileDataString.replaceAll("@SwarmType@", "Persistence");
      fileDataString = fileDataString.replaceAll("@SwarmType2@", "PMF");
      fileDataString = fileDataString.replaceAll("@Instance@", "pmfInstance");
      fileDataString = fileDataString.replaceAll("@FactoryCreate@",
          "JDOHelper.getPersistenceManagerFactory");
    }
    fileDataString = ProjectResources.reformatJavaSourceAsString(fileDataString);

    pack.createCompilationUnit(className, fileDataString, true, new SubProgressMonitor(monitor, 1));
    monitor.done();
  }

  private void createPersistenceXmlFile(IProgressMonitor monitor) throws CoreException, IOException {
    IProject project = current.getJavaProject().getProject();
    IFile f = project.getFile(META_INF_PATH + "persistence.xml");
    if (f.exists()) {
      return;
    }
    String contents = ResourceUtils.getResourceAsString(SwarmServiceCreator.class,
        "persistence.xml.template");
    try {
      ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
          contents.getBytes("UTF-8"));
      f.create(byteArrayInputStream, true, monitor);
    } catch (UnsupportedEncodingException e) {
      AppEngineSwarmPlugin.log(e);
    }
  }

  private void createServiceClass(IProgressMonitor monitor) throws CoreException, IOException,
      MalformedTreeException, BadLocationException {

    String entityName = current.getElementName();
    String serviceName = getServiceNameFromEntityName(entityName);

    String fileDataString;
    if (swarmType == SwarmType.ENTITY) {
      fileDataString = ResourceUtils.getResourceAsString(SwarmServiceCreator.class,
          "JpaSwarmService.java.template");
    } else {
      fileDataString = ResourceUtils.getResourceAsString(SwarmServiceCreator.class,
          "JdoSwarmService.java.template");
    }

    String packageName = current.getPackageFragment().getElementName();
    EndpointPackageInfo endpointPackageInfo = getEndpointPackageInfo(packageName);

    fileDataString = fileDataString.replaceAll("@AppIdLowerCase@", appId.toLowerCase());
    fileDataString = fileDataString.replaceAll("@AppId@", appId);
    fileDataString = fileDataString.replaceAll("@IdType@", primaryKeyType);
    fileDataString = fileDataString.replaceAll("@PackageName@", packageName);
    fileDataString = fileDataString.replaceAll("@PackageName2@", factoryClassPath);
    fileDataString = fileDataString.replaceAll("@ServiceName@", serviceName);
    fileDataString = fileDataString.replaceAll("@ApiName@", getApiNameFromEntityName(entityName));
    fileDataString = fileDataString.replaceAll("@EndpointOwnerDomain@",
        endpointPackageInfo.getOwnerDomain());
    fileDataString = fileDataString.replaceAll("@EndpointPackagePath@",
        endpointPackageInfo.getPackagePath());
    fileDataString = fileDataString.replaceAll("@EntityName@", entityName);
    fileDataString = fileDataString.replaceAll("@EntityNameLowerCase@",
        current.getElementName().toLowerCase());
    fileDataString = fileDataString.replaceAll("@GetId@", idGetterName);

    fileDataString = ProjectResources.reformatJavaSourceAsString(fileDataString);
    current.getPackageFragment().createCompilationUnit(serviceName + ".java", fileDataString, true,
        new SubProgressMonitor(monitor, 1));
    monitor.done();
  }

  /**
   * Deletes all the Api / Discovery Configs in the project.
   */
  public void deleteAllConfigs(String fileExtension) throws CoreException {
    IPath webInfPath = WebAppUtilities.getWebInfPath(project);
    if (webInfPath == null || !webInfPath.toFile().exists() || !webInfPath.toFile().isDirectory()) {
      throw new CoreException(new Status(IStatus.ERROR, AppEngineSwarmPlugin.PLUGIN_ID,
          "Can't find WEB-INF directory"));
    }
    File apiConfigFolder = webInfPath.toFile();
    for (File file : apiConfigFolder.listFiles()) {
      if (file.isFile() && file.getName().endsWith(fileExtension)) {
        file.delete();
      }
    }
  }

  private Class<?> getClassObject(String serviceClassName) throws ClassNotFoundException {
    return loader.loadClass(current.getPackageFragment().getElementName() + "." + serviceClassName);
  }

  private void initializeClassloader(IProgressMonitor monitor) throws CoreException,
      MalformedURLException, FileNotFoundException {
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);

    IJavaProject javaProject = JavaCore.create(project);
    // using getOutputFolderPath instead of getWarOutLocation to just get the default
    // output location from the java project instead of the configuration file style thing
    // that getWarOutLocation uses, maybe that's okay too though.
    IPath binRootPath = WebAppUtilities.getOutputFolderPath(javaProject);
    if (binRootPath == null || !binRootPath.toFile().exists()) {
      throw new CoreException(new Status(IStatus.ERROR, AppEngineSwarmPlugin.PLUGIN_ID,
          "failed to find output folder"));
    }
    File binRoot = binRootPath.toFile();
    
    
    IClasspathEntry[] entries = javaProject.getResolvedClasspath(false);
    // list will not be any larger than entries.length + 2
    List<URL> classpathUrlList = new ArrayList<URL>(entries.length + 2);
    classpathUrlList.add(binRoot.toURI().toURL());
    for (IClasspathEntry e : entries) {
      if (e.getContentKind() != IPackageFragmentRoot.K_BINARY) {
        continue;
      }
      String jarLocation = e.getPath().toString();
      if (jarLocation.startsWith("/" + project.getName())) {
        jarLocation = project.getLocation().toString()
            + jarLocation.substring(("/" + project.getName()).length());
      }
      File classPathEntry = new File(jarLocation.replace("/", File.separator));
      classpathUrlList.add(classPathEntry.toURI().toURL());
    }
    GaeProject gaeProject = GaeProject.create(project);
    IPath gaeInstallationPath = gaeProject.getSdk().getInstallationPath();
    //Todo(appu): expose via sdkinfo class
    classpathUrlList.add(gaeInstallationPath.append(
        "lib/opt/tools/appengine-local-endpoints/v1/appengine-local-endpoints.jar").toFile().toURI().toURL());
    classpathUrlList.add(gaeInstallationPath.append(
        "lib/opt/user/appengine-endpoints/v1/appengine-endpoints.jar").toFile().toURI().toURL());
    loader = new URLClassLoader(classpathUrlList.toArray(new URL[classpathUrlList.size()]));
  }

  public boolean serviceExists(String serviceName, IPackageFragment pack) {
    if (!pack.exists()) {
      return false;
    }
    ICompilationUnit serviceUnit = pack.getCompilationUnit(serviceName + ".java");
    return serviceUnit.exists();
  }

  public void setAppId(String applicationId) {
    appId = applicationId;
  }

  public void setEntities(Iterable<IType> entityTypes) {
    entityList = (List<IType>) entityTypes;
  }

  public void setProject(IProject project) {
    this.project = project;
  }

  private void updateProguardInfo() throws CoreException, IOException {
    if (androidProject == null) {
      return;
    }

    IFolder swarmLibFolder = androidProject.getFolder(ManagedApiPlugin.SWARM_LIB_FOLDER_NAME);
    ProguardState beforeState = ManagedApiUtils.generateCurrentProguardState(androidProject,
        swarmLibFolder);

    EclipseProject ep = new EclipseJavaProject(JavaCore.create(androidProject));
    ManagedApi endpointLibsApi = ManagedApiImpl.createManagedApi(ep, swarmLibFolder);
    ProguardConfig endpointLibsProguardConfig = new ProguardConfig(endpointLibsApi, ep);
    if (endpointLibsProguardConfig.hasProguardConfig()) {
      ProguardState futureState = ProguardState.createForFuture(beforeState,
          endpointLibsProguardConfig, ep);
      futureState.apply();
    }
  }
}
