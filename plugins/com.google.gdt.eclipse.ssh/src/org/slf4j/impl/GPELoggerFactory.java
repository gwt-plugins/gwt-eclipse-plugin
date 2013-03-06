/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple logger factory
 */
public class GPELoggerFactory implements ILoggerFactory {

  // TODO(appu): since we're redirecting to the eclipse log anyway, perhaps we
  // don't need this map and just use a single instance
  private Map<String, GPELogger> loggerMap;

  public GPELoggerFactory() {
    loggerMap = new HashMap<String, GPELogger>();
  }

  public Logger getLogger(String name) {
    synchronized (loggerMap) {
      if (!loggerMap.containsKey(name)) {
        loggerMap.put(name, new GPELogger(name));
      }
    }

    return loggerMap.get(name);
  }
}
