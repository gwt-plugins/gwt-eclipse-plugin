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
 * Presents a common API, implementable on a variety of platforms, for making log entries.
 */
public interface LoggerFacade {

  /**
   * Create an error log entry with a specified message and a specified {@link Throwable}.
   * 
   * @param message the specified message
   * @param t the specified {@code Throwable}
   */
  void logError(String message, Throwable t);

  /**
   * Create a warning log entry with a specified message.
   * 
   * @param message the specified message
   */
  void logWarning(String message);

}
