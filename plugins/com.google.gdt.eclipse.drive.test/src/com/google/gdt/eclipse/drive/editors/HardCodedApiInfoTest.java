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

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Lists;
import com.google.gdt.eclipse.drive.editors.ApiDocumentationService.AttributeDocumentation;
import com.google.gdt.eclipse.drive.editors.ApiDocumentationService.BeanDocumentation;
import com.google.gdt.eclipse.drive.editors.ApiDocumentationService.ClassDocumentation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

/**
 * Unit test for {@link HardCodedApiInfo}.
 */
@RunWith(JUnit4.class)
public class HardCodedApiInfoTest {
  
  private static final String TEST_INPUT_FILE_CONTENT =
      "3\n" 
      + "Parent0\n"
      + "Description of Parent0\n"
      + "5\n"
      + "Parent0Field0\n"
      + "Parent0.Parent0Field0\n"
      + "Description of Parent0Field0\n"
      + "Parent0Field0\n"
      + "Parent0Field1\n"
      + "Parent0.Parent0Field1\n"
      + "Description of Parent0Field1\n"
      + "Parent0Field1\n"
      + "parent0Method0\n"
      + "Parent0.Child00\n"
      + "Description of parent0Method0\n"
      + "parent0Method0(p0m0a0, p0m0a1)\n"
      + "parent0Method1\n"
      + "Parent0.Child00\n"
      + "Description of parent0Method1(,)\n"
      + "parent0Method1(p0m1a0, p0m1a1)\n"
      + "parent0Method1\n"
      + "Parent0.Child00\n"
      + "Description of parent0Method1(,,)\n"
      + "parent0Method1(p0m1a0, p0m1a1, p0m1a2)\n"
      
      + "Parent0.Child00\n"
      + "Description of Child00\n"
      + "2\n"
      + "child00Method0\n"
      + "Parent0.Child00\n"
      + "Description of child00Method0\n"
      + "child00Method0(c00m0a0)\n"
      + "child00Method1\n"
      + "Parent0.Child00\n"
      + "Description of child00Method1\n"
      + "child00Method1()\n"
      
      + "Parent0.Child01\n"
      + "Description of Child01\n"
      + "3\n"
      + "child01Method0\n"
      + "Parent0.Child01\n"
      + "Description of child01Method0\n"
      + "child01Method0(c01m0a0)\n"
      + "child01Method1\n"
      + "Parent0.Child01\n"
      + "Description of child01Method1\n"
      + "child01Method1(c01m1a0)\n"
      + "child01Method2\n"
      + "Parent0.Child01\n"
      + "Description of child01Method2\n"
      + "child01Method2(c01m2a0)\n"
      
      + "Parent0.Enum\n"
      + "Description of Enum\n"
      + "3\n"
      + "ENUM1\n"
      + "String\n"
      + "Description of ENUM1\n"
      + "ENUM1\n"
      + "ENUM2\n"
      + "String\n"
      + "Description of ENUM2\n"
      + "ENUM2\n"
      + "ENUM3\n"
      + "String\n"
      + "Description of ENUM3\n"
      + "ENUM3\n"
      
      
      + "2\n"
      + "Parent1\n"
      + "Description of Parent1\n"
      + "0\n"
      + "Parent1.Child10\n"
      
      + "Description of Child10\n"
      + "2\n"
      + "child10Method0\n"
      + "void\n"
      + "Description of child10Method0\n"
      + "child10Method0()\n"
      + "child10Method1\n"
      + "String[]\n"
      + "Description of child10Method1\n"
      + "child10Method1()\n"
      
      + "Parent1.Child11\n"
      + "Description of Child11\n"
      + "0\n"
      
      
      + "0\n"
      + "Parent2\n"
      + "Description of Parent2\n"
      + "1\n"
      + "parent2Method0\n"
      + "String[]\n"
      + "Description of parent2Method0\n"
      + "parent2Method0()\n"
            
      + "";
  
  @Test
  public void testGetApiInfo() {
    HardCodedApiInfo.setMockInputFile(TEST_INPUT_FILE_CONTENT);
    List<BeanDocumentation> result = Lists.newArrayList(HardCodedApiInfo.getApiInfo());
    assertEquals(3, result.size());
    
    BeanDocumentation bean0 = result.get(0);
    ClassDocumentation parent0 = bean0.getTopLevelType();
    List<ClassDocumentation> childrenOfParent0 = bean0.getChildTypes();
    assertEquals(3, childrenOfParent0.size());
    ClassDocumentation child00 = childrenOfParent0.get(0);
    ClassDocumentation child01 = childrenOfParent0.get(1);
    ClassDocumentation child02 = childrenOfParent0.get(2);
    
    assertEquals("Parent0", parent0.getName());
    assertEquals("Description of Parent0", parent0.getDescription());
    List<AttributeDocumentation> parent0Attributes = Lists.newArrayList(parent0.getAttributes());
    assertEquals(5, parent0Attributes.size());
    AttributeDocumentation parent0Field0 = parent0Attributes.get(0);
    assertEquals("Parent0Field0", parent0Field0.getName());
    assertEquals("Parent0Field0", parent0Field0.getType());
    assertEquals("Description of Parent0Field0", parent0Field0.getDescription());
    assertEquals("Parent0Field0", parent0Field0.getUseTemplate());
    AttributeDocumentation parent0Field1 = parent0Attributes.get(1);
    assertEquals("Parent0Field1", parent0Field1.getName());
    assertEquals("Parent0Field1", parent0Field1.getType());
    assertEquals("Description of Parent0Field1", parent0Field1.getDescription());
    assertEquals("Parent0Field1", parent0Field1.getUseTemplate());
    AttributeDocumentation parent0Method0 = parent0Attributes.get(2);
    assertEquals("parent0Method0", parent0Method0.getName());
    assertEquals("Child00", parent0Method0.getType());
    assertEquals("Description of parent0Method0", parent0Method0.getDescription());
    assertEquals("parent0Method0(p0m0a0, p0m0a1)", parent0Method0.getUseTemplate());
    AttributeDocumentation parent0Method1a = parent0Attributes.get(3);
    assertEquals("parent0Method1", parent0Method1a.getName());
    assertEquals("Child00", parent0Method1a.getType());
    assertEquals("Description of parent0Method1(,)", parent0Method1a.getDescription());
    assertEquals("parent0Method1(p0m1a0, p0m1a1)", parent0Method1a.getUseTemplate());
    AttributeDocumentation parent0Method1b = parent0Attributes.get(4);
    assertEquals("parent0Method1", parent0Method1b.getName());
    assertEquals("Child00", parent0Method1b.getType());
    assertEquals("Description of parent0Method1(,,)", parent0Method1b.getDescription());
    assertEquals("parent0Method1(p0m1a0, p0m1a1, p0m1a2)", parent0Method1b.getUseTemplate());
    
    assertEquals("Child00", child00.getName());
    assertEquals("Description of Child00", child00.getDescription());
    List<AttributeDocumentation> child00Attributes = Lists.newArrayList(child00.getAttributes());
    assertEquals(2, child00Attributes.size());
    AttributeDocumentation child00Method0 = child00Attributes.get(0);
    assertEquals("child00Method0", child00Method0.getName());
    assertEquals("Child00", child00Method0.getType());
    assertEquals("Description of child00Method0", child00Method0.getDescription());
    assertEquals("child00Method0(c00m0a0)", child00Method0.getUseTemplate());
    AttributeDocumentation child00Method1 = child00Attributes.get(1);
    assertEquals("child00Method1", child00Method1.getName());
    assertEquals("Child00", child00Method1.getType());
    assertEquals("Description of child00Method1", child00Method1.getDescription());
    assertEquals("child00Method1()", child00Method1.getUseTemplate());
    
    assertEquals("Child01", child01.getName());
    assertEquals("Description of Child01", child01.getDescription());
    List<AttributeDocumentation> child01Attributes = Lists.newArrayList(child01.getAttributes());
    assertEquals(3, child01Attributes.size());
    AttributeDocumentation child01Method0 = child01Attributes.get(0);
    assertEquals("child01Method0", child01Method0.getName());
    assertEquals("Child01", child01Method0.getType());
    assertEquals("Description of child01Method0", child01Method0.getDescription());
    assertEquals("child01Method0(c01m0a0)", child01Method0.getUseTemplate());
    AttributeDocumentation child01Method1 = child01Attributes.get(1);
    assertEquals("child01Method1", child01Method1.getName());
    assertEquals("Child01", child01Method1.getType());
    assertEquals("Description of child01Method1", child01Method1.getDescription());
    assertEquals("child01Method1(c01m1a0)", child01Method1.getUseTemplate());
    AttributeDocumentation child01Method2 = child01Attributes.get(2);
    assertEquals("child01Method2", child01Method2.getName());
    assertEquals("Child01", child01Method2.getType());
    assertEquals("Description of child01Method2", child01Method2.getDescription());
    assertEquals("child01Method2(c01m2a0)", child01Method2.getUseTemplate());
    
    assertEquals("Enum", child02.getName());
    assertEquals("Description of Enum", child02.getDescription());
    List<AttributeDocumentation> child02Attributes = Lists.newArrayList(child02.getAttributes());
    assertEquals(3, child02Attributes.size());
    AttributeDocumentation child02Val0 = child02Attributes.get(0);
    assertEquals("ENUM1", child02Val0.getName());
    assertEquals("String", child02Val0.getType());
    assertEquals("Description of ENUM1", child02Val0.getDescription());
    assertEquals("ENUM1", child02Val0.getUseTemplate());
    AttributeDocumentation child02Val1 = child02Attributes.get(1);
    assertEquals("ENUM2", child02Val1.getName());
    assertEquals("String", child02Val1.getType());
    assertEquals("Description of ENUM2", child02Val1.getDescription());
    assertEquals("ENUM2", child02Val1.getUseTemplate());
    AttributeDocumentation child02Val2 = child02Attributes.get(2);
    assertEquals("ENUM3", child02Val2.getName());
    assertEquals("String", child02Val2.getType());
    assertEquals("Description of ENUM3", child02Val2.getDescription());
    assertEquals("ENUM3", child02Val2.getUseTemplate());

    
    BeanDocumentation bean1 = result.get(1);
    ClassDocumentation parent1 = bean1.getTopLevelType();
    List<ClassDocumentation> childrenOfParent1 = bean1.getChildTypes();
    assertEquals(2, childrenOfParent1.size());
    ClassDocumentation child10 = childrenOfParent1.get(0);
    ClassDocumentation child11 = childrenOfParent1.get(1);
    
    assertEquals("Parent1", parent1.getName());
    assertEquals("Description of Parent1", parent1.getDescription());
    List<AttributeDocumentation> parent1Attributes = Lists.newArrayList(parent1.getAttributes());
    assertEquals(0, parent1Attributes.size());

    assertEquals("Child10", child10.getName());
    assertEquals("Description of Child10", child10.getDescription());
    List<AttributeDocumentation> child10Attributes = Lists.newArrayList(child10.getAttributes());
    assertEquals(2, child10Attributes.size());
    AttributeDocumentation child10Method0 = child10Attributes.get(0);
    assertEquals("child10Method0", child10Method0.getName());
    assertEquals("void", child10Method0.getType());
    assertEquals("Description of child10Method0", child10Method0.getDescription());
    assertEquals("child10Method0()", child10Method0.getUseTemplate());
    AttributeDocumentation child10Method1 = child10Attributes.get(1);
    assertEquals("child10Method1", child10Method1.getName());
    assertEquals("String[]", child10Method1.getType());
    assertEquals("Description of child10Method1", child10Method1.getDescription());
    assertEquals("child10Method1()", child10Method1.getUseTemplate());
    
    assertEquals("Child11", child11.getName());
    assertEquals("Description of Child11", child11.getDescription());
    List<AttributeDocumentation> child11Attributes = Lists.newArrayList(child11.getAttributes());
    assertEquals(0, child11Attributes.size());    
    
    
    BeanDocumentation bean2 = result.get(2);
    ClassDocumentation parent2 = bean2.getTopLevelType();
    assertEquals("Parent2", parent2.getName());
    assertEquals("Description of Parent2", parent2.getDescription());
    List<AttributeDocumentation> parent2Attributes = Lists.newArrayList(parent2.getAttributes());
    assertEquals(1, parent2Attributes.size());
    List<ClassDocumentation> childrenOfParent2 = bean2.getChildTypes();
    assertEquals(0, childrenOfParent2.size());
  }

}
