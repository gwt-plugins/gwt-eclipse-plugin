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
package com.google.gdt.eclipse.drive.editors.webautocomplete;

import java.util.List;

/**
 * A Java class manually derived from the protocol buffers that the web editor autocomplete
 * algorithm uses to represent information about Google APIs.
 */
public class AutocompleteProto {

  private AutocompleteProto() { // prevent instantiation
  }
  
  public enum DocVisibility { VISIBLE, NOT_DOCUMENTED, NOT_VISIBLE_TO_SCRIPTS, FORBIDDEN }

  /**
   * Editor autocomplete data for a given parent bean.
   */
  public static class Bean {

    // Autocomplete data for the top-level interface of this bean.
    // TODO(bcat): // This field is *badly* misnamed. Not all top_level_type values are actually
    // top-level bean types. This should be renamed to parent_type, and that naming change should be
    // propagated to all code that deals with bean data protos.
    private final Type topLevelType;

    // Autocomplete data for all child types (interfaces that are not top-level,
    // as well as enums) in this bean.
    private final List<Type> childType;

    // Indicates that this is a common bean (e.g., User, Month),
    // and is referenced by multiple beans.
    private final boolean isCommon;

    // Indicates that this is a native bean (e.g. Math).
    private final boolean isNative;

    public Bean(Type topLevelType, List<Type> childType, boolean isCommon, boolean isNative) {
      this.topLevelType = topLevelType;
      this.childType = childType;
      this.isCommon = isCommon;
      this.isNative = isNative;
    }
    
    public Bean(Type topLevelType, List<Type> childType) {
      this(topLevelType, childType, false, false);
    }

    public Type getTopLevelType() {
      return topLevelType;
    }

    public List<Type> getChildTypeList() {
      return childType;
    }

    public boolean getIsCommon() {
      return isCommon;
    }
    
    public boolean getIsNative() {
      return isNative;
    }
  }

  /**
   * Autocomplete data for a bean interface or enum.
   */
  public static class Type {

    // The interface or enum's "friendly name", i.e., the name that should be shown to scripts.
    private final String name;
    
    // A short one-line description of a type
    private final String description;

    // Sorted autocomplete data for the fields in this bean.
    private final List<Field> field;

    // Sorted autocomplete data for the methods in this bean.
    private final List<Method> method;

    // Whether or not this type should shown in autocomplete by default or only
    // when explicitly enabled via CDD configuration.
    private final DocVisibility visibility;

    public Type(
        String name, String description, List<Field> field, List<Method> method,
        DocVisibility visibility) {
      this.name = name;
      this.description = description;
      this.field = field;
      this.method = method;
      this.visibility = visibility;
    }

    public Type(String name, String description, List<Field> field, List<Method> method) {
      this(name, description, field, method, DocVisibility.VISIBLE);
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }

    public List<Field> getFieldList() {
      return field;
    }

    public List<Method> getMethodList() {
      return method;
    }

    public DocVisibility getVisibility() {
      return visibility;
    }    
  }
 
  /**
   * Autocomplete data for a field.
   */
  public static class Field {

    // Determines whether the field is a normal field, or some more specialized
    // kind, e.g., an enum constant.
    enum Kind { NORMAL, ENUM_CONSTANT }

    // The name of this field.
    private final String name;

    // The "friendly name" of this field's type.
    private final String typeName;
    
    // A description of this field.
    private final String description;

    // Specifies what kind of field this is.
    private final Kind kind;

    // Whether or not this field should shown in autocomplete by default or only
    // when explicitly enabled via CDD configuration.
    private final DocVisibility visibility;

    public Field(
        String name, String typeName, String description, Kind kind, DocVisibility visibility) {
      this.name = name;
      this.typeName = typeName;
      this.description = description;
      this.kind = kind;
      this.visibility = visibility;
    }

    public Field(String name, String typeName, String description) {
      this(name, typeName, description, Kind.NORMAL, DocVisibility.VISIBLE);
    }

    public String getName() {
      return name;
    }

    public String getTypeName() {
      return typeName;
    }

    public String getDescription() {
      return description;
    }

    public Kind getKind() {
      return kind;
    }

    public DocVisibility getVisibility() {
      return visibility;
    }
  }

  /**
   * Autocomplete data for a method.
   */
  public static class Method {

    /**
     * Autocomplete data for a parameter.
     */
    public static class Param {
      // The name of this parameter.
      private final String name;
     
      // The "friendly name" of this parameter's type.
      private final String typeName;
  
      public Param(String name, String typeName) {
        this.name = name;
        this.typeName = typeName;
      }
  
      public String getName() {
        return name;
      }
  
      public String getTypeName() {
        return typeName;
      }
    }
   
    // The name of this method.
    private final String name;

    // The "friendly name" of this method's return type.
    private final String returnTypeName;

    // The parameters taken by the method, in the order they occur in the method's signature.
    private final List<Param> parameters;
    
    // A one-line description of this method.
    private final String description;

    // Whether or not this method should shown in autocomplete by default or only
    // when explicitly enabled via CDD configuration.
    private final DocVisibility visibility;

    public Method(
        String name, String returnTypeName, List<Param> param, String description,
        DocVisibility visibility) {
      this.name = name;
      this.returnTypeName = returnTypeName;
      this.parameters = param;
      this.description = description;
      this.visibility = visibility;
    }

    public Method(String name, String returnTypeName, List<Param> param, String description) {
      this(name, returnTypeName, param, description, DocVisibility.VISIBLE);
    }

    public String getName() {
      return name;
    }

    public String getReturnTypeName() {
      return returnTypeName;
    }

    public List<Param> getParamList() {
      return parameters;
    }

    public String getDescription() {
      return description;
    }

    public DocVisibility getVisibility() {
      return visibility;
    }
  }
}
