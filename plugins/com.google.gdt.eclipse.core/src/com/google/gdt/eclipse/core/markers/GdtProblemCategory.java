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
package com.google.gdt.eclipse.core.markers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Defines the categories problems belong to. Problems will be grouped by
 * categories on the Errors/Warnings preference page.
 */
public enum GdtProblemCategory {

  APP_ENGINE(2, "App Engine"),

  CLIENT_BUNDLE(6, "GWT ClientBundle"),

  GWT_RPC(4, "GWT Remote Procedure Calls (RPC)"),

  JSNI(5, "GWT JavaScript Native Interface (JSNI)"),

  SDK(1, "Project structure and SDKs"),

  UI_BINDER(3, "GWT UiBinder");

  public static List<GdtProblemCategory> getAllCategoriesInDisplayOrder() {
    List<GdtProblemCategory> categories = new ArrayList<GdtProblemCategory>(
        Arrays.asList(GdtProblemCategory.values()));
    Collections.sort(categories, new Comparator<GdtProblemCategory>() {
      public int compare(GdtProblemCategory a, GdtProblemCategory b) {
        return a.getDisplayOrder() - b.getDisplayOrder();
      }
    });
    return categories;
  }

  private final String displayName;

  private final int displayOrder;

  private GdtProblemCategory(int displayOrder, String displayName) {
    this.displayOrder = displayOrder;
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public int getDisplayOrder() {
    return displayOrder;
  }

  @Override
  public String toString() {
    return displayName;
  }
}