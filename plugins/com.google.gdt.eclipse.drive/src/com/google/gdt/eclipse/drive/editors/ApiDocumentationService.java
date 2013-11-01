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

import com.google.api.client.util.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gdt.eclipse.drive.editors.webautocomplete.AutocompleteEntryHolder;
import com.google.gdt.eclipse.drive.editors.webautocomplete.AutocompleteProto.Bean;
import com.google.gdt.eclipse.drive.editors.webautocomplete.AutocompleteProto.Field;
import com.google.gdt.eclipse.drive.editors.webautocomplete.AutocompleteProto.Method;
import com.google.gdt.eclipse.drive.editors.webautocomplete.AutocompleteProto.Type;
import com.google.gdt.eclipse.drive.editors.webautocomplete.AutocompleteProto.Method.Param;

import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.concurrent.Immutable;

/**
 * A service providing the information about classes, methods, and properties used in
 * content assist. An object of the inner class {@link ClassDocumentation} contains the information
 * about a particular class, including a description of the class and the methods and properties of
 * that class.
 */
@Immutable
public class ApiDocumentationService {

  private static final Pattern METHOD_CALL_PATTERN = Pattern.compile(".+\\((.*)\\)");
  
  /**
   * Creates an {@code ApiDocumentationService} that will serve information about the classes
   * described by a specified collection of {@link ClassDocumentation} objects.
   * 
   * @param apiData the specified collection of {@code ClassDocumentation} objects
   * @return the {@code ApiDocumentationService
   */
  public static ApiDocumentationService make(Collection<ClassDocumentation> apiData) {
    SortedMap<String, ClassDocumentation> classNamesToClassDocumentation = Maps.newTreeMap();
    for (ClassDocumentation classInfo : apiData) {
      classNamesToClassDocumentation.put(classInfo.getName(), classInfo);
    }
    return new ApiDocumentationService(classNamesToClassDocumentation);
  }
  
  private final SortedMap<String, ClassDocumentation> classNamesToClassDocumentation;
  
  private ApiDocumentationService(
      SortedMap<String, ClassDocumentation> classNamesToClassDocumentation) {
    this.classNamesToClassDocumentation = classNamesToClassDocumentation;
  }

  /**
   * @return a collection of names of classes known to this {@code ApiDocumentationService}
   */
  public Collection<String> getClassNames() {
    return classNamesToClassDocumentation.keySet();
  }
  
  /**
   * Returns the description of a class with a specified name.
   * 
   * @param className the specified name
   * @return the description, or the empty string if there is no class with the specified name
   */
  public String getClassDescription(String className) {
    ClassDocumentation classInfo = classNamesToClassDocumentation.get(className);
    return classInfo == null ? "" : classInfo.getDescription();
  }
  
  /**
   * Returns a collection of {@link AttributeDocumentation} objects for the attributes of a class
   * with a specified name (an empty collection if there is no class with that name).
   * 
   * @param className the specified name
   * @return the collection of {@code AttributeDocumentation} objects
   */
  public Collection<AttributeDocumentation> getAttributes(String className) {
    ClassDocumentation classInfo = classNamesToClassDocumentation.get(className);
    return
        classInfo == null ? ImmutableList.<AttributeDocumentation>of() : classInfo.getAttributes();
  }
  
  public static AutocompleteEntryHolder translateBeanDocumentations(
      Collection<BeanDocumentation> beanDocumentations) {
    AutocompleteEntryHolder result = new AutocompleteEntryHolder();
    for (BeanDocumentation beanDocumentation : beanDocumentations) {
      Type parentType = translateClassDocumentation(beanDocumentation.getTopLevelType());
      List<Type> childTypes = Lists.newLinkedList();
      for (ClassDocumentation childDocumentation : beanDocumentation.getChildTypes()) {
        childTypes.add(translateClassDocumentation(childDocumentation));
      }
      result.addBeanData(new Bean(parentType, childTypes));
    }
    return result;
  }

  private static Type translateClassDocumentation(ClassDocumentation classDocumentation) {
    List<Field> fields = Lists.newLinkedList();
    List<Method> methods = Lists.newLinkedList();
    for (AttributeDocumentation attributeDocumentation : classDocumentation.getAttributes()) {
      String useTemplate = attributeDocumentation.getUseTemplate();
      Matcher methodCallMatcher = METHOD_CALL_PATTERN.matcher(useTemplate);
      if (methodCallMatcher.matches()) {
        List<Param> parameters = Lists.newLinkedList();
        for (String parameterName :
            Splitter.on(',').omitEmptyStrings().split(methodCallMatcher.group(1))) {
          parameters.add(new Param(parameterName.trim(), ""));
        }
        methods.add(
            new Method(
                attributeDocumentation.getName(),
                attributeDocumentation.getType(),
                parameters,
                attributeDocumentation.getDescription()));
      } else {
        fields.add(
            new Field(
                attributeDocumentation.getName(), 
                attributeDocumentation.getType(),
                attributeDocumentation.getDescription()));
      }
    }
    Type parentType =
        new Type(
            classDocumentation.getName(), classDocumentation.getDescription(), fields, methods);
    return parentType;
  }
  
  public static void addKeywords(
      AutocompleteEntryHolder holder, Collection<String> keywords) {
    for (String word : keywords) {
      Type keywordAsType =
          new Type(word, "", ImmutableList.<Field>of(), ImmutableList.<Method>of());
      holder.addBeanData(new Bean(keywordAsType, ImmutableList.<Type>of()));
    }
  }
  
  /**
   * Information documenting a bean, consisting of a {@link ClassDocumentation} for the top-level
   * type and a list of {@code ClassDocumentation} objects for the child types.
   */
  @Immutable
  public static class BeanDocumentation {
    private final ClassDocumentation topLevelType;
    private final List<ClassDocumentation> childTypes;
    
    public BeanDocumentation(ClassDocumentation topLevelType,
        List<ClassDocumentation> childTypes) {
      this.topLevelType = topLevelType;
      this.childTypes = childTypes;
    }

    public ClassDocumentation getTopLevelType() {
      return topLevelType;
    }

    public List<ClassDocumentation> getChildTypes() {
      return childTypes;
    }    
  }

  /**
   * Information documenting a class, consisting of the name of the class, a description of the
   * class, and an {@link AttributeDocumentation} for each of the class's attributes.
   */
  @Immutable
  public static class ClassDocumentation {
    private final String name;
    private final String description;
    private final Collection<AttributeDocumentation> attributes;
    
    public ClassDocumentation(
        String name, String description, Collection<AttributeDocumentation> attributes) {
      this.name = name;
      this.description = description;
      this.attributes = attributes;
    }

    public String getName() {
      return name;
    }
    
    public String getDescription() {
      return description;
    }
    
    public Collection<AttributeDocumentation> getAttributes() {
      return attributes;
    }
  }
  
  /**
   * Information documenting an attribute (method or property) of a class.
   */
  @Immutable
  public static class AttributeDocumentation {
    private final String name;
    private final String type;
    private final String description;
    private final String useTemplate;
    
    /**
     * Constructs an {@code AttributeDocumentation} with a given name, description,
     * and use template.
     * 
     * @param name the name of the method or property to be documented
     * @param type the JavaScript datatype of the method result or property
     * @param description
     *     a human-readable textual description of the method or property to be documented
     * @param useTemplate
     *     for a method, a model method call; for a property, the name of the property
     */
    public AttributeDocumentation(
        String name, String type, String description, String useTemplate) {
      this.name = Preconditions.checkNotNull(name);
      this.type = Preconditions.checkNotNull(type);
      this.description = Preconditions.checkNotNull(description);
      this.useTemplate = Preconditions.checkNotNull(useTemplate);
    }
    
    /**
     * Constructs an {@code AttributeDocumentation} for a property without a description.
     * 
     * @param name the name of the property
     */
    public AttributeDocumentation(String name, String type) {
      this(name, type, "", name);
    }
    
    /**
     * @return the name of the method or property described by this {@code AttributeDocumentation}
     */
    public String getName() {
      return name;
    }
    
    /**
     * @return the JavaScript datatype of the method result or property
     */
    public String getType() {
      return type;
    }
    
    /**
     * @return a human-readable textual description of an attribute
     */
    public String getDescription() {
      return description;
    }
    
    /**
     * Returns a template for the use of the attribute described by this
     * {@code AttributeDocumentation}. The template is the text that is inserted into the editor if
     * the completion proposal corresponding to this {@code AttributeDocumentation} is selected.
     * In the case of a method, the template consists of a method call. In the case of a property,
     * the template is the name of the property.
     * 
     * @return the template
     */
    public String getUseTemplate() {
      return useTemplate;
    }
  }
}
