/*******************************************************************************
 * Copyright 2014 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.login.common;

/**
 * Presents a common API, implementable on a variety of platforms, for storing a particular user's
 * {@link OAuthData} object persistently, retrieving it, and clearing it.
 */
public interface OAuthDataStore {
  
  /**
   * Stores a specified {@link OAuthData} object persistently.
   * 
   * @param credentials the specified {@code Credentials object}
   */
  void saveOAuthData(OAuthData credentials);
  
  /**
   * Retrieves the persistently stored {@link OAuthData} object, if any.
   * 
   * @return
   *     if there is a persistently stored {@code OAuthData} object, that object; otherwise an
   *     {@code OAuthData} object all of whose getters return {@code null}
   */
  OAuthData loadOAuthData();
  
  /**
   * Clears the persistently stored {@link OAuthData} object, if any.
   */
  void clearStoredOAuthData();
}