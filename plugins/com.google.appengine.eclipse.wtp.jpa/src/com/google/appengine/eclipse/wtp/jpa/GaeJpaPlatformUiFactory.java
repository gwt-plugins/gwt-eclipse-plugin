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
package com.google.appengine.eclipse.wtp.jpa;

import com.google.appengine.eclipse.webtools.facet.JpaFacetHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jpt.jpa.ui.JpaPlatformUi;
import org.eclipse.jpt.jpa.ui.JpaPlatformUiFactory;
import org.eclipse.jpt.jpa.ui.internal.platform.generic.GenericJpaPlatformUiFactory;

/**
 * Factory for JPA platform UI.
 */
@SuppressWarnings("restriction")
public final class GaeJpaPlatformUiFactory implements JpaPlatformUiFactory {

  @Override
  public JpaPlatformUi buildJpaPlatformUi() {
    try {
      return new GaeJpaPlatformUi(GenericJpaPlatformUiFactory.NAVIGATOR_FACTORY_PROVIDER,
          JpaFacetHelper.getJpaPlatformUiProvider());
    } catch (CoreException e) {
      AppEngineJpaPlugin.logMessage("Unable to bring up the JPA conversion UI.", e);
    }
    // On failures return null. The framework already handles null returns for the cases where it
    // doesn't find a factory.
    return null;
  }
}
