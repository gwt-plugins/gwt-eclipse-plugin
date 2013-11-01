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
package com.google.gdt.eclipse.drive.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import com.google.common.testing.EqualsTester;

import org.eclipse.core.resources.IFile;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit test for {@link PendingSaveEvent}.
 */
@RunWith(JUnit4.class)
public class PendingSaveEventTest {
  
  @Mock private IFile mockFile1;
  @Mock private IFile mockFile2;
  
  @Test
  public void testConstructorAndGetters() {
    MockitoAnnotations.initMocks(this);
    PendingSaveEvent event1 = new PendingSaveEvent(mockFile1, true);
    assertSame(mockFile1, event1.getFile());
    assertEquals(true, event1.isUnsaved());
    PendingSaveEvent event2 = new PendingSaveEvent(mockFile1, false);
    assertSame(mockFile1, event2.getFile());
    assertEquals(false, event2.isUnsaved());
  }
  
  @Test
  public void testEquals() {
    MockitoAnnotations.initMocks(this);
    PendingSaveEvent event1a = new PendingSaveEvent(mockFile1, true);
    PendingSaveEvent event1b = new PendingSaveEvent(mockFile1, true);
    PendingSaveEvent event2 = new PendingSaveEvent(mockFile1, false);
    PendingSaveEvent event3 = new PendingSaveEvent(mockFile2, true);
    new EqualsTester()
        .addEqualityGroup(event1a, event1a, event1b)
        .addEqualityGroup(event2)
        .addEqualityGroup(event3)
        .testEquals();
  }

}
