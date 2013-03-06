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
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;

/**
 * A content provider for the tree viewer that is part of the
 * {@link TreeViewer}.
 */
public class TreeContentProvider implements ITreeContentProvider,
    IWebAppDebugModelListener {

  private static final Object[] NO_ELEMENTS = new Object[0];
  private TreeViewer viewer = null;

  public void browserTabCreated(WebAppDebugModelEvent<BrowserTab> e) {
    final BrowserTab b = e.getElement();
    assert (b != null);

    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        assert (b.getParent() != null);
        viewer.add(b.getParent(), b);
        viewer.setSelection(new StructuredSelection(b));
      }
    });
  }

  public void browserTabNeedsAttention(WebAppDebugModelEvent<BrowserTab> e) {
    final BrowserTab b = e.getElement();
    assert (b != null);

    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        viewer.update(b, null);
        viewer.update(b.getParent(), null);
      }
    });
  }

  public void browserTabRemoved(WebAppDebugModelEvent<BrowserTab> e) {
    final BrowserTab b = e.getElement();
    assert (b != null);

    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        assert (b.getParent() != null);
        viewer.remove(b);
      }
    });
  }

  public void browserTabTerminated(WebAppDebugModelEvent<BrowserTab> e) {
    final BrowserTab b = e.getElement();
    assert (b != null);

    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        viewer.update(b, null);
      }
    });
  }

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
    this.viewer = (TreeViewer) viewer;

    if (oldInput != null) {
      ((WebAppDebugModel) oldInput).removeWebAppDebugModelListener(this);
    }

    if (newInput != null) {
      ((WebAppDebugModel) newInput).addWebAppDebugModelListener(this);
    }
  }

  public void launchConfigurationLaunched(
      WebAppDebugModelEvent<LaunchConfiguration> e) {
    final LaunchConfiguration lc = e.getElement();
    assert (lc != null);

    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        assert (lc.getParent() != null);
        viewer.add(lc.getParent(), lc);
        viewer.setSelection(new StructuredSelection(lc));
      }
    });
  }

  public void launchConfigurationLaunchUrlsChanged(
      WebAppDebugModelEvent<LaunchConfiguration> e) {
  }

  public void launchConfigurationRemoved(
      WebAppDebugModelEvent<LaunchConfiguration> e) {
    final LaunchConfiguration lc = e.getElement();
    assert (lc != null);

    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        viewer.remove(lc);
        if (WebAppDebugModel.getInstance().getLaunchConfigurations().size() > 0) {
          viewer.setSelection(new StructuredSelection(
              WebAppDebugModel.getInstance().getLaunchConfigurations().get(0)));
        }
      }
    });
  }

  public void launchConfigurationRestartWebServerStatusChanged(
      WebAppDebugModelEvent<LaunchConfiguration> e) {
    // Ignore
  }

  public void launchConfigurationTerminated(
      WebAppDebugModelEvent<LaunchConfiguration> e) {
    final LaunchConfiguration lc = e.getElement();
    assert (lc != null);

    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        viewer.update(lc, null);
      }
    });
  }
  public void serverCreated(WebAppDebugModelEvent<Server> e) {
    final Server s = e.getElement();
    assert (s != null);

    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        assert (s.getParent() != null);
        viewer.add(s.getParent(), s);
      }
    });
  }

  public void serverNeedsAttention(WebAppDebugModelEvent<Server> e) {
    final Server s = e.getElement();
    assert (s != null);

    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        viewer.update(s, null);
        viewer.update(s.getParent(), null);
      }
    });
  }

  public void serverTerminated(WebAppDebugModelEvent<Server> e) {
    final Server s = e.getElement();
    assert (s != null);

    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        viewer.update(s, null);
      }
    });
  }

}
