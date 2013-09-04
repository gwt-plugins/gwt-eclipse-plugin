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

import com.google.appengine.eclipse.wtp.AppEnginePlugin;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jpt.jpa.core.JpaProjectManager;
import org.eclipse.ui.IStartup;

/**
 * An {@link IStartup} class for invoking a workaround code due to bugs in Eclipse 4.2.
 */
public final class JpaWorkarounds implements IStartup {

  @Override
  public void earlyStartup() {
    // BUG IN JPA: JpaProject models construct lazily and using async workspace job. If we do any
    // change to JPA project before JpaProject model is created then we'll get an error attempting
    // to add JPA resource twice. The workaround is to schedule it now.
    JpaProjectManager jpaProjectManager = (JpaProjectManager) ResourcesPlugin.getWorkspace().getAdapter(
        JpaProjectManager.class);
    try {
      jpaProjectManager.waitToGetJpaProjects();
    } catch (InterruptedException e) {
      AppEnginePlugin.logMessage(e);
    }
  }
}
