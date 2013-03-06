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
package com.google.gdt.eclipse.appsmarketplace.data;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import java.util.Map;

/**
 * Pre-defined categories for Google Apps Marketplace.
 */
public class Category {
  private static Map<String, Integer> categoryMap = null;

  public static String[] getCategories() {
    initialize();
    return categoryMap.keySet().toArray(new String[0]);

  }

  public static Integer getCategoryId(String name) {
    return categoryMap.get(name);
  }

  public static Integer getDefault() {
    // Default category is Productivity
    return 5;
  }

  private static void initialize() {
    if (categoryMap == null) {
      Builder<String, Integer> builder = ImmutableMap.<
          String, Integer> builder();
      builder.put("Google Apps: Accounting & Finance", 0);
      builder.put("Google Apps: Admin Tools", 1);
      builder.put("Google Apps: Calendar & Scheduling", 2);
      builder.put("Google Apps: Customer Management", 3);
      builder.put("Google Apps: Document Management", 4);
      builder.put("Google Apps: Productivity", 5);
      builder.put("Google Apps: Project Management", 6);
      builder.put("Google Apps: Sales & Marketing", 7);
      builder.put("Google Apps: Security & Compliance", 8);
      builder.put("Google Apps: Workflow", 9);
      categoryMap = builder.build();
    }
  }
}
