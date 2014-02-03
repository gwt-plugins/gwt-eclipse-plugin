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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.embedder.MavenImpl;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
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

@SuppressWarnings("restriction") // MavenImpl and MavenImpl.getPlexusContainer()
public class GaeSdkInstaller {
  
  private static final String APPENGINE_MAVEN_PLUGIN_KEY =
      "com.google.appengine:appengine-maven-plugin";
  
  private static final String MIN_APPENGINE_MAVEN_PLUGIN_VERSION = "1.8.4";
  
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
  
  public GaeSdk installGaeSdkIfNeeded(Model pom, IProgressMonitor monitor) throws CoreException {
    String sdkVersion = getAppengineMavenPluginVersion(pom);
    SdkManager<GaeSdk> sdkManager = GaePreferences.getSdkManager();
    SdkSet<GaeSdk> sdks = sdkManager.getSdks();
    GaeSdk requiredSdk = findSdk(sdkVersion, sdks);
    sdks.setDefault(requiredSdk);
    sdkManager.setSdks(sdks);
    return requiredSdk;
  }
  
  private GaeSdk findSdk(String sdkVersion, SdkSet<GaeSdk> sdks) throws CoreException {
    String sdkPathString = mavenRepositorySdkPath(sdkVersion);
    IPath sdkInstallationPath = new Path(sdkPathString);
    for (GaeSdk sdk : sdks) {
      if (sdkInstallationPath.equals(sdk.getInstallationPath())) {
        return sdk;
      }
    }
    String uniqueName = SdkUtils.generateUniqueSdkNameFrom("App Engine", sdks);
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
    return
        appengineMavenPlugin == null ?
            MIN_APPENGINE_MAVEN_PLUGIN_VERSION : appengineMavenPlugin.getVersion();
  }
  
  private String mavenRepositorySdkPath(String version) {
    try {
      IMaven maven = MavenPlugin.getMaven();
      String localRepositoryRoot = maven.getLocalRepository().getBasedir() + '/';
      String sdkVersionPath =
          localRepositoryRoot + "/com/google/appengine/appengine-java-sdk/" + version;
      String sdkPath = sdkVersionPath + "/appengine-java-sdk/appengine-java-sdk-" + version;
      File sdkDirectory = new File(sdkPath);
      if (!sdkDirectory.exists()) {
        fetchSdkFromRemoteRepository(
            new DefaultArtifact("com.google.appengine", "appengine-java-sdk", "zip", version),
            version,
            toAetherRepositoryList(maven.getArtifactRepositories()));
        unzip(sdkVersionPath, version);
      }
      return sdkPath;
    } catch (CoreException e) {
      return null;
    }
  }
  
  private void fetchSdkFromRemoteRepository(
      Artifact originalArtifact, String version, List<RemoteRepository> repositories) {
    RepositorySystemSession session =
        MavenPlugin.getMaven()
            .getExecutionContext().newProjectBuildingRequest().getRepositorySession();
    ArtifactRequest request =
        new ArtifactRequest(
            new DefaultArtifact("com.google.appengine", "appengine-java-sdk", "zip", version),
            repositories,
            null);
    try {
      repositorySystem.resolveArtifact(session, request);
    } catch (ArtifactResolutionException e) {
      AppEngineMavenPlugin.logError("Error downloading SDK from remote repository", e);
    }
  }
  
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
    result.setProxy(null);
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
