/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.appengine.eclipse.wtp.maven;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.embedder.MavenImpl;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.repository.RepositoryPolicy;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import com.google.appengine.eclipse.core.preferences.GaePreferences;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.gdt.eclipse.core.sdk.SdkManager;
import com.google.gdt.eclipse.core.sdk.SdkSet;
import com.google.gdt.eclipse.core.sdk.SdkUtils;

/**
 * Provides a method to obtain a {@link GaeSdk} corresponding to the appengine-maven-plugin version
 * specified in a POM, pointing to a GAE SDK in the local Maven repository.
 */
@SuppressWarnings("restriction") // MavenImpl and MavenImpl.getPlexusContainer()
public class GaeSdkInstaller {

  private static final String APPENGINE_MAVEN_PLUGIN_KEY =
      "com.google.appengine:appengine-maven-plugin";
  
  private static final String MIN_APPENGINE_MAVEN_PLUGIN_VERSION = "1.8.4";
  
  /**
   * Template for the path of the highest-level directory within the local Maven repository
   * designated for a particular version of the GAE Java SDK. The first {@code %s} is to be replaced
   * by an absolute path to the local Maven repository and the second {@code %s} is to be replaced
   * by the version number (e.g., "1.8.9"). When a new SDK version is fetched from a remote
   * repository, its zip file is placed in this directory.
   */
  private static final String SDK_VERSION_PATH_TEMPLATE =
      "%s/com/google/appengine/appengine-java-sdk/%s";
  
  /**
   * Template for the root of the actual GAE Java SDK within the local Maven repository. The first
   * {@code %s} is to be replaced by a substituted instance of {@link SDK_VERSION_PATH_TEMPLATE} and
   * the second {@code %s} is to be replaced by the version number (e.g., "1.8.9"). When a new SDK
   * version is fetched from a remote repository, its zip file is unzipped into this directory
   * (which then contains subdirectories like {@code src}, {@code bin}, and {@code lib} and files
   * like {@code ABOUT} and {@code README}).
   */
  private static final String SDK_PATH_TEMPLATE = "%s/appengine-java-sdk/appengine-java-sdk-%s";

  private static final String MAVEN_GAE_GROUP_ID = "com.google.appengine";
  private static final String MAVEN_GAE_JAVA_SDK_ARTIFACT_ID = "appengine-java-sdk";
  private static final String MAVEN_GAE_JAVA_SDK_ARTIFACT_EXTENSION = "zip";
  
  private final RepositorySystem repositorySystem; 

  public GaeSdkInstaller() {
    MavenImpl maven = (MavenImpl) MavenPlugin.getMaven();
    RepositorySystem newRepoSystem;
    try {
      newRepoSystem = maven.getPlexusContainer().lookup(RepositorySystem.class);
    } catch (ComponentLookupException | CoreException e) {
      AppEngineMavenPlugin.logError("Error using Plexus to construct ArtifactDescriptorReader", e);
      newRepoSystem = null;
    }
    repositorySystem = newRepoSystem;
  }
  
  /**
   * Ensures that an appropriate {@link GaeSdk} for a specified Maven POM is registered with GPE as
   * the default GAE SDK, and returns it. The {@code GaeSdk} corresponds to a GAE SDK with the same
   * version as the appengine-maven-plugin version specified in the POM plugins, installed in the
   * local Maven repository. If there is not already a GAE SDK of the required version installed in
   * the local Maven repository, it is fetched from a remote repository.
   * 
   * <p>Local and remote repository locations are obtained from the {@link MavenPlugin}.
   * 
   * @param pom the specified Maven POM
   * @return the {@code GaeSdk}
   * @throws CoreException if there is an error registering the {@code GaeSdk} with GPE
   */
  public GaeSdk installGaeSdkIfNeeded(Model pom) throws CoreException {
    String sdkVersion = getAppengineMavenPluginVersion(pom);
    SdkManager<GaeSdk> sdkManager = GaePreferences.getSdkManager();
    SdkSet<GaeSdk> sdks = sdkManager.getSdks();
    GaeSdk requiredSdk = findSdk(sdkVersion, sdks);
    sdks.setDefault(requiredSdk);
    sdkManager.setSdks(sdks);
    return requiredSdk;
  }
  
  /**
   * Obtains a {@link GaeSdk} object for a specified version of the GAE SDK, installed in the local
   * Maven repository, preferably from among the {@code GaeSdk} objects contained in a specified
   * {@link SdkSet}. If the specified version of the GAE SDK is not already present in the expected
   * location in the local Maven repository, it is fetched from a remote repository and installed
   * there. If a {@code GaeSdk} for an SDK in that location is not found in the specified
   * {@code SdkSet}, a new {@code GaeSdk} is created and added to that {@code SdkSet}.
   * 
   * @param sdkVersion the specified GAE SDK version
   * @param sdks the specified {@code SdkSet}
   * @return
   *     the {@code GaeSdk} for the specified version, corresponding to a GAE SDK installed in the
   *     local Maven repository
   * @throws CoreException if a new {@code GaeSdk} is created and it fails validation
   */
  private GaeSdk findSdk(String sdkVersion, SdkSet<GaeSdk> sdks) throws CoreException {
    String sdkPathString = mavenRepositorySdkPath(sdkVersion); // may fetch SDK from remote repo
    IPath sdkInstallationPath = new Path(sdkPathString);
    for (GaeSdk sdk : sdks) {
      if (sdkInstallationPath.equals(sdk.getInstallationPath())) {
        return sdk;
      }
    }
    String uniqueName = SdkUtils.generateUniqueSdkNameFrom("App Engine " + sdkVersion, sdks);
    GaeSdk newSdk = GaeSdk.getFactory().newInstance(uniqueName, sdkInstallationPath);
    IStatus status = newSdk.validate();
    if (!status.isOK()) {
      throw new CoreException(status);
    }
    sdks.add(newSdk);
    return newSdk;
  }
  
  private String getAppengineMavenPluginVersion(Model pom) {
    Plugin appengineMavenPlugin =
        pom.getBuild().getPluginsAsMap().get(APPENGINE_MAVEN_PLUGIN_KEY);
    if (appengineMavenPlugin == null) {
      AppEngineMavenPlugin.logInfo(
          "The <plugins> section of pom.xml has no entry for appengine-maven-plugin. "
              + "Using GAE SDK verion " + MIN_APPENGINE_MAVEN_PLUGIN_VERSION);
      return MIN_APPENGINE_MAVEN_PLUGIN_VERSION;
    } else {
      return appengineMavenPlugin.getVersion();
    }
  }
  
  /**
   * Ensures that a GAE SDK with a given version is installed in the local Maven repository, and
   * returns its path. If the GAE SDK was not previously installed in the local Maven repository,
   * it is fetched from a remote repository and installed locally. 
   * 
   * @param version the given version
   * @return the path, or {@code null} if an error is encountered
   */
  private String mavenRepositorySdkPath(String version) {
    try {
      IMaven maven = MavenPlugin.getMaven();
      String localRepositoryRoot = maven.getLocalRepository().getBasedir() + '/';
      String sdkVersionPath =
          String.format(SDK_VERSION_PATH_TEMPLATE, localRepositoryRoot, version);
      String sdkPath = String.format(SDK_PATH_TEMPLATE, sdkVersionPath, version);
      File sdkDirectory = new File(sdkPath);
      if (!sdkDirectory.exists()) {
        fetchSdkFromRemoteRepository(
            version, toAetherRepositoryList(maven.getArtifactRepositories()));
        unzip(sdkVersionPath, version);
      }
      return sdkPath;
    } catch (CoreException e) {
      return null;
    }
  }
  
  private void fetchSdkFromRemoteRepository(String version, List<RemoteRepository> repositories) {
    RepositorySystemSession session =
        MavenPlugin.getMaven()
            .getExecutionContext().newProjectBuildingRequest().getRepositorySession();
    ArtifactRequest request =
        new ArtifactRequest(
            new DefaultArtifact(
                MAVEN_GAE_GROUP_ID,
                MAVEN_GAE_JAVA_SDK_ARTIFACT_ID,
                MAVEN_GAE_JAVA_SDK_ARTIFACT_EXTENSION,
                version),
            repositories,
            null);
    try {
      repositorySystem.resolveArtifact(session, request);
    } catch (ArtifactResolutionException e) {
      AppEngineMavenPlugin.logError("Error downloading SDK from remote repository", e);
    }
  }
  
  /**
   * Converts a list of remote-repository specifications from their representation in the
   * {@code org.apache.maven} framework (as reported by {@link IMaven.getArtifactRepositories()})
   * to their representation in the {@code org.sonatype.aether} framework (as used in an
   * {@link ArtifactRequest} submitted to
   * {@link RepositorySystem.resolveArtifact(RepositorySystemSession, ArtifactRequest)}.
   * 
   * @param allRepositories
   *     a list of {@code org.apache.maven.artifact.repository.ArtifactRepository} objects
   * @return a list of corresponding {@code org.sonatype.aether.repository.RemoteRepository} objects
   */
  private static List<RemoteRepository> toAetherRepositoryList(
      List<ArtifactRepository> allRepositories) {
    List<RemoteRepository> result = Lists.newArrayListWithCapacity(allRepositories.size());
    for (ArtifactRepository repository : allRepositories) {
      result.add(toAetherRepository(repository));
    }
    return result;
  }
  
  private static RemoteRepository toAetherRepository(ArtifactRepository repository) {
    RemoteRepository result = new RemoteRepository();
    result.setId(repository.getId());
    result.setContentType("default");
    result.setUrl(repository.getUrl());
    result.setPolicy(true, toAetherRepositoryPolicy(repository.getSnapshots()));
    result.setPolicy(false, toAetherRepositoryPolicy(repository.getReleases()));
    result.setProxy(toAetherProxy(repository.getProxy()));
    result.setAuthentication(toAetherAuthentication(repository.getAuthentication()));
    result.setMirroredRepositories(toAetherRepositoryList(repository.getMirroredRepositories()));
    result.setRepositoryManager(false);
    return result;
  }
  
  private static RepositoryPolicy toAetherRepositoryPolicy(ArtifactRepositoryPolicy policy) {
    return 
        policy == null ?
            null
            : new RepositoryPolicy(
                policy.isEnabled(), policy.getUpdatePolicy(), policy.getChecksumPolicy());
  }
  
  private static org.sonatype.aether.repository.Proxy toAetherProxy(
      org.apache.maven.repository.Proxy mavenProxy) {
    return
        mavenProxy == null ?
            null
            : new org.sonatype.aether.repository.Proxy(
                mavenProxy.getProtocol(),
                mavenProxy.getHost(),
                mavenProxy.getPort(),
                new org.sonatype.aether.repository.Authentication(
                    mavenProxy.getUserName(), mavenProxy.getPassword()));
  }
  
  private static org.sonatype.aether.repository.Authentication toAetherAuthentication(
      org.apache.maven.artifact.repository.Authentication authentication) {
    return
        authentication == null ?
            null
            : new org.sonatype.aether.repository.Authentication(
                authentication.getUsername(),
                authentication.getPassword(),
                authentication.getPrivateKey(),
                authentication.getPassphrase());
  }
  
  private static void unzip(String sdkVersionPath, String version) {
    String zipFilePath = sdkVersionPath + "/appengine-java-sdk-" + version + ".zip";
    String destinationPathString = sdkVersionPath + "/appengine-java-sdk";
    try {
      
      try (ZipFile zipFile = new ZipFile(zipFilePath)) {
        Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
        while (zipEntries.hasMoreElements()) {
          ZipEntry zipEntry = zipEntries.nextElement();
          if (!zipEntry.isDirectory()) {
            File zipEntryDestination = new File(destinationPathString, zipEntry.getName());
            if (!zipEntryDestination.exists()) {
              Files.createParentDirs(zipEntryDestination);
              byte[] unzippedBytes = ByteStreams.toByteArray(zipFile.getInputStream(zipEntry));
              Files.write(unzippedBytes, zipEntryDestination);
            }
          }
        }
      }
      
    } catch (IOException e) {      
      AppEngineMavenPlugin.logError("Error unzipping downloaded SDK archive " + zipFilePath, e);
    }
  }

}
