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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit test for {@link DriveQueries}.
 */
@RunWith(JUnit4.class)
public class DriveQueriesTest {
  
  @Test
  public void testMimeTypeQuery() {
    assertEquals("mimeType='test-mime-type'", DriveQueries.mimeTypeQuery("test-mime-type", false));
    assertEquals(
        "mimeType='test-mime-type' and trashed=false",
        DriveQueries.mimeTypeQuery("test-mime-type", true));
  }

}
