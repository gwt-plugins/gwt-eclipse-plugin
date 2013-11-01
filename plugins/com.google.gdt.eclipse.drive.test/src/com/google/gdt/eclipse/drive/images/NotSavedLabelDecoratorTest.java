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
package com.google.gdt.eclipse.drive.images;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gdt.eclipse.drive.DrivePlugin;
import com.google.gdt.eclipse.drive.DrivePlugin.ICommandServiceFactory;
import com.google.gdt.eclipse.drive.DrivePlugin.ILogFactory;
import com.google.gdt.eclipse.drive.resources.PendingSaveManager;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.ILog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.ui.commands.ICommandService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit test for {@link NotSavedLabelDecorator}.
 */
@RunWith(JUnit4.class)
public class NotSavedLabelDecoratorTest {
  
  @Mock private IFolder mockFolder;
  @Mock private IFile mockSavedFile;
  @Mock private IFile mockUnsavedFile;
    
  @Mock private PendingSaveManager mockPendingSaveManager;
  @Mock private IDecoration mockDecoration1;
  @Mock private IDecoration mockDecoration2;
  @Mock private IDecoration mockDecoration3;
  
  @Test
  public void testDecorate() {
    MockitoAnnotations.initMocks(this);
    when(mockPendingSaveManager.isUnsaved(mockSavedFile)).thenReturn(false);
    when(mockPendingSaveManager.isUnsaved(mockUnsavedFile)).thenReturn(true);
    DrivePlugin testPlugin =
        new DrivePlugin(
            mockPendingSaveManager,
            new ILogFactory(){
              @Override public ILog get() {
                return null;
              }
            }, 
            new ICommandServiceFactory(){
              @Override public ICommandService get() {
                return null;
              }
            });
    NotSavedLabelDecorator decorator = new NotSavedLabelDecorator(testPlugin);
    decorator.decorate(mockSavedFile, mockDecoration1);
    decorator.decorate(mockFolder, mockDecoration2);
    decorator.decorate(mockUnsavedFile, mockDecoration3);
    verify(mockDecoration1, times(0)).addOverlay(any(ImageDescriptor.class));
    verify(mockDecoration2, times(0)).addOverlay(any(ImageDescriptor.class));
    verify(mockDecoration3).addOverlay(any(ImageDescriptor.class));
  }

}
