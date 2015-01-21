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
package com.google.appengine.eclipse.wtp.utils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Various methods to work with IO operations/streams.
 */
public class IOUtils {
  private static final int DEFAULT_BUFFER_SIZE = 4096;

  /**
   * Closes given {@link Closeable} without throwing any exceptions.
   */
  public static void closeQuietly(Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (Throwable e) {
        // do nothing
      }
    }
  }

  /**
   * Copies all available data from <code>input</code> to <code>output</code>.
   */
  public static long copy(Reader input, Writer output) throws IOException {
    char[] buffer = new char[DEFAULT_BUFFER_SIZE];
    long count = 0;
    int n = 0;
    while (-1 != (n = input.read(buffer))) {
      output.write(buffer, 0, n);
      count += n;
    }
    return count;
  }

  /**
   * Reads an {@link InputStream} into {@link String}.
   */
  public static String readString(InputStream is) throws IOException {
    try {
      StringWriter w = new StringWriter();
      copy(new InputStreamReader(is), w);
      return w.toString();
    } finally {
      closeQuietly(is);
    }
  }
}
