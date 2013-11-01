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
package com.google.gdt.eclipse.drive.editors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit test for {@link JavaScriptIdentifierNames}.
 */
@RunWith(JUnit4.class)
public class JavaScriptIdentifierNamesTest {
  
  private static final String CAN_START_IDENTIFIERS =
      "_$AZaz\u01C5\u1FCC\u02B0\uFF9F\u00AA\uFFDC\u16EE\uA6EF";
    // _ $ A Z a z ǅ ῼ ʰ ﾟ ª ￜ ᛮ ꛯ 
  private static final String CAN_OCCUR_IN_IDENTIFIERS =
      CAN_START_IDENTIFIERS +  "09\u0300\uFE26\u0903\uABEC\u005F\uFF3F";
      // 0 9 <combining grave accent> <combining conjoining macron> ः ꯬ _ ＿
  private static final String CANNOT_OCCUR_IN_IDENTIFIERS = "!\"#%&'()*+,-./:;<=>?@[\\]^`{|}~";
  private static final String CANNOT_START_IDENTIFIERS = CANNOT_OCCUR_IN_IDENTIFIERS + "09";
  
  @Test
  public void testIsIdentifierNameStartingCharacter_yes() {
    for (char c : CAN_START_IDENTIFIERS.toCharArray()) {
      assertTrue("" + c, JavaScriptIdentifierNames.isIdentifierNameStartingCharacter(c));
    }
  }
  
  @Test
  public void testIsIdentifierNameStartingCharacter_no() {
    for (char c : CANNOT_START_IDENTIFIERS.toCharArray()) {
      assertFalse("" + c, JavaScriptIdentifierNames.isIdentifierNameStartingCharacter(c));
    }
  }
  
  @Test
  public void testIsIdentifierNameCharacter_yes() {
    for (char c : CAN_OCCUR_IN_IDENTIFIERS.toCharArray()) {
      assertTrue("" + c, JavaScriptIdentifierNames.isIdentifierNameCharacter(c));
    }
  }
  
  @Test
  public void testIsIdentifierNameCharacter_no() {
    for (char c : CANNOT_OCCUR_IN_IDENTIFIERS.toCharArray()) {
      assertFalse("" + c, JavaScriptIdentifierNames.isIdentifierNameCharacter(c));
    }
  }

}
