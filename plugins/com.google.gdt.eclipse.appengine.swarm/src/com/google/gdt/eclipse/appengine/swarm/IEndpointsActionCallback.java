/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gdt.eclipse.appengine.swarm;

import org.eclipse.core.resources.IProject;

/**
 * Callback interface for those implementers that want to know about the
 * exceution of Cloud Endpoint-related actions.
 */
public interface IEndpointsActionCallback {
  void onGenerateEndpointClass(IProject project);

  void onGenerateAppEngineBackend(IProject project);

  void onGenerateAppEngineConnectedAndroidProject(IProject project);
}
