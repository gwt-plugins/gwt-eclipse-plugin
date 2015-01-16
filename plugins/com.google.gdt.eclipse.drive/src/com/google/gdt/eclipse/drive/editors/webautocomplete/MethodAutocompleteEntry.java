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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.gdt.eclipse.drive.editors.webautocomplete.AutocompleteProto.Method.Param;

import java.util.List;

/**
 * This class represents a method for the autocomplete system.
 */
@SuppressWarnings("serial")
public class MethodAutocompleteEntry extends AbstractAutocompleteEntry {

  private String returnType;
  private String name;
  private List<AutocompleteProto.Method.Param> params;

  public MethodAutocompleteEntry(
      String returnType, String name, List<AutocompleteProto.Method.Param> params,
      String description) {
    super(description);
    this.name = name;
    this.returnType = returnType;
    this.params = params;
  }

  @Override
  public String getPopupView() {
    return name + "(" + getParameters() + ") : " + getDisplayReturnType();
  }

  @Override
  public String getInsertedView() {
    return name + "(" + getParameterNames() + ")";
  }

  @Override
  public String getReturnType() {
    return returnType;
  }

  @Override
  public String getEntryName() {
    return name;
  }

  public List<AutocompleteProto.Method.Param> getParams() {
    return params;
  }

  @Override
  public int getFinalCursorOffset() {
    return params.isEmpty() ? (name.length() + 2) : (name.length() + 1);
  }

  @Override
  public boolean shouldAutoSelect() {
    //    return !(parameterNames.isEmpty() || parameterNames.contains(","));
    return params.size() == 1;
  }

  @Override
  public int autoSelectStart() {
    return name.length() + 1;
  }

  @Override
  public int autoSelectEnd() {
    return autoSelectStart() + params.size();
  }

  /**
   * @return The names of the parameters as a comma separated string.
   * (If getParameters() returns "String foo, int bar, Object[] x",
   * this function will return "foo, bar, x")
   */
  @VisibleForTesting String getParameterNames() {
    return
        getParameterString(
            new Function<Param, String>(){
              @Override public String apply(Param parameter) {
                return parameter.getName();
              }
            });
  }

  public String getParameters() {
    return
        getParameterString(
            new Function<Param, String>(){
              @Override public String apply(Param parameter) {
                return sanitizeTypeNames(parameter.getTypeName()) + " " + parameter.getName();
              }
            });
  }

  public String methodCallTemplate() {
    StringBuilder resultBuilder = new StringBuilder();
    resultBuilder.append(name);
    resultBuilder.append('(');
    resultBuilder.append(
        getParameterString(
            new Function<Param, String>(){
              @Override public String apply(Param parameter) {
                return "${" + parameter.getName() + "}";
              }
            }));
    resultBuilder.append(')');
    return resultBuilder.toString();
  }

  private String getParameterString(Function<Param, String> stringForParam) {
    StringBuilder resultBuilder = new StringBuilder();
    boolean commaNeeded = false;
    for (AutocompleteProto.Method.Param param : params) {
      if (commaNeeded) {
        resultBuilder.append(", ");
      } else {
        commaNeeded = true;
      }
      resultBuilder.append(stringForParam.apply(param));
    }
    return resultBuilder.toString();
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("returnType", returnType)
        .add("name", name)
        .add("params", params)
        .toString();
  }
}