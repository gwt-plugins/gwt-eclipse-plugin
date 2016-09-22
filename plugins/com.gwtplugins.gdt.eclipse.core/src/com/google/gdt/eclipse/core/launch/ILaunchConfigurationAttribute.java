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
package com.google.gdt.eclipse.core.launch;

/*
 * TODO: move GWTLaunchConfiguration to use this
 */
/**
 * An attribute that can be read or written to a launch configuration that also
 * has some default value.
 */
public interface ILaunchConfigurationAttribute {

  /**
   * @return the default value, or null if there is no constant default value
   */
  Object getDefaultValue();

  String getQualifiedName();
}
