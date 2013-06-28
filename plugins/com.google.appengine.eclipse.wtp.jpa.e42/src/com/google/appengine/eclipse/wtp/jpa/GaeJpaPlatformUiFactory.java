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
package com.google.appengine.eclipse.wtp.jpa;

import org.eclipse.jpt.jpa.ui.JpaPlatformUi;
import org.eclipse.jpt.jpa.ui.JpaPlatformUiFactory;
import org.eclipse.jpt.jpa.ui.internal.jpa2.Generic2_0JpaPlatformUiProvider;
import org.eclipse.jpt.jpa.ui.internal.platform.generic.GenericJpaPlatformUiFactory;

/**
 * Factory for JPA platform UI.
 */
@SuppressWarnings("restriction")
public final class GaeJpaPlatformUiFactory implements JpaPlatformUiFactory {

  @Override
  public JpaPlatformUi buildJpaPlatformUi() {
    return new GaeJpaPlatformUi(GenericJpaPlatformUiFactory.NAVIGATOR_FACTORY_PROVIDER,
        Generic2_0JpaPlatformUiProvider.instance());
  }
}
