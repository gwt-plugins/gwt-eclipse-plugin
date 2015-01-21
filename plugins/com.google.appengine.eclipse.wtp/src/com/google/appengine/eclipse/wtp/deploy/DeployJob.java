/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.appengine.eclipse.wtp.deploy;

import com.google.appengine.eclipse.core.properties.GaeProjectProperties;
import com.google.appengine.eclipse.wtp.server.GaeServer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import java.io.OutputStream;
import java.util.List;

/**
 * Does deploy to Google server.
 */
public final class DeployJob extends PublishingJob {
  private static final String COMMAND_UPDATE = "update";
  private static final String ARG_ENABLE_JAR_SPLITTING = "--enable_jar_splitting";
  private static final String ARG_DO_JAR_CLASSES = "--enable_jar_classes";
  private static final String ARG_RETAIN_STAGING_DIR = "--retain_upload_dir";

  public DeployJob(String oauth2ClientId, String oauth2ClientSecret, String oauth2RefreshToken,
      GaeServer gaeServer, OutputStream outputStream) throws CoreException {
    super(oauth2ClientId, oauth2ClientSecret, oauth2RefreshToken, gaeServer, outputStream);
  }

  @Override
  protected List<String> getProcessArguments() throws CoreException {
    List<String> args = super.getProcessArguments();
    IProject project = gaeServer.getProject();
    if (GaeProjectProperties.getGaeEnableJarSplitting(project)) {
      args.add(ARG_ENABLE_JAR_SPLITTING);
    }
    if (GaeProjectProperties.getGaeDoJarClasses(project)) {
      args.add(ARG_DO_JAR_CLASSES);
    }
    if (GaeProjectProperties.getGaeRetainStagingDir(project)) {
      args.add(ARG_RETAIN_STAGING_DIR);
    }
    args.add(COMMAND_UPDATE);
    args.add(appDirectory.toOSString());
    return args;
  }
}