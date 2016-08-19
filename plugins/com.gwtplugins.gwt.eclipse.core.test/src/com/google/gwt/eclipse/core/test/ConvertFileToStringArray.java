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
package com.google.gwt.eclipse.core.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a file from disk and converts it into a String array assignment that
 * can be dropped into a plugin test case.
 */
public final class ConvertFileToStringArray {

  /**
   * @param args
   */
  public static void main(String[] args) {
    if (args.length != 2) {
      System.out.println("Usage: "
          + ConvertFileToStringArray.class.getSimpleName()
          + " file variableName");
      return;
    }

    try {
      String[] lines = readFile(args[0]);

      // Start with the variable assignment
      System.out.println(args[1] + " = new String[]{");

      for (int i = 0; i < lines.length; i++) {
        // Each line has to be in quotes (which means we have to escape any
        // internal quotes), and lines have to be separated by commas
        String line = "\"" + lines[i].replaceAll("\"", "\\\\\"") + "\"";
        if (i < (lines.length - 1)) {
          line += ",";
        }
        System.out.println(line);
      }

      // Close the array initializer
      System.out.println("};");

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static String[] readFile(String fileName) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(fileName));
    List<String> lines = new ArrayList<String>();

    while (reader.ready()) {
      String line = reader.readLine();
      lines.add(line);
    }

    return lines.toArray(new String[0]);
  }

}
