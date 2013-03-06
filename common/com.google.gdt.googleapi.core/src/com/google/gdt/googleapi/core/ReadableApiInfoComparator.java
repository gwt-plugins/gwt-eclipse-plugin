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
package com.google.gdt.googleapi.core;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Static class encapsulates sorting rules for ReadableApiInfo.
 */
public class ReadableApiInfoComparator implements Comparator<ReadableApiInfo>,
    Serializable {

  private static final long serialVersionUID = -7941371127006621593L;

  public int compare(ReadableApiInfo item1, ReadableApiInfo item2) {
    int result = 0;
    if (0 == result) {
      result = item1.getRanking().compareTo(item2.getRanking());
    }
    if (0 == result) {
      result = (item1.getName()).compareTo(item2.getName());
    }
    if (0 == result) {
      result = (item1.getVersion()).compareTo(item2.getVersion());
    }
    return result;
  }
}
