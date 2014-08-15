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

import com.google.common.base.Objects;

import org.eclipse.core.resources.IFile;

/**
 * An event indicating a change in the unsaved status of an {@link IFile}. The unsaved status of a
 * file becomes true when it is saved in Eclipse, but not yet saved in Drive, and false once it is
 * saved in Drive. (When a project is first imported, the unsaved status of each of its files is
 * false.
 */
public class PendingSaveEvent {
  
  private IFile file;
  private boolean unsaved;
  
  public PendingSaveEvent(IFile file, boolean unsaved) {
    this.file = file;
    this.unsaved = unsaved;
  }

  public IFile getFile() {
    return file;
  }

  public boolean isUnsaved() {
    return unsaved;
  }
  
  @Override
  public int hashCode() {
    return Objects.hashCode(file, unsaved);
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof PendingSaveEvent) {
      PendingSaveEvent that = (PendingSaveEvent) obj;
      return this.unsaved == that.unsaved && this.file == that.file;
    } else {
      return false;
    }
  }

}
