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

import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableSet;

/**
 * Authentication data, consisting of an access token, a refresh token, an access-token expiration
 * time, and a user email address, each of which may be null, and a set of authorized scopes that is
 * never null but may be empty.
 */
@Immutable
public class OAuthData {
  @Nullable private final String storedEmail;
  @Nullable private final String accessToken;
  @Nullable private final String refreshToken;
  @Nullable private final long accessTokenExpiryTime;
  private final Set<String> storedScopes;

  public OAuthData(
      @Nullable String accessToken, @Nullable String refreshToken, @Nullable String storedEmail,
      @Nullable Set<String> scopes, @Nullable long accessTokenExpiryTime) {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    this.storedEmail = storedEmail;
    this.storedScopes = (scopes == null ? ImmutableSet.<String>of() : scopes);
    this.accessTokenExpiryTime = accessTokenExpiryTime;
  }

  @Nullable
  public String getStoredEmail() {
    return storedEmail;
  }

  @Nullable
  public Set<String> getStoredScopes() {
    return storedScopes;
  }

  @Nullable
  public String getAccessToken() {
    return accessToken;
  }

  @Nullable
  public String getRefreshToken() {
    return refreshToken;
  }

  @Nullable
  public long getAccessTokenExpiryTime() {
    return accessTokenExpiryTime;
  }
}