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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;

/**
 * Unit test for {@link DriveWritingException}.
 */
@RunWith(JUnit4.class)
public class DriveWritingExceptionTest {
  
  private static final String IO_EXCEPTION_MESSAGE_WITH_JAVA_SCRIPT_ERROR =
      "blah blah blah cause: com.google.apps.drive.rosy.exceptions.BadRequestException blah blah";

  private static final String IO_EXCEPTION_MESSAGE_WITHOUT_JAVA_SCRIPT_ERROR = "blah blah blah";
  
  @Test
  public void javaScriptError() {
    IOException serverException = new IOException(IO_EXCEPTION_MESSAGE_WITH_JAVA_SCRIPT_ERROR);
    DriveWritingException exceptionUnderTest = new DriveWritingException(serverException);
    assertNotNull(exceptionUnderTest.getJavaScriptError());
  }
  
  @Test
  public void nonJavaScriptError() {
    IOException serverException = new IOException(IO_EXCEPTION_MESSAGE_WITHOUT_JAVA_SCRIPT_ERROR);
    DriveWritingException exceptionUnderTest = new DriveWritingException(serverException);
    assertNull(exceptionUnderTest.getJavaScriptError());
  }
  
}
