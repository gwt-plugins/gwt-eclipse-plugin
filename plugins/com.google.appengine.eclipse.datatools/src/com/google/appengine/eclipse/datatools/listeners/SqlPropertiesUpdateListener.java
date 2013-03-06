/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
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
package com.google.appengine.eclipse.datatools.listeners;

import com.google.appengine.eclipse.core.datatools.GaeSqlToolsExtension;
import com.google.appengine.eclipse.core.datatools.SqlConnectionProperties;
import com.google.appengine.eclipse.datatools.utils.DatatoolsUtils;

/**
 * Uses My SQL driver type and definition to connect to Cloud SQL, as well as local MySQL.
 */
public class SqlPropertiesUpdateListener implements GaeSqlToolsExtension {
  
  public void updateConnectionProperties(SqlConnectionProperties connectionProperties) {
    DatatoolsUtils.createProfile(connectionProperties);
  }
}
