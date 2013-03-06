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
package com.google.gdt.eclipse.maven;

import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.natures.NatureUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

/**
 * A set of utilities for dealing with paths to Maven artifacts.
 */
public class MavenUtils {

  /**
   * A class to hold Maven-related information about a given artifact.
   */
  public static class MavenInfo {

    private static final int VERSION_INDEX_FROM_END_OF_MAVEN_PATH = 1;
    private static final int ARTIFACTID_INDEX_FROM_END_OF_MAVEN_PATH = 2;

    private static final int NUM_SEGMENTS_IN_ARTIFACTID = 1;
    private static final int NUM_SEGMENTS_IN_VERSION = 1;

    // The +1 is for the file name at the end of the Maven path
    private static final int NUM_TRAILING_SEGMENTS_AFTER_GROUP_ID = NUM_SEGMENTS_IN_ARTIFACTID
        + NUM_SEGMENTS_IN_VERSION + 1;

    /**
     * Creates a MavenInfo instance based on the path to a Maven artifact in a
     * local repository and the artifact's group id.
     * 
     * @param artifactPath the path to the artifact in the user's local
     *          repository (e.g.,
     *          <code>/home/user/.m2/repository/com/google/gwt/gwt
     *          -user/2.1-SNAPSHOT/gwt-user-2.1-SNAPSHOT.jar</code>)
     * @param groupId the group id of the artifact (e.g.,
     *          <code>com.google.gwt</code>)
     * @return an instance of MavenInfo, or null if one could not be found due
     *         to a mismatch between the group id and the artifact path
     */
    public static MavenInfo create(IPath artifactPath, String groupId) {
      if (artifactPath == null || artifactPath.isEmpty() || groupId == null
          || StringUtilities.isEmpty(groupId)) {
        return null;
      }

      IPath groupPath = Path.fromPortableString(groupId.replace('.', '/'));
      final int numTrailingSegmentsAfterRepositoryBase = NUM_TRAILING_SEGMENTS_AFTER_GROUP_ID
          + groupPath.segmentCount();

      if (artifactPath.segmentCount() <= numTrailingSegmentsAfterRepositoryBase) {
        return null;
      }

      String artifactName = artifactPath.lastSegment();
      String version = artifactPath.segment(artifactPath.segmentCount()
          - VERSION_INDEX_FROM_END_OF_MAVEN_PATH - 1);
      String artifactId = artifactPath.segment(artifactPath.segmentCount()
          - ARTIFACTID_INDEX_FROM_END_OF_MAVEN_PATH - 1);

      if (!artifactPath.removeLastSegments(NUM_TRAILING_SEGMENTS_AFTER_GROUP_ID).removeTrailingSeparator().toPortableString().endsWith(
          groupPath.toPortableString())) {
        return null;
      }

      IPath repositoryPath = artifactPath.removeLastSegments(numTrailingSegmentsAfterRepositoryBase);

      return new MavenInfo(repositoryPath, groupId, artifactId, artifactName,
          version);
    }

    private final IPath repositoryPath;
    private final String groupId;
    private final String artifactId;
    private final String artifactName;

    private final String version;

    protected MavenInfo(IPath repositoryPath, String groupId,
        String artifactId, String artifactName, String version) {
      this.repositoryPath = repositoryPath;
      this.groupId = groupId;
      this.artifactId = artifactId;
      this.artifactName = artifactName;
      this.version = version;
    }

    /**
     * Returns anything that comes after the
     * <code><artifact id>-<artifact version></code> prefix of an artifact's
     * name, including any hyphens and file extensions. For example, for the
     * artifact <code>gwt-dev-2.1-SNAPSHOT-javadoc.jar</code>, the
     * artifactNameSuffix would be <code>-SNAPSHOT-javadoc.jar</code>.
     * 
     * If there is a mismatch between the version of the artifact and the
     * artifact's name, then the default suffix, <code>.jar</code> is returned.
     */
    public String computeArtifactNameSuffix() {
      int matchIndex = artifactName.indexOf(version);

      if (matchIndex == -1
          || matchIndex + version.length() >= artifactName.length()) {
        return ".jar";
      }

      return artifactName.substring(matchIndex + version.length());
    }

    /**
     * Returns the artifact id (e.g., <code>gwt-dev</code>).
     */
    public String getArtifactId() {
      return artifactId;
    }

    /**
     * Returns the artifact's name (e.g.,
     * <code>gwt-dev-2.1-SNAPSHOT-javadoc.jar</code>).
     */
    public String getArtifactName() {
      return artifactName;
    }

    /**
     * Returns the group id (e.g., <code>com.google.foo</code>).
     */
    public String getGroupId() {
      return groupId;
    }

    /**
     * Returns the local repository path (e.g.,
     * <code>/home/user/.m2/repository</code>).
     */
    public IPath getRepositoryPath() {
      return repositoryPath;
    }

    /**
     * Returns the version (e.g., 2.1, 2.1-SNAPSHOT).
     */
    public String getVersion() {
      return version;
    }
  }

  public static final String OLD_MAVEN2_NATURE_ID = "org.maven.ide.eclipse.maven2Nature";
  public static final String MAVEN2_NATURE_ID = "org.eclipse.m2e.core.maven2Nature";

  /**
   * Generates the absolute path to an artifact in a local Maven repository,
   * based on the parameters given.
   * 
   * @param repositoryBase the path to the local repository (e.g.,
   *          <code>/home/user/.m2/repository</code>)
   * @param groupId the group id of the artifact (e.g.,
   *          <code>com.google.foo</code>)
   * @param version the version of the artifact (e.g.,
   *          <code>2.1, 2.1-SNAPSHOT</code>)
   * @param artifactId the artifact id (e.g., <code>gwt-dev</code>)
   * @param artifactNameSuffix anything that comes after the <code><artifact
   *          id>-<artifact version></code> prefix of an artifact's name,
   *          including any hyphens and file extensions. For example, for the
   *          artifact <code>gwt-dev-2.1-SNAPSHOT-javadoc.jar</code>, the
   *          artifactNameSuffix would be <code>-SNAPSHOT-javadoc.jar</code>.
   */
  public static IPath generateArtifactPath(IPath repositoryBase,
      String groupId, String version, String artifactId,
      String artifactNameSuffix) {
    if (repositoryBase == null || repositoryBase.isEmpty()) {
      return null;
    }

    if (StringUtilities.isEmpty(groupId) || StringUtilities.isEmpty(version)
        || StringUtilities.isEmpty(artifactId)
        || StringUtilities.isEmpty(artifactNameSuffix)) {
      return null;
    }

    return repositoryBase.append(groupId.replace('.', '/')).append(artifactId).append(
        version).append(artifactId + '-' + version + artifactNameSuffix);
  }

  /**
   * Given the path of an artifact in a local Maven repository and its group id,
   * and the artifact id of a peer artifact, return the path in the local Maven
   * repository of the peer artifact.
   * 
   * For example, with a base artifact path of
   * <code>/home/user/.m2/com/google/gwt/gwt-user/2.1/gwt-user-2.1.jar</code>, a
   * group id of <code>com.google.gwt</code>, and a peer artifact id of
   * <code>gwt-dev.jar</code>, the returned path would be
   * <code>/home/user/.m2/com/google/gwt/gwt-dev/2.1/gwt-dev-2.1.jar</code>.
   */
  public static IPath getArtifactPathForPeerMavenArtifact(
      IPath baseArtifactPath, String groupId, String peerArtifactId) {
    if (baseArtifactPath == null || baseArtifactPath.isEmpty()) {
      return null;
    }

    if (StringUtilities.isEmpty(groupId)) {
      return null;
    }

    if (StringUtilities.isEmpty(peerArtifactId)) {
      return null;
    }

    MavenInfo baseArtifactInfo = MavenInfo.create(baseArtifactPath, groupId);

    if (baseArtifactInfo == null) {
      return null;
    }

    return generateArtifactPath(baseArtifactInfo.getRepositoryPath(), groupId,
        baseArtifactInfo.getVersion(), peerArtifactId,
        baseArtifactInfo.computeArtifactNameSuffix());
  }

  /**
   * Returns <code>true</code> if the given project has the Maven 2 nature.
   * This checks for the old maven nature (till m2Eclipse 0.12) and the new
   * Maven nature (m2Eclipse 1.0.0 and up).
   */
  public static boolean hasMavenNature(IProject project) {
    try {
      if (NatureUtils.hasNature(project, MavenUtils.MAVEN2_NATURE_ID)) {
        return true;
      }
      if (NatureUtils.hasNature(project, MavenUtils.OLD_MAVEN2_NATURE_ID)) {
        return true;
      }
    } catch (CoreException ce) {
      Activator.getDefault().getLog().log(
          new Status(IStatus.ERROR, Activator.PLUGIN_ID,
              "Unable to examine natures on project " + project.getName(), ce));
    }
    return false;
  }

  /**
   * Returns <code>true</code> if the given project has the Spring nature.
   * 
   * Technically, this should live in a Spring-specific plugin, but since the
   * only Spring-specific functionality that we have is related to Maven, we can
   * tuck it in here for now.
   */
  public static boolean hasSpringNature(IProject project) {
    try {
      if (NatureUtils.hasNature(project,
          "org.springframework.ide.eclipse.core.springnature")) {
        return true;
      }
    } catch (CoreException ce) {
      Activator.getDefault().getLog().log(
          new Status(IStatus.ERROR, Activator.PLUGIN_ID,
              "Unable to examine natures on project " + project.getName(), ce));
    }
    return false;
  }
}
