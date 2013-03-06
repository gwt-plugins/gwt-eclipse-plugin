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
package com.google.gwt.eclipse.core.speedtracer;

import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gwt.eclipse.core.preferences.GWTPreferences;

import org.eclipse.core.runtime.Path;

import java.io.File;

/**
 * Removes the artifacts generated for Speed Tracer launches.
 */
public class SpeedTracerArtifactsRemover {

  private final File warOutFolder;

  public SpeedTracerArtifactsRemover(File warOutFolder) {
    this.warOutFolder = warOutFolder;
  }

  public void removeAll() {
    removeSymbolMaps();
    removeSymbolManifest();
    removeGenFiles();
  }

  private void removeGenFiles() {
    File genFilesFolder = GWTPreferences.computeSpeedTracerGeneratedFolderPath(
      new Path(warOutFolder.getAbsolutePath())).toFile();
    if (genFilesFolder.exists()) {
      ResourceUtils.deleteFileRecursively(genFilesFolder);
    }
  }

  private void removeSymbolManifest() {
    File symbolManifestFile = SymbolManifestGenerator.getSymbolManifestFile(warOutFolder);
    if (symbolManifestFile.exists()) {
      symbolManifestFile.delete();
    }
  }

  private void removeSymbolMaps() {
    File symbolMapsFolder = SymbolManifestGenerator.getSymbolMapsFolder(warOutFolder);
    if (symbolMapsFolder.exists()) {
      ResourceUtils.deleteFileRecursively(symbolMapsFolder);
    }
  }
}
