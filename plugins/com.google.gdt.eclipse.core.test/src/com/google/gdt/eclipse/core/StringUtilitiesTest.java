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
package com.google.gdt.eclipse.core;

import junit.framework.TestCase;

/**
 * Tests {@link StringUtilities}
 */
public class StringUtilitiesTest extends TestCase {
  public void testQuote() {
    assertEquals("\"simple\"", StringUtilities.quote("simple"));
    // quote(\ ") -> \ \"
    assertEquals("\"\\ \\\"\"", StringUtilities.quote("\\ \""));
    assertEquals("\"escaped\\\"quote\"",
        StringUtilities.quote("escaped\"quote"));
    assertEquals("\"unescaped\\backslash\"",
        StringUtilities.quote("unescaped\\backslash"));
    assertEquals("\"\"", StringUtilities.quote(""));
  }
}
