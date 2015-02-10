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
package com.google.gdt.eclipse.suite.update.usage;

import com.google.appengine.eclipse.core.ILogGaeStats;
import com.google.gdt.eclipse.suite.update.GdtExtPlugin;

/**
 * This sends the usage stats of datanucleus library versions. Can be extended to add usage stats
 * for other features.
 */
public class LogGaeStats implements ILogGaeStats{
  @Override
  public void sendDatanucleusLibVersionChangedPing(String version) {
    GdtExtPlugin.getAnalyticsPingManager().sendDatanucleusLibVersionChanged(version);
  }
}
