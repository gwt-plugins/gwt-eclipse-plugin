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

import static org.junit.Assert.assertEquals;

import com.google.common.testing.EqualsTester;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit test for {@link DriveScriptInfo}.
 */
@RunWith(JUnit4.class)
public class DriveScriptInfoTest {

  private static final String IMPORT_NAME = "import name";
  private static final String DOCUMENT_ID = "document ID";
  private static final String TYPE = "type";
  private static final String CONTENTS = "contents";
  
  @Test
  public void testConstructorAndGetters() {
    DriveScriptInfo info = new DriveScriptInfo(IMPORT_NAME, DOCUMENT_ID, TYPE, CONTENTS);
    assertEquals(IMPORT_NAME, info.getImportName());
    assertEquals(DOCUMENT_ID, info.getDocumentId());
    assertEquals(TYPE, info.getType());
    assertEquals(CONTENTS, info.getContents());
  }
  
  @Test
  public void testEqualsAndHashCode() {
    DriveScriptInfo info = new DriveScriptInfo(IMPORT_NAME, DOCUMENT_ID, TYPE, CONTENTS);
    new EqualsTester()
       .addEqualityGroup(info, info, new DriveScriptInfo(IMPORT_NAME, DOCUMENT_ID, TYPE, CONTENTS))
       .addEqualityGroup(new DriveScriptInfo(IMPORT_NAME + "x", DOCUMENT_ID, TYPE, CONTENTS))
       .addEqualityGroup(new DriveScriptInfo(IMPORT_NAME, DOCUMENT_ID + "x", TYPE, CONTENTS))
       .addEqualityGroup(new DriveScriptInfo(IMPORT_NAME, DOCUMENT_ID, TYPE + "x", CONTENTS))
       .addEqualityGroup(new DriveScriptInfo(IMPORT_NAME, DOCUMENT_ID, TYPE, CONTENTS + "x"))
       .testEquals();
  }

}
