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

import com.google.common.annotations.VisibleForTesting;
import com.google.gdt.eclipse.drive.DrivePlugin;
import com.google.gdt.eclipse.drive.resources.PendingSaveEvent;
import com.google.gdt.eclipse.drive.resources.PendingSaveEventListener;
import com.google.gdt.eclipse.drive.resources.PendingSaveManager;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;

/**
 * A label decorator that marks files saved in Eclipse, but not in Drive.
 */
public class NotSavedLabelDecorator
    extends BaseLabelProvider implements ILightweightLabelDecorator {
  
  private ImageDescriptor unsavedImageDescriptor;
  
  private PendingSaveManager pendingSaveManager;
  
  /**
   * Constructs a {@code NotSavedLabelDecorator} when invoked reflectively during plugin startup.
   */
  public NotSavedLabelDecorator() {
    this(DrivePlugin.getDefault());
    unsavedImageDescriptor =
        DrivePlugin.getDefault().getImageDescriptor(ImageKeys.UNSAVED_ICON);
  }
  
  /**
   * Constructs a {@code NotSavedLabelDecorator}, injecting a specified {@link DrivePlugin}.
   * In production, this constructor is invoked from {@link #NotSavedLabelDecorator()}, with the
   * singleton instance of the production {@code DrivePlugin}. This constructor is also invoked
   * directly from unit tests, with a test mode {@code DrivePlugin}.
   * 
   * @param drivePlugin the specified {@code DrivePlugin}
   */
  @VisibleForTesting
  NotSavedLabelDecorator(DrivePlugin drivePlugin) {
    pendingSaveManager = drivePlugin.getPendingSaveManager();
    pendingSaveManager.addPendingSaveListener(
        new PendingSaveEventListener(){
          @Override public void onPendingSaveEvent(PendingSaveEvent savedEvent) {
            LabelProviderChangedEvent labelProviderEvent =
                new LabelProviderChangedEvent(NotSavedLabelDecorator.this, savedEvent.getFile());
            fireLabelProviderChanged(labelProviderEvent);
          }
        });
  }

  @Override
  public void decorate(Object element, IDecoration decoration) {
    if (element instanceof IFile) {
      IFile file = (IFile) element;
      if (pendingSaveManager.isUnsaved(file)) {
        decoration.addOverlay(unsavedImageDescriptor);
      }
    }
  }
}
