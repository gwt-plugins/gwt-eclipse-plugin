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

import com.google.appengine.eclipse.webtools.facet.JpaFacetHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jpt.common.core.JptResourceType;
import org.eclipse.jpt.jpa.core.JpaPlatform;
import org.eclipse.jpt.jpa.core.JpaPlatform.Config;
import org.eclipse.jpt.jpa.core.JpaPlatform.Version;
import org.eclipse.jpt.jpa.core.JpaPlatformFactory;
import org.eclipse.jpt.jpa.core.JpaPlatformVariation;
import org.eclipse.jpt.jpa.core.context.AccessType;
import org.eclipse.jpt.jpa.core.internal.GenericJpaPlatform;
import org.eclipse.jpt.jpa.core.internal.GenericJpaPlatformFactory;
import org.eclipse.jpt.jpa.core.internal.JpaAnnotationProvider;
import org.eclipse.jpt.jpa.core.internal.jpa2.GenericJpaAnnotationDefinitionProvider2_0;
import org.eclipse.jpt.jpa.core.internal.jpa2.GenericJpaFactory2_0;
import org.eclipse.jpt.jpa.core.internal.jpa2.GenericJpaPlatformProvider2_0;
import org.eclipse.jpt.jpa.core.jpa2.JpaProject2_0;
import org.eclipse.persistence.jpa.jpql.parser.JPQLGrammar2_0;

/**
 * {@link JpaPlatformFactory} implemented currently as generic JPA factory for version 2.0.
 */
@SuppressWarnings("restriction")
public class GaeJpaPlatformFactory implements JpaPlatformFactory {
  /*
   * The break in API compatibility in Eclipse 4.3 makes this code non-compilable by Eclipse 4.2,
   * (the first parameter to the ctor was changed from a String to a JpaPlatform.Config, which did
   * not exist in 4.2) and reflection must be used to compile the 4.2-compatible code in 4.3+.
   *
   * This can safely built by 4.3+ and deployed by for 4.2 because the buildJpaPlatform(Config)
   * method will never be invoked by the 4.2 framework.
   */
  // Eclipse 4.3+ override
  @Override
  public JpaPlatform buildJpaPlatform(Config config) {
    return new GenericJpaPlatform(
        config,
        buildJpaVersion(),
        new GenericJpaFactory2_0(),
        new JpaAnnotationProvider(GenericJpaAnnotationDefinitionProvider2_0.instance()),
        GenericJpaPlatformProvider2_0.instance(),
        buildJpaPlatformVariation(),
        JPQLGrammar2_0.instance());
  }

  // Eclipse 4.2 override
  public JpaPlatform buildJpaPlatform(String id) {
    try {
      return JpaFacetHelper.newJunoGenericJpaPlatform(
          id,
          JpaFacetHelper.buildJpaVersion(),
          new GenericJpaFactory2_0(),
          new JpaAnnotationProvider(JpaFacetHelper.getJpaAnnotationDefinitionProvider()),
          JpaFacetHelper.getJpaPlatformProvider(),
          buildJpaPlatformVariation(),
          JPQLGrammar2_0.instance());
    } catch (CoreException e) {
      // TODO: Restore logging when merged with the c.g.appengine.eclipse.wtp.jpa plug-in.
//      AppEngineJpaPlugin.logMessage("Unable to build the JPA platform.", e);
    }
    return null;
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
    return new GenericJpaPlatformFactory.GenericJpaPlatformVersion(
        JpaProject2_0.FACET_VERSION_STRING);
  }
}
