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
package com.google.gwt.eclipse.oophm.model;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * Content provider for the
 * {@link com.google.gwt.eclipse.oophm.breadcrumbs.BreadcrumbViewer}. This
 * content provider is very similar to the {@link TreeContentProvider}. The main
 * difference is that the {@link WebAppDebugModel} is considered to be the
 * "parent" of each {@link LaunchConfiguration}. The other difference is that
 * listeners are not hooked up to the {@link WebAppDebugModel} on calls to
 * {@link #inputChanged(Viewer, Object, Object)}.
 */
public class BreadcrumbContentProvider implements ITreeContentProvider {
  private static final Object[] NO_ELEMENTS = new Object[0];

  public void dispose() {
  }

  public Object[] getChildren(Object parentElement) {
    if (parentElement instanceof IModelNode) {
      return ((IModelNode) parentElement).getChildren().toArray();
    }

    return NO_ELEMENTS;
  }

  public Object[] getElements(Object inputElement) {
    return getChildren(inputElement);
  }

  public Object getParent(Object element) {
    if (element instanceof IModelNode) {
      return ((IModelNode) element).getParent();
    }

    return null;
  }

  public boolean hasChildren(Object element) {
    return getChildren(element).length > 0;
  }

  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    /*
     * Do nothing here. Normally, we would hook up listeners for the model, but
     * since this method is called every time a selection is made (which is
     * contrary to the JFace viewer architecture), we hook up the listeners in
     * the BreadcrumbNavigationView.
     */
  }

}
