/*******************************************************************************
 * Copyright 2014 Google Inc. All Rights Reserved.
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
package com.google.gcp.eclipse.testing;

import com.google.common.base.Preconditions;

import org.eclipse.core.runtime.IPath;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Utilities for creating files in OSGi or non-OSGi tests. Access is directly to the filesystem,
 * not through the IResource layer.
 */
public class PathTestUtil {
  private PathTestUtil() {} // Non-instantiatable utility class

  /**
   * Creates a new temporary directory for the invoking test.
   */
  public static IPath makeTempDir() throws IOException {
    return org.eclipse.core.runtime.Path.fromOSString(
        Files.createTempDirectory("unittest").toString());
  }

  /**
   * Deletes a folder and all of its contents, deleting symlinks rather than their targets.
   */
  public static void deleteRecursively(IPath path) throws IOException {
    Path directory = FileSystems.getDefault().getPath(path.toOSString());
    Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  /**
   * Creates the named directory, including any needed parent directories.
   *
   * @param pathString the absolute path of the directory to create
   * @throws IOException if creating the directory fails
   */
  public static void makeDir(String pathString) throws IOException {
    makeDir(org.eclipse.core.runtime.Path.fromOSString(pathString));
  }

  /**
   * Creates the named directory, including any needed parent directories.
   *
   * @param path the absolute path of the directory to create
   * @throws IOException if creating the directory fails
   */
  public static void makeDir(IPath path) throws IOException {
    Preconditions.checkArgument(path.isAbsolute());
    Path directory = FileSystems.getDefault().getPath(path.append("dummy").toOSString());
    Files.createDirectories(directory);
  }

  /**
   * Creates an empty file.
   *
   * @param path the absolute path of the file to create
   * @throws IOException if creating the file fails
   */
  public static void touchFile(IPath path) throws IOException {
    Preconditions.checkArgument(path.isAbsolute());
    Path file = FileSystems.getDefault().getPath(path.toOSString());
    Files.createFile(file);
  }

  /**
   * Creates the named file, creating parent directories if needed.
   */
  public static void touch(IPath path) throws IOException {
    Preconditions.checkArgument(path.isAbsolute());
    makeDir(path.removeLastSegments(1));
    Path file = FileSystems.getDefault().getPath(path.toOSString());
    Files.createFile(file);
  }

  /**
   * Creates a file from input string text.
   *
   * @param sourceText the file's contents
   * @param path the absolute path of the file to create
   * @throws IOException if creating the file fails
   */
  public static void createFileFromText(String sourceText, IPath path) throws IOException {
    Preconditions.checkArgument(path.isAbsolute());
    Path file = FileSystems.getDefault().getPath(path.toOSString());
    Files.write(file, sourceText.getBytes());
  }
}
