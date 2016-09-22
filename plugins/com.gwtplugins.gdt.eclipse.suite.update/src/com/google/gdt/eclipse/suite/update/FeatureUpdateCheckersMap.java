/*******************************************************************************
 * Copyright 2012 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.suite.update;

import com.google.gdt.eclipse.core.update.internal.core.FeatureUpdateChecker;
import com.google.gdt.eclipse.suite.update.FeatureUpdateCheckersMap.UpdateSiteToken;

import java.util.EnumMap;
import java.util.Map;

/**
 * A map from {@link UpdateSiteToken} to {@link FeatureUpdateChecker}. This is
 * used to determine which FeatureUpdateChecker is to be used if the update site
 * URL has the token given by UpdateSiteToken. FeatureUpdateChecker is used to
 * scan the downloaded contents from the update site to determine if an update
 * is available. The class is created so that we don't have to write
 * EnumMap<UpdateSiteToken, FeatureUpdateChecker> again and again.
 */
public class FeatureUpdateCheckersMap
    extends EnumMap<UpdateSiteToken, FeatureUpdateChecker> {

  /**
   * The token is a unique substring in the update site URL which will decide if
   * the update site is for GPE or GAE SDK.
   */
  public enum UpdateSiteToken {
    // TODO(deepanshu): Add a build time flag that will choose tokens based on the build being from
    // trunk or release branch.
    GAE_SDK("appengine"),
    GPE_CORE("eclipse/plugin"),
    GWT_SDK("gwt");

    private final String token;

    private UpdateSiteToken(String token) {
      this.token = token;
    }

    public String getToken() {
      return token;
    }
  }

  public FeatureUpdateCheckersMap(Class<UpdateSiteToken> keyType) {
    super(keyType);
  }

  public FeatureUpdateCheckersMap(EnumMap<UpdateSiteToken, ? extends FeatureUpdateChecker> m) {
    super(m);
  }

  public FeatureUpdateCheckersMap(Map<UpdateSiteToken, ? extends FeatureUpdateChecker> m) {
    super(m);
  }

}
