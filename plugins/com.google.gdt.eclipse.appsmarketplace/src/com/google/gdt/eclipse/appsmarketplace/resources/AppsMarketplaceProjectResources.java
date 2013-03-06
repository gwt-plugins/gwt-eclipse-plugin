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
package com.google.gdt.eclipse.appsmarketplace.resources;

import com.google.gdt.eclipse.appsmarketplace.AppsMarketplacePlugin;
import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.WebAppUtilities.FileInfo;
import com.google.gdt.eclipse.core.resources.ProjectResources;

import org.eclipse.core.internal.resources.ICoreConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains methods used to generate the source for Apps Marketplace projects.
 *
 */
public class AppsMarketplaceProjectResources {
  private static final String welcomeFileName = "index.html";

  private static String[] sampleAppJavaFilenames = new String[] {
      "CalendarServlet.java", "OpenIdServlet.java", "UserInfo.java",
      "openid/ConsumerFactory.java", "openid/GoogleHostedHostMetaFetcher.java",
      "openid/GuiceModule.java", "openid/SerialHostMetaFetcher.java",
      "openid/appengine/AppEngineHttpFetcher.java",
      "openid/appengine/AppEngineTrustsRootProvider.java",
      "openid/appengine/Openid4javaFetcher.java"};

  private static String[] sampleAppJSPFilenames = new String[] {"meeting.jsp"};

  public static void addAppsMarketplaceMetadata(
      String projectName, IFolder warFolder, IProgressMonitor monitor)
      throws CoreException {
    Map<String, String> replacements = new HashMap<String, String>();
    replacements.put("@ProjectName@", projectName);
    InputStream is;

    IFolder webInfFolder = warFolder.getFolder("WEB-INF");
    if (webInfFolder.exists() == false) {
      webInfFolder.create(IFile.FORCE, true, monitor);
    }

    IFile appManifestFile = warFolder.getFile(
        "/WEB-INF/application-manifest.xml");
    appManifestFile.delete(IFile.FORCE, monitor);
    is = ResourceUtils.getResourceAsStreamAndFilterContents(
        AppsMarketplaceProjectResources.class, replacements,
        "application-manifest.xml");
    appManifestFile.create(is, IFile.FORCE, monitor);

    IFile listingManifestFile = warFolder.getFile(
        "/WEB-INF/listing-manifest.xml");
    listingManifestFile.delete(IFile.FORCE, monitor);
    is = ResourceUtils.getResourceAsStreamAndFilterContents(
        AppsMarketplaceProjectResources.class, replacements,
        "listing-manifest.xml");
    listingManifestFile.create(is, IFile.FORCE, monitor);
  }

  public static void createAppsMarketplaceMetadata(
      String projectName, List<FileInfo> fileInfos) throws CoreException {
    Map<String, String> replacements = new HashMap<String, String>();
    replacements.put("@ProjectName@", projectName);
    // Add marketplace manifests.
    addFile(new Path(WebAppUtilities.DEFAULT_WAR_DIR_NAME
        + "/WEB-INF/application-manifest.xml"),
        ResourceUtils.getResourceAsStreamAndFilterContents(
            AppsMarketplaceProjectResources.class, replacements,
            "application-manifest.xml"), fileInfos);
    addFile(new Path(
        WebAppUtilities.DEFAULT_WAR_DIR_NAME + "/WEB-INF/listing-manifest.xml"),
        AppsMarketplaceProjectResources.class.getResourceAsStream(
            "listing-manifest.xml"), fileInfos);
  }

  public static void createAppsMarketplaceMetadataAndSamples(
      String packageName, List<FileInfo> fileInfos)
      throws CoreException, UnsupportedEncodingException {
    // Add Java files.
    String packagePath = packageName.replace('.', '/');

    String[] filenames = AppsMarketplaceProjectResources.sampleAppJavaFilenames;
    for (String filename : filenames) {
      addFile(new Path(
          "src/" + packagePath + "/samples/apps/marketplace/" + filename),
          AppsMarketplaceProjectResources.createSource(packageName,
              "samples/apps/marketplace/" + filename + ".template"), fileInfos);
    }

    // Add css for meeting app.
    addFile(new Path(
        WebAppUtilities.DEFAULT_WAR_DIR_NAME + "/AppsMarketplaceDemo.css"),
        AppsMarketplaceProjectResources.createSource(
            packageName, "AppsMarketplaceDemoCss.template"), fileInfos);

    // Add JSPs
    filenames = AppsMarketplaceProjectResources.sampleAppJSPFilenames;
    for (String filename : filenames) {
      addFile(new Path(
          WebAppUtilities.DEFAULT_WAR_DIR_NAME + "/WEB-INF/jsp/" + filename),
          AppsMarketplaceProjectResources.createSource(
              packageName, "jsp/" + filename), fileInfos);
    }
    // Add cacerts
    addFile(new Path("src/cacerts.bin"),
        AppsMarketplaceProjectResources.class.getResourceAsStream(
            "cacerts.bin"), fileInfos);
    // Add marketplace manifests.
    addFile(new Path(WebAppUtilities.DEFAULT_WAR_DIR_NAME
        + "/WEB-INF/application-manifest.xml"),
        AppsMarketplaceProjectResources.class.getResourceAsStream(
            "application-manifest-sample.xml"), fileInfos);
    addFile(new Path(
        WebAppUtilities.DEFAULT_WAR_DIR_NAME + "/WEB-INF/listing-manifest.xml"),
        AppsMarketplaceProjectResources.class.getResourceAsStream(
            "listing-manifest.xml"), fileInfos);
  }

  public static String createSource(String packageName, String templatePath)
      throws CoreException {
    String source = getSource(packageName, templatePath);
    return ProjectResources.reformatJavaSourceAsString(source);
  }

  public static String createWebXmlSnippet(String packageName)
      throws CoreException {
    return getSource(packageName, "appsmarketplace-web.xml.template");
  }

  public static StringBuffer readFileAsStringBuffer(IFile file)
      throws CoreException {
    Reader reader = new InputStreamReader(file.getContents());
    StringBuffer stringBuffer = new StringBuffer();
    char[] charBuffer = new char[4096];
    int charsRead;
    try {
      while ((charsRead = reader.read(charBuffer)) > 0) {
        stringBuffer.append(charBuffer, 0, charsRead);
      }
      reader.close();
    } catch (IOException e) {
      throw new CoreException(new Status(
          Status.ERROR, AppsMarketplacePlugin.PLUGIN_ID, e.getMessage()));
    }
    return stringBuffer;
  }

  public static void removeAppsMarketplaceMetaData(
      IProject javaProject, IFolder warFolder, IProgressMonitor monitor)
      throws CoreException {
    IFile appManifestFile = warFolder.getFile(
        "/WEB-INF/application-manifest.xml");
    IFile listingManifestFile = warFolder.getFile(
        "/WEB-INF/listing-manifest.xml");

    appManifestFile.delete(IFile.FORCE, monitor);
    listingManifestFile.delete(IFile.FORCE, monitor);
    IFolder webInfFolder = warFolder.getFolder("WEB-INF");
    if (webInfFolder.members(true) == ICoreConstants.EMPTY_RESOURCE_ARRAY) {
      webInfFolder.delete(IFile.FORCE, monitor);
    }
  }

  public static void updateAppEngineWebxml(
      IProject javaProject, IProgressMonitor monitor) throws CoreException {
    IProject project = javaProject.getProject();

    WebAppUtilities.verifyIsWebApp(project);

    IFolder webInfFolder = WebAppUtilities.getWebInfSrc(project);
    IFile appEngineWebXml = webInfFolder.getFile("appengine-web.xml");

    if (appEngineWebXml == null) {
      throw new CoreException(
          new Status(Status.ERROR, AppsMarketplacePlugin.PLUGIN_ID,
              "Could not find web.xml in project."));
    }
    StringBuffer sb = readFileAsStringBuffer(appEngineWebXml);
    sb.insert(sb.indexOf("<system-properties>"),
        "<sessions-enabled>true</sessions-enabled>\n");
    sb.insert(sb.indexOf("</system-properties>"),
        "<property name=\"com.google.gdata.DisableCookieHandler\""
            + " value=\"true\"/>\n");
    InputStream inputStream = new ByteArrayInputStream(
        sb.toString().getBytes());
    appEngineWebXml.setContents(inputStream, IFile.FORCE, monitor);
  }

  public static void updateWebXml(
      IProject javaProject, String packageName, IProgressMonitor monitor)
      throws CoreException {
    IProject project = javaProject.getProject();

    WebAppUtilities.verifyIsWebApp(project);

    IFolder webInfFolder = WebAppUtilities.getWebInfSrc(project);
    IFile webXmlFile = webInfFolder.getFile("web.xml");

    if (webXmlFile == null) {
      throw new CoreException(
          new Status(Status.ERROR, AppsMarketplacePlugin.PLUGIN_ID,
              "Could not find web.xml in project."));
    }
    StringBuffer wxsb = readFileAsStringBuffer(webXmlFile);
    wxsb.insert(wxsb.indexOf("</web-app>"), createWebXmlSnippet(packageName));
    InputStream inputStream = new ByteArrayInputStream(
        wxsb.toString().getBytes());
    webXmlFile.setContents(inputStream, IFile.FORCE, monitor);
  }

  /**
   * Updates welcome file in web.xml for non Gwt projects during sample app creation.
   */
  public static void updateWelcomeFile(IProject javaProject, String servletName,
    String servletPath, IProgressMonitor monitor) throws CoreException {
    IProject project = javaProject.getProject();
    WebAppUtilities.verifyIsWebApp(project);

    IFolder warfolder = WebAppUtilities.getWarSrc(project);
    IFile welcomeFile = warfolder.getFile(welcomeFileName);

    welcomeFile.delete(IFile.FORCE, monitor);

    InputStream is = AppsMarketplaceProjectResources.class.getResourceAsStream(
        "WelcomeFile.template");

    welcomeFile.create(is, IFile.FORCE, monitor);
  }

  public static boolean validateAppsMarketplaceMetadataExists(
      IProject project, String warFolderPath) {
    IResource resource = project.findMember(warFolderPath);
    IFolder warFolder;
    if (resource instanceof IFolder) {
      warFolder = (IFolder) resource;
      IPath appManifest = new Path("/WEB-INF/application-manifest.xml");
      IPath listingManifest = new Path("/WEB-INF/listing-manifest.xml");
      return warFolder.exists(appManifest) && warFolder.exists(listingManifest);
    } else {
      return false;
    }
  }

  private static void addFile(
      IPath path, InputStream inputStream, List<FileInfo> fileInfos) {
    fileInfos.add(new FileInfo(path, inputStream));
  }

  private static void addFile(
      IPath path, String content, List<FileInfo> fileInfos)
      throws UnsupportedEncodingException {
    fileInfos.add(new FileInfo(
        path, new ByteArrayInputStream(content.getBytes("UTF-8"))));
  }

  private static String getSource(String packageName, String templatePath)
      throws CoreException {
    Map<String, String> replacements = new HashMap<String, String>();
    replacements.put("@PackageName@", packageName);
    return ResourceUtils.getResourceAsStringAndFilterContents(
        AppsMarketplaceProjectResources.class, replacements, templatePath);
  }
}
