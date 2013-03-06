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
package com.google.gdt.eclipse.login.update;

import com.google.gdt.eclipse.core.update.internal.core.UpdateQueryArgContributor;
import com.google.gdt.eclipse.login.GoogleLogin;

import org.eclipse.core.resources.IProject;

/**
 * Class that contributes a URL argument to GPE's update ping to track the number
 * of users who are signed in.
 */
public class SignedInUpdateQueryArgContributor implements UpdateQueryArgContributor {

  public String getContribution(IProject project) {
    if (GoogleLogin.getInstance().isLoggedIn()) {
      return "&isSignedIn=true";
    } else {
      return "&isSignedIn=false";
    }
  }

}
