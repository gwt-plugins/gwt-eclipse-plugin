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

import javax.annotation.Nullable;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;

/**
 * Presents a common API, implementable on a variety of platforms, for specific user interactions
 * that are part of the login and logout processes.
 */
public interface UiFacade {
  
  /**
   * Initiates a browser-based user-facing interaction with the OAuth server that culminates in the
   * delivery of an OAuth verification code from the OAuth server, and returns that code. An
   * implementation of this method may obtain its return value, for example, directly from the
   * browser, or from a widget into which the user is asked to copy and paste a verification code
   * displayed by the browser.
   *  
   * @param title a title for the widget containing the browser display
   * @param authCodeRequestUrl
   *     a {@link GoogleAuthorizationCodeRequestUrl} representing the HTTP request issued to
   *     initiate the interaction
   * @return the verification code
   */
  @Nullable
  String obtainVerificationCodeFromUserInteraction(
      String title, GoogleAuthorizationCodeRequestUrl authCodeRequestUrl);
  
  /**
   * Displays an error dialog with a specified title and message and blocks until the user dismisses
   * it.
   * 
   * @param title the specified title
   * @param message the specified message
   */
  void showErrorDialog(String title, String message);
  
  /**
   * Displays a question dialog with a specified title and yes/no question, blocks until the user
   * responds, and returns the user's response.
   * 
   * @param title the specified title
   * @param message the specified yes/no question
   * @return {@code true} for a yes response, {@code false} for a no response
   */
  boolean askYesOrNo(String title, String message);
  
  /**
   * Causes a widget that displays logged-in/logged-out status to query the current status and
   * to refresh itself accordingly.
   */
  void notifyStatusIndicator();
}