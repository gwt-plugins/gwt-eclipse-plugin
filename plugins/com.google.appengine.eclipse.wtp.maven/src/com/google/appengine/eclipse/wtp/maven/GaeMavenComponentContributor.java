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
package com.google.appengine.eclipse.wtp.maven;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.m2e.core.internal.embedder.DefaultMavenComponentContributor;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;

/**
 *
 */
@SuppressWarnings("restriction") // DefaultMavenComponentContributor
public class GaeMavenComponentContributor extends DefaultMavenComponentContributor {

  @Override
  public void contribute(IMavenComponentBinder binder) {
    System.out.println("com.google.appengine.eclipse.wtp.maven.GaeMavenComponentContributor.contribute(IMavenComponentBinder)");
    Map<String, IConfigurationElement> configuratorMap =  LifecycleMappingFactory.getProjectConfiguratorExtensions();
    
    super.contribute(binder);
    // A series of calls like:
    // binder.bind(RepositoryListener.class, EclipseRepositoryListener.class, EclipseRepositoryListener.ROLE_HINT);
  }

}
