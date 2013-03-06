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

import com.google.gwt.eclipse.oophm.breadcrumbs.BreadcrumbViewer;
import com.google.gwt.eclipse.oophm.model.BreadcrumbContentProvider;
import com.google.gwt.eclipse.oophm.model.BrowserTab;
import com.google.gwt.eclipse.oophm.model.IModelNode;
import com.google.gwt.eclipse.oophm.model.IWebAppDebugModelListener;
import com.google.gwt.eclipse.oophm.model.LaunchConfiguration;
import com.google.gwt.eclipse.oophm.model.ModelLabelProvider;
import com.google.gwt.eclipse.oophm.model.Server;
import com.google.gwt.eclipse.oophm.model.WebAppDebugModel;
import com.google.gwt.eclipse.oophm.model.WebAppDebugModelEvent;

import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * The panel for the navigation setup using the breadcrumb. Note that this class
 * registers a listener (itself) for model changes, as opposed to the
 * BreadcrumbViewer's content provider registering a listener. This is due to
 * the unconventional way in which the viewer's setInput method is used.
 */
public class BreadcrumbNavigationView extends SelectionProvidingComposite
    implements IWebAppDebugModelListener {

  private final BreadcrumbViewer breadcrumbViewer;
  private final ContentPanel contentPanel;
  private WebAppDebugModel model;

  public BreadcrumbNavigationView(Composite parent, int style) {
    super(parent, style);

    addDisposeListener(new DisposeListener() {
      public void widgetDisposed(DisposeEvent e) {
        setInput(null);
      }
    });

    GridLayout layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.horizontalSpacing = 0;
    layout.verticalSpacing = 0;
    setLayout(layout);
    setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    breadcrumbViewer = new BreadcrumbViewer(this, SWT.HORIZONTAL) {
      @Override
      protected void configureDropDownViewer(TreeViewer viewer, Object input) {
        viewer.setContentProvider(new BreadcrumbContentProvider());
        viewer.setLabelProvider(new ModelLabelProvider());
        viewer.setComparator(new ModelNodeViewerComparator());
      }
    };

    breadcrumbViewer.addOpenListener(new IOpenListener() {
      public void open(OpenEvent event) {
        setSelection(event.getSelection());
      }
    });
    breadcrumbViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        fireSelectionChangedEvent(event);
      }
    });

    breadcrumbViewer.setLabelProvider(new ModelLabelProvider());
    breadcrumbViewer.setContentProvider(new BreadcrumbContentProvider());
    contentPanel = new ContentPanel(this, SWT.NONE);
  }

  public void browserTabCreated(final WebAppDebugModelEvent<BrowserTab> e) {
    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        setSelection(new StructuredSelection(e.getElement()));
        breadcrumbViewer.refresh();
      }
    });
  }

  public void browserTabNeedsAttention(final WebAppDebugModelEvent<BrowserTab> e) {
    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        /*
         * If it is the case that the browser tab that needs attention is
         * currently selected, then we need to reset the selection so that the
         * text color update is picked up.
         * 
         * We also need to handle the case where the parent of the tab that
         * needs attention (i.e. the launch configuration) is selected, but the
         * launch config itself is not. Since the launch config inherits its
         * attention state from its children, its text color changes as well.
         */
        if (!resetSelectionIfModelNodeIsEqualToCurrentSelection(e.getElement())) {
          resetSelectionIfModelNodeIsChildOfCurrentSelection(e.getElement());
        }
        breadcrumbViewer.refresh();
      }
    });
  }

  public void browserTabRemoved(final WebAppDebugModelEvent<BrowserTab> e) {
    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        maybeClearSelectionAndSelectNextAvailable(e.getElement());
        breadcrumbViewer.refresh();
      }
    });

  }

  public void browserTabTerminated(final WebAppDebugModelEvent<BrowserTab> e) {
    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        resetSelectionIfModelNodeIsEqualToCurrentSelection(e.getElement());
        breadcrumbViewer.refresh();
      }
    });
  }

  @Override
  public ISelection getSelection() {
    return breadcrumbViewer.getSelection();
  }

  public void launchConfigurationLaunched(
      final WebAppDebugModelEvent<LaunchConfiguration> e) {
    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        setSelection(new StructuredSelection(e.getElement()));
        breadcrumbViewer.refresh();
      }
    });
  }

  public void launchConfigurationLaunchUrlsChanged(WebAppDebugModelEvent<LaunchConfiguration> e) {
  }

  public void launchConfigurationRemoved(
      final WebAppDebugModelEvent<LaunchConfiguration> e) {

    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        maybeClearSelectionAndSelectNextAvailable(e.getElement());
        breadcrumbViewer.refresh();
      }
    });
  }

  public void launchConfigurationRestartWebServerStatusChanged(
      WebAppDebugModelEvent<LaunchConfiguration> e) {
    // Ignore    
  }

  public void launchConfigurationTerminated(
      final WebAppDebugModelEvent<LaunchConfiguration> e) {
    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        resetSelectionIfModelNodeIsEqualToCurrentSelection(e.getElement());
        breadcrumbViewer.refresh();
      }
    });
  }

  public void serverCreated(final WebAppDebugModelEvent<Server> e) {
    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        resetSelectionIfModelNodeIsChildOfCurrentSelection(e.getElement());
        breadcrumbViewer.refresh();
      }
    });
  }

  public void serverNeedsAttention(final WebAppDebugModelEvent<Server> e) {
    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        /*
         * If it is the case that the server that needs attention is currently
         * selected, then we need to reset the selection so that the text color
         * update is picked up.
         * 
         * We also need to handle the case where the parent of the server that
         * needs attention (i.e. the launch configuration) is selected, but the
         * launch config itself is not. Since the launch config inherits its
         * attention state from its children, its text color changes as well.
         */
        if (!resetSelectionIfModelNodeIsEqualToCurrentSelection(e.getElement())) {
          resetSelectionIfModelNodeIsChildOfCurrentSelection(e.getElement());
        }
        breadcrumbViewer.refresh();
      }
    });
  }

  public void serverTerminated(final WebAppDebugModelEvent<Server> e) {
    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        resetSelectionIfModelNodeIsEqualToCurrentSelection(e.getElement());
        breadcrumbViewer.refresh();
      }
    });
  }

  public void setInput(WebAppDebugModel newModel) {
    if (model != null) {
      model.removeWebAppDebugModelListener(this);
    }

    this.model = newModel;
    setSelection(new StructuredSelection(WebAppDebugModel.getInstance()));

    if (model != null) {
      model.addWebAppDebugModelListener(this);
    }
  }

  @Override
  public void setSelection(final ISelection selection) {
    assert (selection != null && !selection.isEmpty());
    final Object firstElement = ((IStructuredSelection) selection).getFirstElement();
    /*
     * breadcrumbViewer.setInput is called every time an item is selected.
     * Because of this, it does not make sense for the content provider
     * associated with the breadcrumbViewer to register a listener on the model
     */
    breadcrumbViewer.setInput(firstElement);
    breadcrumbViewer.setSelection(selection, true);
    contentPanel.setSelection(firstElement);
    contentPanel.setFocus();
  }

  private IModelNode getFirstElementInSelection() {
    StructuredSelection currentSelection = (StructuredSelection) getSelection();
    if (currentSelection == null || currentSelection.isEmpty()) {
      return null;
    }

    return (IModelNode) currentSelection.getFirstElement();
  }

  /**
   * Even though we don't have a lock on it, querying the model at this point is
   * actually safe. We're looking for parent relationships, and parent
   * relationships are set at construction time for each model object. So, it is
   * not possible for a parent-child relationship to become invalid.
   * 
   * The only possible issue is that a launch configuration could become
   * disconnected from the model while this code is running. That in itself is
   * not dangerous, because the possibleParent is a launch configuration.
   */
  private boolean isParentOf(IModelNode node, IModelNode possibleParent) {
    assert (!(possibleParent instanceof WebAppDebugModel));

    if (possibleParent == null) {
      return false;
    }

    while (node != null) {
      if (node.getParent() == possibleParent) {
        return true;
      }
      node = node.getParent();
    }
    return false;
  }

  private void maybeClearSelectionAndSelectNextAvailable(
      BrowserTab removedBrowserTab) {
    IModelNode currentSelection = getFirstElementInSelection();

    if (currentSelection == null) {
      return;
    }

    if (currentSelection == removedBrowserTab) {

      /*
       * Technically, a browser tab could have been added to the model (but not
       * the view) at the point that we perform this query. In reality, this is
       * very unlikely to happen. Even it it does, there will be no harm - this
       * call will force a refresh of the data, and the new one that "sneaked"
       * in will be selected. The notification event for the new launch will
       * follow shortly afterwards.
       */
      BrowserTab latestActiveBrowserTab = removedBrowserTab.getLaunchConfiguration().getLatestActiveBrowserTab();
      if (latestActiveBrowserTab != null) {
        setSelection(new StructuredSelection(latestActiveBrowserTab));
      } else {
        setSelection(new StructuredSelection(
            removedBrowserTab.getLaunchConfiguration()));
      }
    }
  }

  private void maybeClearSelectionAndSelectNextAvailable(
      LaunchConfiguration removedLaunchConfiguration) {
    IModelNode currentSelection = getFirstElementInSelection();

    if (currentSelection == null) {
      return;
    }

    if (currentSelection == removedLaunchConfiguration
        || isParentOf(currentSelection, removedLaunchConfiguration)) {

      /*
       * Technically, a launch configuration could have been added to the model
       * (but not the view) at the point that we perform this query. In reality,
       * this is very unlikely to happen. Even it it does, there will be no harm
       * - this call will force a refresh of the data, and the new one that
       * "sneaked" in will be selected. The notification event for the new
       * launch will follow shortly afterwards.
       */
      LaunchConfiguration latestActiveLaunchConfiguration = WebAppDebugModel.getInstance().getLatestActiveLaunchConfiguration();
      if (latestActiveLaunchConfiguration != null) {
        setSelection(new StructuredSelection(latestActiveLaunchConfiguration));
      } else {
        setSelection(new StructuredSelection(WebAppDebugModel.getInstance()));
      }
    }
  }

  private boolean resetSelectionIfModelNodeIsChildOfCurrentSelection(
      IModelNode modelNode) {
    assert (modelNode instanceof BrowserTab || modelNode instanceof Server);

    IModelNode selectionModelNode = getFirstElementInSelection();
    if (isParentOf(modelNode, selectionModelNode)) {
      /*
       * We're basically resetting the current selection here. This has the
       * effect of refreshing the top line of the breadcrumb view, thereby
       * forcing an arrow next to the launch configuration to appear, indicating
       * that a child of the launch configuration has just appeared.
       * 
       * This hack is necessary because refreshing the breadcrumb viewer does
       * not do this (due to the odd way in which BreadCrumb viewer is
       * implemented; it is not a traditional JFace viewer, even though it looks
       * like one from the outside).
       */
      setSelection(getSelection());
      return true;
    }

    return false;
  }

  private boolean resetSelectionIfModelNodeIsEqualToCurrentSelection(final IModelNode modelNode) {
    if (getFirstElementInSelection() == modelNode) {
      /*
       * We're basically resetting the current selection here. This has the
       * effect of refreshing the top line of the breadcrumb view, thereby
       * forcing an arrow next to the launch configuration to appear, indicating
       * that a child of the launch configuration has just appeared.
       * 
       * This hack is necessary because refreshing the breadcrumb viewer does
       * not do this (due to the odd way in which BreadCrumb viewer is
       * implemented; it is not a traditional JFace viewer, even though it looks
       * like one from the outside).
       */
      setSelection(getSelection());
      return true;
    }

    return false;
  }

}