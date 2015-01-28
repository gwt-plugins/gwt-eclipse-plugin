/*******************************************************************************
 * Copyright 2015 Google Inc. All Rights Reserved.
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
package com.google.cloudsdk.eclipse.wtp;

import junit.framework.TestCase;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit test for {@link CloudSdkUtils}.
 */
public class CloudSdkUtilsTest extends TestCase {
  @Mock Build mockBuild;
  @Mock Model mockModel;

  /**
   * Tests that {@link CloudSdkUtils#hasGcloudMavenPlugin(Model)} returns
   * false when {@code Model} does not have the gcloud-maven-plugin.
   */
  public void testHasGcloudMavenPlugin_noGcloudPlugin() {
    MockitoAnnotations.initMocks(this);
    List<Plugin> plugins = createPluginList("myArtifcatId1", "myGroupId1", "myArtifcatId2",
        "myGroupId2");
    when(mockBuild.getPlugins()).thenReturn(plugins);
    when(mockModel.getBuild()).thenReturn(mockBuild);
    assertFalse(CloudSdkUtils.hasGcloudMavenPlugin(mockModel));
  }

  /**
   * Tests that {@link CloudSdkUtils#hasGcloudMavenPlugin(Model)} returns
   * true when {@code Model} has the gcloud-maven-plugin.
   */
  public void testHasGcloudMavenPlugin_withGcloudPlugin() {
    MockitoAnnotations.initMocks(this);
    List<Plugin> plugins = createPluginList("myArtifcatId", "myGroupId", "gcloud-maven-plugin",
        "com.google.appengine");
    when(mockBuild.getPlugins()).thenReturn(plugins);
    when(mockModel.getBuild()).thenReturn(mockBuild);

    assertTrue(CloudSdkUtils.hasGcloudMavenPlugin(mockModel));
  }

  private List<Plugin> createPluginList(String artifactId1, String groupId1, String artifactId2,
      String groupId2) {
    Plugin plugin1 = new Plugin();
    plugin1.setArtifactId(artifactId1);
    plugin1.setGroupId(groupId1);

    Plugin plugin2 = new Plugin();
    plugin2.setArtifactId(artifactId2);
    plugin2.setGroupId(groupId2);

    List<Plugin> plugins = new ArrayList<Plugin>();
    plugins.add(plugin1);
    plugins.add(plugin2);

    return plugins;
  }
}
