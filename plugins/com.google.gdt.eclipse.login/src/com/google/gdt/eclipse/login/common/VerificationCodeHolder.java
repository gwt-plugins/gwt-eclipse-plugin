/*******************************************************************************
 * Copyright 2014 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.login.common;

/**
 * Holder for a Google OAuth verification code and the redirect URL used to create it.
 */
public class VerificationCodeHolder {
  private final String verificationCode;
  private final String redirectUrl;

  public VerificationCodeHolder(String verificationCode, String redirectUrl) {
    this.verificationCode = verificationCode;
    this.redirectUrl = redirectUrl;
  }

  public String getRedirectUrl() {
    return redirectUrl;
  }

  public String getVerificationCode() {
    return verificationCode;
  }
}
