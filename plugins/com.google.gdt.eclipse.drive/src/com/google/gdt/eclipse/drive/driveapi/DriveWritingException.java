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
package com.google.gdt.eclipse.drive.driveapi;

import java.io.IOException;

import javax.annotation.Nullable;

/**
 * An exception thrown by the failure of a write to Drive, either because of an internal error or
 * an attempt to save a script with syntax errors. In the case of a script with syntax errors, the
 * exception may contain a JavaScript error message.
 */
@SuppressWarnings("serial")
public class DriveWritingException extends Exception {
  
  private static final String JAVA_SCRIPT_ERROR_FINGERPRINT =
      "cause: com.google.apps.drive.rosy.exceptions.BadRequestException";
  
  private static final String MESSAGE_FOR_SERVER_ERROR =
      "Server error while storing project in Drive.";
  private static final String MESSAGE_FOR_JAVA_SCRIPT_ERROR =
      "Attempt to store a script containing one or more errors.";
  
  private final String javaScriptError;
  
  public DriveWritingException(IOException reportedByDrive) {
    this(reportedByDrive, extractJavaScriptError(reportedByDrive));
  }
  
  private DriveWritingException(IOException reportedByDrive, @Nullable String javaScriptError) {
    super(
        javaScriptError == null ? MESSAGE_FOR_SERVER_ERROR : MESSAGE_FOR_JAVA_SCRIPT_ERROR,
        reportedByDrive);
    this.javaScriptError = javaScriptError;
  }
  
  @Nullable
  private static String extractJavaScriptError(IOException reportedByDrive) {
    String messageFromServer = reportedByDrive.getMessage();
    int fingerprintIndex = messageFromServer.indexOf(JAVA_SCRIPT_ERROR_FINGERPRINT);
    if (fingerprintIndex == -1) {
      return null;
    }
    // TODO(nhcohen): If the message in the IOException error message evolves to include details
    // about the JavaScript error found on the server, return a string describing those details, and
    // modify DriveEclipseProjectMediator.writeEclipseProjectToDrive accordingly.
    return "(details unavailable)";
  }
  
  @Nullable
  public String getJavaScriptError() {
    return javaScriptError;
  }

}
