/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.appengine.api;

import com.google.gdt.eclipse.appengine.api.AppEngineErrorInfo.Errors;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import junit.framework.TestCase;

import org.junit.Assert;

import java.util.List;

/**
 * Unit test for {@link AppEngineErrorInfo}.
 */
public class AppEngineErrorInfoTest extends TestCase {
  private static final int SERVER_CODE = 100;
  private static final String SERVER_MESSAGE = "server message";
  private static final String SERVER_ERROR_DOMAIN = "server error domain";
  private static final String SERVER_ERROR_MESSAGE = "server error message";
  private static final String SERVER_ERROR_REASON = "server error reason";
  private static final String SERVER_ERROR_DEBUG_INFO = "server error debug info";
  private static final String JSON_STRING = "{\n\"code\" : " + SERVER_CODE
      + ",\n\"errors\" : [ {\n\"domain\" : \"" + SERVER_ERROR_DOMAIN + "\",\n\"message\" : \""
      + SERVER_ERROR_MESSAGE + "\",\n\"reason\" :\"" + SERVER_ERROR_REASON
      + "\",\n\"debugInfo\" : \"" + SERVER_ERROR_DEBUG_INFO
      + "\"\n} ],\n\"message\" : \"" + SERVER_MESSAGE + "\"\n}";

  /**
   * Tests that the AppEngineErrorInfo can be correctly generated from an App Engine API server json
   * string.
   */
  public void testJsonGeneration() {
    Gson gson = new GsonBuilder().create();
    AppEngineErrorInfo errorInfo = gson.fromJson(JSON_STRING, AppEngineErrorInfo.class);
    Assert.assertEquals(SERVER_CODE, errorInfo.getCode());
    Assert.assertEquals(SERVER_MESSAGE, errorInfo.getMessage());

    List<Errors> errors = errorInfo.getErrors();
    Assert.assertNotNull(errors);
    Assert.assertEquals(1, errors.size());
    Errors errorItem = errors.get(0);
    Assert.assertNotNull(errorItem);
    Assert.assertEquals(SERVER_ERROR_DOMAIN, errorItem.getDomain());
    Assert.assertEquals(SERVER_ERROR_MESSAGE, errorItem.getMessage());
    Assert.assertEquals(SERVER_ERROR_REASON, errorItem.getReason());
    Assert.assertEquals(SERVER_ERROR_DEBUG_INFO, errorItem.getDebugInfo());
  }
}
