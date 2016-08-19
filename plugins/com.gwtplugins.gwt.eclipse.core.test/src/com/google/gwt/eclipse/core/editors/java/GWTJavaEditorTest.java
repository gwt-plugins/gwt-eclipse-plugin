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
package com.google.gwt.eclipse.core.editors.java;

import com.google.gwt.eclipse.core.test.AbstractGWTPluginTestCase;

import org.eclipse.jdt.internal.ui.actions.CompositeActionGroup;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

import java.lang.reflect.Field;

/**
 * Test cases for the {@link GWTJavaEditor} class.
 */
@SuppressWarnings("restriction")
public class GWTJavaEditorTest extends AbstractGWTPluginTestCase {

  /**
   * Verify that {@link org.eclipse.jdt.internal.ui.actions.CompositeActionGroup}
   * contains the private fields we access reflectively in {@link GWTJavaEditor}.
   * 
   * @throws NoSuchFieldException
   * @throws SecurityException
   */
  public void testCompositeActionGroupPrivateFields() throws SecurityException,
      NoSuchFieldException {
    Field groupsField = CompositeActionGroup.class.getDeclaredField("fGroups");
    groupsField.setAccessible(true);
  }

  /**
   * Verify that {@link org.eclipse.jdt.internal.ui.javaeditor.JavaEditor}
   * contains the private fields we access reflectively in {@link GWTJavaEditor}.
   * 
   * @throws NoSuchFieldException
   * @throws SecurityException
   */
  public void testJavaEditorPrivateFields() throws SecurityException,
      NoSuchFieldException {
    Field contextMenuField = JavaEditor.class.getDeclaredField("fContextMenuGroup");
    contextMenuField.setAccessible(true);
  }
}
