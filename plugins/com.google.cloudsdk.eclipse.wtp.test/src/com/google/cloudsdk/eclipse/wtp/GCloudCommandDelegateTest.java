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
package com.google.cloudsdk.eclipse.wtp;

import junit.framework.TestCase;

/**
 * Unit tests for {@link GCloudCommandDelegate}
 */
public class GCloudCommandDelegateTest extends TestCase {
  public void testIsComponentInstalled() {
    String output1 =  createOutput("Installed");
    assertTrue(GCloudCommandDelegate.isComponentInstalled(output1,
        GCloudCommandDelegate.APP_ENGINE_COMPONENT_NAME));

    String output2 =  createOutput("Not Installed");
    assertFalse(GCloudCommandDelegate.isComponentInstalled(output2,
        GCloudCommandDelegate.APP_ENGINE_COMPONENT_NAME));

    String output3 =  createOutput("Update Available");
    assertTrue(GCloudCommandDelegate.isComponentInstalled(output3,
        GCloudCommandDelegate.APP_ENGINE_COMPONENT_NAME));

    String output4 = "Some error occured";
    assertFalse(GCloudCommandDelegate.isComponentInstalled(output4,
        GCloudCommandDelegate.CLOUD_SDK_COMPONENT_NAME));

    assertFalse(GCloudCommandDelegate.isComponentInstalled(output1,
        "Invalid component name"));
  }

  private String createOutput(String status) {
    return "------------------------------------------------------------------------------------\n"
        + "|                                          Individual Components                   |\n"
        + "|----------------------------------------------------------------------------------|\n"
        + "| Status           | Name                                          | ID |     Size |\n"
        + "|------------------+-----------------------------------------------+----------------\n"
        + "| " + status + " | App Engine Command Line Interface (Preview)   | app  |   < 1 MB |\n"
        + "| Update Available | BigQuery Command Line Tool                    | bq |   < 1 MB |\n"
        + "------------------------------------------------------------------------------------";
  }
}
