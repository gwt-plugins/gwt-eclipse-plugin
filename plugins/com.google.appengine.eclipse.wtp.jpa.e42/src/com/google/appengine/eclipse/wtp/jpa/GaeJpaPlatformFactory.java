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

import org.eclipse.jpt.common.core.JptResourceType;
import org.eclipse.jpt.jpa.core.JpaFacet;
import org.eclipse.jpt.jpa.core.JpaPlatform;
import org.eclipse.jpt.jpa.core.JpaPlatform.Version;
import org.eclipse.jpt.jpa.core.JpaPlatformFactory;
import org.eclipse.jpt.jpa.core.JpaPlatformVariation;
import org.eclipse.jpt.jpa.core.context.AccessType;
import org.eclipse.jpt.jpa.core.internal.GenericJpaPlatform;
import org.eclipse.jpt.jpa.core.internal.GenericJpaPlatformFactory;
import org.eclipse.jpt.jpa.core.internal.JpaAnnotationProvider;
import org.eclipse.jpt.jpa.core.internal.jpa2.Generic2_0JpaAnnotationDefinitionProvider;
import org.eclipse.jpt.jpa.core.internal.jpa2.Generic2_0JpaPlatformProvider;
import org.eclipse.jpt.jpa.core.internal.jpa2.GenericJpaFactory2_0;
import org.eclipse.persistence.jpa.jpql.parser.JPQLGrammar2_0;

/**
 * {@link JpaPlatformFactory} implemented currently as generic JPA factory for version 2.0.
 */
@SuppressWarnings("restriction")
public class GaeJpaPlatformFactory implements JpaPlatformFactory {

  @Override
  public JpaPlatform buildJpaPlatform(String id) {
    return new GenericJpaPlatform(id, buildJpaVersion(), new GenericJpaFactory2_0(),
        new JpaAnnotationProvider(Generic2_0JpaAnnotationDefinitionProvider.instance()),
        Generic2_0JpaPlatformProvider.instance(), buildJpaPlatformVariation(),
        JPQLGrammar2_0.instance());
  }

  protected JpaPlatformVariation buildJpaPlatformVariation() {
    return new JpaPlatformVariation() {
      @Override
      public AccessType[] getSupportedAccessTypes(JptResourceType resourceType) {
        return GENERIC_SUPPORTED_ACCESS_TYPES;
      }

      @Override
      public Supported getTablePerConcreteClassInheritanceIsSupported() {
        return Supported.MAYBE;
      }

      @Override
      public boolean isJoinTableOverridable() {
        return false;
      }
    };
  }

  private Version buildJpaVersion() {
    return new GenericJpaPlatformFactory.SimpleVersion(JpaFacet.VERSION_2_0.getVersionString());
  }
}
