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
package com.google.appengine.eclipse.wtp.maven;

import org.eclipse.m2e.wtp.WTPProjectConfigurator;

/**
 * A {@link WTPProjectConfigurator} corresponding to the {@code endpoints_get_discovery_doc} goal.
 * This class adds no behavior to that inherited from  {@code WTPProjectConfigurator}, but exists to
 * define an action to which lifecycle-mapping-metadata.xml binds the
 * {@code endpoints_get_discovery_doc} goal.
 */
@SuppressWarnings("restriction") // WTPProjectConfigurator
public class EndpointsDiscoveryConfigurator extends WTPProjectConfigurator {

}
