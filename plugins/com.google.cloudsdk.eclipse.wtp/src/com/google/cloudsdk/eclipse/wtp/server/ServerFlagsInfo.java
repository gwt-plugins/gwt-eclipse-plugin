/*******************************************************************************
 * Copyright 2015 Google Inc. All Rights Reserved.
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
package com.google.cloudsdk.eclipse.wtp.server;

import java.util.ArrayList;
import java.util.List;

/**
 * Class used to get the list of "gcloud app run" flags.
 */
public class ServerFlagsInfo {
  /**
   * Class holding "name", "description" and "supportsArgument" members of "flags" in the
   * server-flags.json.
   */
  public static class Flag {
    private String name;
    private String description;
    private boolean supportsArgument;

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }

    public boolean getSupportsArgument() {
      return supportsArgument;
    }
  }

  private List<Flag> flags = new ArrayList<Flag>();

  public List<Flag> getFlags() {
    return flags;
  }

}
