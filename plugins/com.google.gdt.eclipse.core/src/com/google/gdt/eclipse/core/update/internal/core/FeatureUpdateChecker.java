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
package com.google.gdt.eclipse.core.update.internal.core;

import com.google.gdt.eclipse.core.CorePluginLog;

import org.eclipse.core.runtime.PluginVersionIdentifier;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Queries a set of {@link UpdateComputer}s to determine if any updates are
 * available. The UpdateComputers are given the root element of a
 * <code>site.xml</code> file as input.
 */
@SuppressWarnings("deprecation")
public class FeatureUpdateChecker {

  /**
   * Participants in the update check process which are used to determine if an
   * update is available based on the contents of the <code>site.xml</code>
   * file.
   */
  public abstract static class UpdateComputer {

    /**
     * Called by {@link FeatureUpdateChecker} to determine if any updates are
     * available, based on the root element of a <code>site.xml</code> file.
     * 
     * @param siteXMLRootElem
     * @return an {@link UpdateInfo} object containing information about whether
     *         or not an update is available
     */
    public abstract UpdateInfo checkSiteXMLForUpdates(Element siteXMLRootElem);

    /**
     * Helper method that can be invoked via
     * {@link #checkSiteXMLForUpdates(Element)} to determine if an update for a
     * given feature is available.
     * 
     * If the feature element in the <code>site.xml</code> document has a
     * higher version number than the <code>featureVersion</code>, AND is
     * higher than the version number than
     * <code>lastAcknowlegedVertionForFeatureUpdate</code>, then an
     * {@link UpdateInfo} object is returned with the version of the latest
     * available update set, along with the update available flag.
     * 
     * If there is no update available, or the
     * <code>siteXMLRootElement</site> is null, then an {@link UpdateInfo}
     * object is returned with the feature id set, and the update available flag set to <code>false</code>
     * .
     */
    protected UpdateInfo doCheckSiteXMLForUpdates(String featureId,
        PluginVersionIdentifier featureVersion,
        PluginVersionIdentifier lastAcknowledgedVersionForFeatureUpdate,
        Element siteXMLRootElement, boolean compareQualifiers) {

      FeatureUpdateChecker.UpdateInfo context = new FeatureUpdateChecker.UpdateInfo(
          featureId);

      if (siteXMLRootElement == null) {
        return context;
      }

      /*
       * We need to do a search through all of the feature elements that match
       * the feature id of the installedFeature, and find the one with the
       * highest version number.
       */
      PluginVersionIdentifier maxFeatureVersion = null;

      NodeList list = siteXMLRootElement.getChildNodes();
      int length = list.getLength();
      for (int i = 0; i < length; ++i) {
        Node node = list.item(i);
        short type = node.getNodeType();
        if (type == Node.ELEMENT_NODE) {
          Element featureElement = (Element) node;
          if (featureElement.getNodeName().equalsIgnoreCase("feature")) {
            String curFeatureId = featureElement.getAttribute("id");

            if (!featureId.equals(curFeatureId)) {
              continue;
            }

            String versionStr = featureElement.getAttribute("version");

            // No version attribute; this is an improperly formatted feature
            // element
            if (versionStr == null) {
              continue;
            }

            PluginVersionIdentifier siteXMLFeatureVersion = new PluginVersionIdentifier(
                versionStr);

            if (!compareQualifiers) {
              siteXMLFeatureVersion = new PluginVersionIdentifier(
                  siteXMLFeatureVersion.getMajorComponent(),
                  siteXMLFeatureVersion.getMinorComponent(),
                  siteXMLFeatureVersion.getServiceComponent());
            }

            if (maxFeatureVersion == null
                || siteXMLFeatureVersion.isGreaterThan(maxFeatureVersion)) {
              maxFeatureVersion = siteXMLFeatureVersion;
            }
          }
        }
      }

      if (maxFeatureVersion == null) {
        /*
         * If we get here, there must not have been a single feature element
         * with an id that matches that of this feature with a version attribute
         * specified.
         */
        CorePluginLog.logError("The site.xml file downloaded from the update site does not contain any <feature> elements with an id of '"
            + featureId
            + "'. Unable to determine if there are updates available.");
        return context;
      }

      /*
       * If the feature with the highest version on the update site has a higher
       * version than the currently installed feature AND a higher version than
       * any feature updates that the user has acknowledged, then we've found an
       * update that has a higher version than anything the user has been
       * notified about beforehand.
       */
      if (maxFeatureVersion.isGreaterThan(featureVersion)
          && maxFeatureVersion.isGreaterThan(lastAcknowledgedVersionForFeatureUpdate)) {
        context.setUpdateAvailable(true);
        context.setUpdatedFeatureVersion(maxFeatureVersion);
      }

      return context;
    }
  }

  /**
   * Provides information about available updates.
   */
  public static class UpdateInfo {

    private static final PluginVersionIdentifier NO_VERSION = new PluginVersionIdentifier(
        "0.0.0.0");

    private boolean isUpdateAvailable = false;
    private String featureId = null;
    private PluginVersionIdentifier updatedFeatureVersion = NO_VERSION;

    /**
     * Constructs an object to hold update information for the given feature.
     * 
     * @param featureId the id of the feature
     */
    public UpdateInfo(String featureId) {
      this.featureId = featureId;
    }

    public String getFeatureId() {
      return featureId;
    }

    /**
     * If an update is available, returns the updated version that is available
     * for the feature. Otherwise, returns a version of <code>0.0.0.0</code>.
     */
    public PluginVersionIdentifier getUpdatedFeatureVersion() {
      return updatedFeatureVersion;
    }

    /**
     * Returns <code>true</code> if an update is available for the feature.
     */
    public boolean isUpdateAvailable() {
      return isUpdateAvailable;
    }

    public void setUpdateAvailable(boolean isUpdateAvailable) {
      this.isUpdateAvailable = isUpdateAvailable;
    }

    public void setUpdatedFeatureVersion(
        PluginVersionIdentifier updatedFeatureVersion) {
      if (updatedFeatureVersion == null) {
        this.updatedFeatureVersion = NO_VERSION;
      } else {
        this.updatedFeatureVersion = updatedFeatureVersion;
      }
    }
  }

  private List<UpdateComputer> updateComputers = new ArrayList<UpdateComputer>();

  /**
   * Constructs an instance with a set of {@link UpdateComputer}s. The
   * UpdateComputers are used to determine if updates are available based on the
   * contents of the <code>site.xml</code> file.
   * 
   * @param updateComputers used to determine if updates are available based on
   *          the contents of the <code>site.xml</code> file
   */
  public FeatureUpdateChecker(UpdateComputer... updateComputers) {
    this.updateComputers.addAll(Arrays.asList(updateComputers));
  }

  /**
   * Given the root element of a parsed <code>site.xml</code> files, runs each
   * of the update computers with the root element as input to determine if
   * updates are available. If any updates are available, information about each
   * update is returned.
   * 
   * @param rootElement the root element of a parsed <code>site.xml</code>
   *          file
   * @return a list containing information about each available update, or an
   *         empty list if no updates are available
   */
  public List<UpdateInfo> checkForUpdates(Element rootElement) {
    List<UpdateInfo> updateInfo = new ArrayList<UpdateInfo>();
    if (rootElement != null
        && rootElement.getNodeName().equalsIgnoreCase("site")) {
      for (UpdateComputer updateComputer : updateComputers) {
        UpdateInfo context = updateComputer.checkSiteXMLForUpdates(rootElement);

        if (context.isUpdateAvailable()) {
          updateInfo.add(context);
        }
      }
    } else {
      CorePluginLog.logError("The XML file downloaded from the update site "
          + " is not a valid site.xml file - Unable to determine if there are updates available.");
    }
    return updateInfo;
  }
}
