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
package com.google.gwt.eclipse.oophm.views.hierarchical;

import com.google.gwt.eclipse.oophm.model.IModelNode;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

/**
 * Defines a Viewer sort order for {@link IModelNode} elements. Makes sure that
 * newer elements appear first and terminated elements appear last.
 */
public class ModelNodeViewerComparator extends ViewerComparator {
  private int compare(IModelNode n1, IModelNode n2) {
    assert (n1 != null && n2 != null);
     
    if (n1.isTerminated()) {
      if (n2.isTerminated()) {
        // n1 and n2 are terminated; larger ID should appears first
        return n2.getId() - n1.getId();
      }

      // n1 appears after n2, n2 is terminated
      return 1;
    } else {
      if (n2.isTerminated()) {
        // n1 is not terminated but n2 is; n1 appears first
        return -1;
      }

      // neither n1 nor n2 are terminated; larger ID appears first 
      return n2.getId() - n1.getId();
    }
  }

  @Override
  public int compare(Viewer viewer, Object e1, Object e2) {
    assert (e1 instanceof IModelNode);
    assert (e2 instanceof IModelNode);

    return compare((IModelNode) e1, (IModelNode) e2);
  }
}
