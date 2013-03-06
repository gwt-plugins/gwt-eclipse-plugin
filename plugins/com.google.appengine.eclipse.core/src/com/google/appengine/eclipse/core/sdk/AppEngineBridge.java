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
package com.google.appengine.eclipse.core.sdk;

import com.google.gdt.eclipse.core.deploy.DeploymentSet;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;

import javax.management.ReflectionException;

/**
 * The bridge interface that allows communication between the GAE Plugin and the
 * App Engine SDK. The implementation of this interface is loaded in an isolated
 * classloader with the App Engine SDK. However, the isolated classloader has
 * access to this interface type because the parent classloader is that of the
 * GAE Plugin.
 */
public interface AppEngineBridge {

  /**
   * Options which are passed in to the App Engine API on deployment.
   */
  public static class DeployOptions {
    private String deployFolderOSPath = null;
    private DeploymentSet deploymentSet = null;
    private String email = null;
    private OutputStream errorStream = null;
    private String javaCompilerExecutableOSPath = null;
    private String javaExecutableOSPath = null;
    private String oauth2Token = null;
    private OutputStream outputStream = null;
    private String password = null;

    public DeployOptions(String email, String password, String oauth2Token,
        String deployFolderOSPath, DeploymentSet deploymentSet, OutputStream outputStream,
        OutputStream errorStream, String javaExecutableOSPath,
        String javaCompilerExecutableOSPath) {
      this.deploymentSet = deploymentSet;
      this.oauth2Token = oauth2Token;
      this.email = email;
      this.password = password;
      this.deployFolderOSPath = deployFolderOSPath;
      this.outputStream = outputStream;
      this.errorStream = errorStream;
      this.javaExecutableOSPath = javaExecutableOSPath;
      this.javaCompilerExecutableOSPath = javaCompilerExecutableOSPath;
    }

    public String getDeployFolderOSPath() {
      return deployFolderOSPath;
    }

    public DeploymentSet getDeploymentSet() {
      return deploymentSet;
    }

    public String getEmail() {
      return email;
    }

    public OutputStream getErrorStream() {
      return errorStream;
    }

    public String getJavaCompilerExecutableOSPath() {
      return javaCompilerExecutableOSPath;
    }

    public String getJavaExecutableOSPath() {
      return javaExecutableOSPath;
    }

    public String getOAuth2Token() {
      return oauth2Token;
    }

    public OutputStream getOutputStream() {
      return outputStream;
    }

    public String getPassword() {
      return password;
    }

    public void setDeployFolderOSPath(String deployFolderOSPath) {
      this.deployFolderOSPath = deployFolderOSPath;
    }

    public void setDeploymentSet(DeploymentSet deploymentSet) {
      this.deploymentSet = deploymentSet;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public void setErrorStream(OutputStream errorStream) {
      this.errorStream = errorStream;
    }

    public void setJavaCompilerExecutableOSPath(
        String javaCompilerExecutableOSPath) {
      this.javaCompilerExecutableOSPath = javaCompilerExecutableOSPath;
    }

    public void setJavaExecutableOSPath(String javaExecutableOSPath) {
      this.javaExecutableOSPath = javaExecutableOSPath;
    }

    public void setOAuth2Token(String token) {
      this.oauth2Token = token;
    }

    public void setOutputStream(OutputStream outputStream) {
      this.outputStream = outputStream;
    }

    public void setPassword(String password) {
      this.password = password;
    }
  }

  String APPENGINE_CLOUD_SQL_JAR = "google_sql.jar";

  String APPENGINE_CLOUD_SQL_JAR_PATH_IN_SDK = "/lib/impl/";

  String APPENGINE_PROXY_JAR_NAME = "appengine-sdk-proxy.jar";

  String APPENGINE_TOOLS_JAR_NAME = "appengine-tools-api.jar";

  String MIN_VERSION_FOR_OPT_DATANUCLEUS_LIB = "1.6.4";

  IStatus deploy(IProgressMonitor monitor, DeployOptions options)
      throws IOException;

  String getAppId(IPath warPath) throws IOException;

  /**
   * Return the value for the version element in the WEB-INF/appengine-web.xml
   * file under the war path.
   */
  String getAppVersion(IPath warPath) throws IOException;

  /**
   * Returns the set of files to place on a project's build classpath.
   */
  List<File> getBuildclasspathFiles() throws ReflectionException;

  /**
   * Returns the set of files to place on a project's build classpath.
   *
   * @param getDatanucleusFiles should the datanucleus jars be returned. If the SDK doesn't support
   *          optional libraries, the datanucleus jars are returned regardless of the parameter.
   */
  List<File> getBuildclasspathFiles(boolean getDatanucleusFiles) throws ReflectionException;

  /**
   * Returns the latest version of all the user libs.
   *
   * @param getDatanucleusFiles should the datanucleus jars be returned. If the SDK doesn't support
   *          optional libraries, the datanucleus jars are returned regardless of the parameter.
   */
  List<File> getLatestUserLibFiles(boolean getDatanucleusFiles) throws ReflectionException;

  String getLatestVersion(String libName) throws ReflectionException;

  String getSdkVersion();

  List<File> getSharedLibFiles();

  /**
   * Use {@link #getToolsLibFiles(String libName, String version)} for newer versions of GAE SDK.
   */
  List<File> getToolsLibFiles();

  /**
   * @param libName The library name. The list of libraries can be obtained from
   *          {@link #getToolsLibNames()}. The list of versions can be obtained from
   *          {@link #getToolsLibVersions(String libName)}.
   */
  List<File> getToolsLibFiles(String libName, String version) throws ReflectionException;

  List<String> getToolsLibNames() throws ReflectionException;

  List<String> getToolsLibVersions(String libName) throws ReflectionException;

  /**
   * Use {@link #getUserLibFiles(String libName, String version)} for newer versions of GAE SDK.
   */
  List<File> getUserLibFiles();

  /**
   * @param libName The library name. The list of libraries can be obtained from
   *          {@link #getUserLibNames()}. The list of versions can be obtained from
   *          {@link #getUserLibVersions(String libName)}.
   */
  List<File> getUserLibFiles(String libName, String version) throws ReflectionException;

  List<String> getUserLibNames() throws ReflectionException;

  List<String> getUserLibVersions(String libName) throws ReflectionException;

  Set<String> getWhiteList();
}
