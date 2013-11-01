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
package com.google.gdt.eclipse.drive.editors;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.gdt.eclipse.drive.DrivePlugin;
import com.google.gdt.eclipse.drive.editors.ApiDocumentationService.AttributeDocumentation;
import com.google.gdt.eclipse.drive.editors.ApiDocumentationService.BeanDocumentation;
import com.google.gdt.eclipse.drive.editors.ApiDocumentationService.ClassDocumentation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Collection;
import java.util.List;

// TODO(nhcohen): Eliminate this class, and obtain API information from a web service call instead,
// once b/9104082  has been resolved.
/**
 * A provisional source of the information used in content assist for Apps Script APIs, based on a
 * snapshot of API information stored in a resource of this plugin. This class ought to be replaced
 * by service that obtains the information dynamically, or else provided in a separate bundle that
 * is updated each time one of the Apps Script APIs is updated.
 */
public class HardCodedApiInfo {

  private HardCodedApiInfo() { } // prevent instantiation
  
  private static final String API_INFO_RESOURCE_PATH =
      "com/google/gdt/eclipse/drive/editors/api-info.txt";
  
  private static List<BeanDocumentation> apiInfo = null;
  private static String mockInputFile = null;
  
  /**
   * Obtains {@link BeanDocumentation} objects describing "beans" in the Apps Script library.
   * The information about this beans is obtained from a data file hard-coded as a resource in this
   * plugin.
   * 
   * @return the {@code ClassDocumentation} objects
   */
  public static synchronized Collection<BeanDocumentation> getApiInfo() {
    if (apiInfo == null) {
      apiInfo = Lists.newLinkedList();
      try {
        readApiInfo();
      } catch (IOException e) {
        DrivePlugin.logError("IOException reading API info", e);
        DrivePlugin.displayLoggedErrorDialog(
            "There was an error reading the API information used for content assist. "
                + "See the Error Log for details.");
        // Continue execution with partial list.
      }
    }
    return apiInfo;
  }
  
  // The resource read by this method is assumed to be a a UTF-8-encoded text file containing, for
  // each bean documented, a series of lines with the following structure:
  // * a line containing the number of child types that the bean has, as a decimal integer
  // * lines describing the bean's top-level type
  // * for each child type, lines describing that child type
  //
  // The lines describing a top-level or child type have the following structure:
  // * a line containing the class name (possibly a qualified name of the form <parent>.<child>)
  // * a line containing the class description
  // * a line containing the number of attributes (methods or fields) that the class has, as a 
  //     decimal integer
  // * for each attribute, the following series of lines:
  //   - a line containing the attribute name
  //   - a line containing the attribute type
  //   - a line containing the attribute description (possibly empty)
  //   - a line containing the attribute use template (a field name or a model method call)
  private static void readApiInfo() throws IOException {
    BufferedReader apiInfoLineReader = getInputReader();
    while (true) {
      String childTypeCountString = apiInfoLineReader.readLine();
      
      if (childTypeCountString == null) {
        break;
      }

      int childTypeCount;
      try {
        childTypeCount = Integer.valueOf(childTypeCountString);
      } catch (NumberFormatException e) {
        throw new IOException("Invalid child-type-count string <" + childTypeCountString + ">");
      }
      
      ClassDocumentation topLevelTypeDocumentation = readTypeInfo(apiInfoLineReader);
      List<ClassDocumentation> childTypeDocumentations = Lists.newLinkedList();
      for (int i = 0; i < childTypeCount; i++) {
        childTypeDocumentations.add(readTypeInfo(apiInfoLineReader));
      }
      apiInfo.add(new BeanDocumentation(topLevelTypeDocumentation, childTypeDocumentations));
    }
  }

  private static BufferedReader getInputReader() {
    BufferedReader apiInfoLineReader;
    if (mockInputFile == null) {
      // production
      InputStream apiInfoInputStream =
          HardCodedApiInfo.class.getClassLoader().getResourceAsStream(API_INFO_RESOURCE_PATH);
      apiInfoLineReader =
          new BufferedReader(new InputStreamReader(apiInfoInputStream, Charsets.UTF_8));
    } else {
      // test
      apiInfoLineReader = new BufferedReader(new StringReader(mockInputFile));
    }
    return apiInfoLineReader;
  }
  
  private static ClassDocumentation readTypeInfo(
      BufferedReader apiInfoLineReader) throws IOException {
    String className = apiInfoLineReader.readLine();
    String classSimpleName = extractSimpleName(className);
    String classDescription = apiInfoLineReader.readLine();
    String attributeCountString = apiInfoLineReader.readLine();
    int attributeCount;
    try {
      attributeCount = Integer.valueOf(attributeCountString);
    } catch (NumberFormatException e) {
      throw new IOException("Invalid attribute-count string <" + attributeCountString + ">");
    }
    List<AttributeDocumentation> attributes = Lists.newLinkedList();
    for (int i = 0; i < attributeCount; i++) {
      String attributeName = apiInfoLineReader.readLine();
      String attributeType = apiInfoLineReader.readLine();
      String attributeDescription = apiInfoLineReader.readLine();
      String attributeUseTemplate = apiInfoLineReader.readLine();
      attributes.add(
          new AttributeDocumentation(
              attributeName,
              extractSimpleName(attributeType),
              attributeDescription,
              attributeUseTemplate));
    }
    return new ClassDocumentation(classSimpleName, classDescription, attributes); 
  }
  
  private static String extractSimpleName(String fullName) {
    int lastDotPos = fullName.lastIndexOf('.');
    return lastDotPos == -1 ? fullName : fullName.substring(lastDotPos + 1);
  }
  
  @VisibleForTesting static void setMockInputFile(String mockInputFile) {
    HardCodedApiInfo.mockInputFile = mockInputFile;
  }

}
