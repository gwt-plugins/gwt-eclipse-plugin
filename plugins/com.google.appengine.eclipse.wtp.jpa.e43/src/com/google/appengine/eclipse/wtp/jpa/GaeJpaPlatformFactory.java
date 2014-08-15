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
import org.eclipse.jpt.jpa.core.JpaPlatform;
import org.eclipse.jpt.jpa.core.JpaPlatform.Config;
import org.eclipse.jpt.jpa.core.JpaPlatform.Version;
import org.eclipse.jpt.jpa.core.JpaPlatformFactory;
import org.eclipse.jpt.jpa.core.JpaPlatformVariation;
import org.eclipse.jpt.jpa.core.JpaProject;
import org.eclipse.jpt.jpa.core.context.AccessType;
import org.eclipse.jpt.jpa.core.context.persistence.Persistence;
import org.eclipse.jpt.jpa.core.internal.GenericJpaPlatform;
import org.eclipse.jpt.jpa.core.internal.GenericJpaPlatformFactory;
import org.eclipse.jpt.jpa.core.internal.JpaAnnotationProvider;
import org.eclipse.jpt.jpa.core.internal.jpa2.GenericJpaAnnotationDefinitionProvider2_0;
import org.eclipse.jpt.jpa.core.internal.jpa2.GenericJpaFactory2_0;
import org.eclipse.jpt.jpa.core.internal.jpa2.GenericJpaPlatformProvider2_0;
import org.eclipse.jpt.jpa.core.jpa2.JpaProject2_0;
import org.eclipse.persistence.jpa.jpql.parser.JPQLGrammar2_0;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * {@link JpaPlatformFactory} implemented currently as generic JPA factory for version 2.0.
 */
@SuppressWarnings("restriction")
public class GaeJpaPlatformFactory implements JpaPlatformFactory {

  protected static Persistence getPersistence(JpaProject jpaProject) {
    Method m = null;
    try {
      // Method name on Eclipse 4.3
      m = JpaProject.class.getDeclaredMethod("getContextModelRoot");
    } catch (NoSuchMethodException e) {
      // Ignore
    }

    if (m == null) {
      try {
        // Method name on Eclipse 4.4+
        m = JpaProject.class.getDeclaredMethod("getContextRoot");
      } catch (NoSuchMethodException e) {
        AppEngineJpaPlugin.logMessage(e);
      }
    }

    try {
      return (Persistence) m.invoke(jpaProject);
    } catch (IllegalAccessException e) {
      // Should never happen.
      AppEngineJpaPlugin.logMessage(e);
    } catch (IllegalArgumentException e) {
      // Should never happen.
      AppEngineJpaPlugin.logMessage(e);
    } catch (InvocationTargetException e) {
      // Should never happen.
      AppEngineJpaPlugin.logMessage(e);
    }

    // Should never reach this point
    return null;
  }

  @Override
  public JpaPlatform buildJpaPlatform(Config config) {
    return new GenericJpaPlatform(config, buildJpaVersion(), new GenericJpaFactory2_0(),
        new JpaAnnotationProvider(GenericJpaAnnotationDefinitionProvider2_0.instance()),
        GenericJpaPlatformProvider2_0.instance(), buildJpaPlatformVariation(),
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
    return new GenericJpaPlatformFactory.GenericJpaPlatformVersion(
        JpaProject2_0.FACET_VERSION_STRING);
  }
}
