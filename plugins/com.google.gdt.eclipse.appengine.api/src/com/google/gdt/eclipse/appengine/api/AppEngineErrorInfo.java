/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.appengine.api;

import java.util.List;

/**
 * Class used for parsing the App Engine IOException messages using Gson.
 */
public class AppEngineErrorInfo {

  /**
   * Class used for parsing the "errors" member of the App Engine IOException message.
   */
    public static class Errors {
      private String domain;
      private String message;
      private String reason;
      private String debugInfo;

      public String getDebugInfo() {
        return debugInfo;
      }

      public String getDomain() {
        return domain;
      }
      
      public String getMessage() {
        return message;
      }

      public String getReason() {
        return reason;
      }
    }

    private int code;
    private List<Errors> errors;
    private String message;

    public int getCode() {
      return code;
    }

    public List<Errors>  getErrors() {
      return errors;
    }

  public String getMessage() {
    return message;
  }

}
