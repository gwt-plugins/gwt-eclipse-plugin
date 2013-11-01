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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.gdt.eclipse.drive.editors.ApiDocumentationService.AttributeDocumentation;
import com.google.gdt.eclipse.drive.editors.ApiDocumentationService.ClassDocumentation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Map;

/**
 * Unit test for {@link ApiDocumentationService}.
 */
@RunWith(JUnit4.class)
public class ApiDocumentationServiceTest {
  
  private static final Collection<ClassDocumentation> TEST_DATA =
      ImmutableList.of(
          new ClassDocumentation(
              "Class1",
              "Description of Class1",
              ImmutableList.of(
                  new AttributeDocumentation(
                      "method11",
                      "type1",
                      "Description of method11",
                      "method11(param111, param112)"),
                  new AttributeDocumentation(
                      "property12",
                      "type2",
                      "Description of property12",
                      "property12"))
                  ),
              new ClassDocumentation(
                  "Class2",
                  "Description of Class2",
                  ImmutableList.of(
                      new AttributeDocumentation(
                          "method21",
                          "type3",
                          "Description of method21",
                          "method21(param211, param212)"),
                      new AttributeDocumentation("property22", "type4"))
                      )
          );

  @Test
  public void testGetClassNames() {
    ApiDocumentationService service = ApiDocumentationService.make(TEST_DATA);
    Collection<String> result = service.getClassNames();
    assertEquals(2, result.size());
    assertTrue(result.contains("Class1"));
    assertTrue(result.contains("Class2"));
  }
  
  @Test
  public void testGetClassDescription() {
    ApiDocumentationService service = ApiDocumentationService.make(TEST_DATA);
    assertEquals("Description of Class1", service.getClassDescription("Class1"));
    assertEquals("Description of Class2", service.getClassDescription("Class2"));
    assertEquals("", service.getClassDescription("bad class name"));
  }

  @Test public void testGetAttributesAndAttributeDocumentationMethods() {
    ApiDocumentationService service = ApiDocumentationService.make(TEST_DATA);
    
    Map<String, AttributeDocumentation> result1 = collectionToMap(service.getAttributes("Class1"));
    assertEquals(ImmutableSet.of("method11", "property12"), result1.keySet());
    AttributeDocumentation method11 = result1.get("method11");
    AttributeDocumentation property12 = result1.get("property12");
    assertEquals("type1", method11.getType());
    assertEquals("Description of method11", method11.getDescription());
    assertEquals("method11(param111, param112)", method11.getUseTemplate());
    assertEquals("type2", property12.getType());
    assertEquals("Description of property12", property12.getDescription());
    assertEquals("property12", property12.getUseTemplate());
    
    Map<String, AttributeDocumentation> result2 = collectionToMap(service.getAttributes("Class2"));
    assertEquals(ImmutableSet.of("method21", "property22"), result2.keySet());
    AttributeDocumentation method21 = result2.get("method21");
    AttributeDocumentation property22 = result2.get("property22");
    assertEquals("type3", method21.getType());
    assertEquals("Description of method21", method21.getDescription());
    assertEquals("method21(param211, param212)", method21.getUseTemplate());
    assertEquals("type4", property22.getType());
    assertEquals("", property22.getDescription());
    assertEquals("property22", property22.getUseTemplate());
    
    assertTrue(service.getAttributes("bad class name").isEmpty());
  }
  
  private static Map<String, AttributeDocumentation> collectionToMap(
      Collection<AttributeDocumentation> attributes) {
    Map<String, AttributeDocumentation> result = Maps.newHashMap();
    for (AttributeDocumentation attribute : attributes) {
      result.put(attribute.getName(), attribute);
    }
    return result;
  }

}
