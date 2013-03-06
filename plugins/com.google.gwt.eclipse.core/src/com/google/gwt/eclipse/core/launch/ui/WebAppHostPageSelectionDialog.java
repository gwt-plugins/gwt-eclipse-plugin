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
package com.google.gwt.eclipse.core.launch.ui;

import com.google.gdt.eclipse.core.AdapterUtilities;
import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.SWTUtilities;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.properties.WebAppProjectProperties;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FilteredResourcesSelectionDialog;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Selects an HTML host page from the given project's war directory.
 * 
 * TODO: If there are no HTML files to select, the checkbox will still stay
 * checked and the user will have to uncheck the box to continue.
 * 
 * TODO: The FilteredResourcesSelectionDialog is pretty heavy weight for what
 * we're trying to do here (just select an html page), eg, it makes it difficult
 * to know right at the start of the dialog how many items there are because it
 * fills the list in a number of asynchronous jobs (and there doesn't seem to be
 * any way to get notified when that's done), so we may want to use a different
 * dialog base, or make our own.
 * 
 */
public class WebAppHostPageSelectionDialog extends
    FilteredResourcesSelectionDialog {

  private class HostPageFilter extends ResourceFilter {
    public HostPageFilter() {
      /*
       * A new filter is created every time the filter text is changed, so we
       * can just tack a on the end of the pattern to make the pattern match
       * even if the user starts typing part of the file extension
       */
      patternMatcher.setPattern(patternMatcher.getPattern() + "*");
    }

    @Override
    public boolean matchItem(Object item) {
      IFile file = AdapterUtilities.getAdapter(item, IFile.class);
      if (!super.matchItem(item) || (file == null)) {
        return false;
      }

      if (!ResourceUtils.hasJspOrHtmlExtension(file)) {
        return false;
      }

      if (!warFolder.equals(file.getParent())) {
        return false;
      }

      return true;
    }
  }

  private static final String URL_LABEL_TEXT = "Final URL: ";
  private static final IStatus OK_STATUS = new Status(Status.OK,
      GWTPlugin.PLUGIN_ID, "");

  /**
   * Selects an HTML host page from a given project. Returns <code>null</code>
   * if the selection dialog was canceled.
   * 
   * @param javaProject the project from which to select a host page
   * @return a relative URL, or null if the selection dialog was canceled.
   * 
   */
  public static String show(IJavaProject javaProject, boolean isExternal) {
    Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    try {
      IProject project = javaProject.getProject();
      WebAppUtilities.verifyIsWebApp(project);

      IFolder warFolder = WebAppUtilities.getWarSrc(project);
      if (warFolder.exists()) {
        WebAppHostPageSelectionDialog dialog = new WebAppHostPageSelectionDialog(
            shell, javaProject, warFolder, isExternal);
        if (dialog.open() == OK) {
          Object[] result = dialog.getResult();
          IFile htmlFile = dialog.getSelectedFile(result);
          try {
            WebAppProjectProperties.setLaunchConfigExternalUrlPrefix(project,
                dialog.getExternalUrlPrefix());
          } catch (BackingStoreException e) {
            GWTPluginLog.logError(e);
          }
          return dialog.getUrl(warFolder, htmlFile);
        }
      }
    } catch (CoreException e) {
      CorePluginLog.logError(e);
    }

    return null;
  }

  private Text externalUrlPrefixText;
  private String externalUrlPrefixTextCache;
  private Group fileSelectionGroup;
  private final boolean isExternal;

  private IStatus oldStatus;
  private final IProject project;
  // superclass keeps status private, so we have to duplicate functionality
  private IStatus status;
  private boolean useFile = true;
  private Button useProjectFileCheckbox;
  private Label urlLabel;
  private IFile currentSelection;

  private final IFolder warFolder;

  private WebAppHostPageSelectionDialog(Shell shell, IJavaProject project,
      IFolder warFolder, boolean isExternal) {
    super(shell, false, project.getProject(), IResource.FILE);

    assert (warFolder != null);

    this.project = project.getProject();
    this.isExternal = isExternal;
    this.warFolder = warFolder;
    setTitle("HTML Page Selection");
    setMessage("");
  }

  @SuppressWarnings("restriction")
  @Override
  protected Control createDialogArea(Composite parent) {
    SWTFactory.createVerticalSpacer(parent, 16);
    if (isExternal) {
      createExternalUI(parent);
    }

    fileSelectionGroup = new Group(parent, SWT.SHADOW_ETCHED_IN);
    GridData gd = new GridData(GridData.FILL_BOTH);
    fileSelectionGroup.setLayoutData(gd);

    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    fileSelectionGroup.setLayout(layout);

    Control controlToReturn = super.createDialogArea(fileSelectionGroup);

    SWTFactory.createVerticalSpacer(parent, 8);
    Composite urlComposite = new Composite(parent, SWT.NONE);
    urlComposite.setLayout(layout);
    urlComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    new Label(urlComposite, SWT.NONE).setText(URL_LABEL_TEXT);

    gd = new GridData(GridData.FILL_HORIZONTAL);
    Group urlGroup = new Group(urlComposite, SWT.SHADOW_ETCHED_IN);
    urlGroup.setLayoutData(gd);
    urlGroup.setLayout(layout);

    gd = new GridData(GridData.FILL_BOTH);
    urlLabel = new Label(urlGroup, SWT.NONE);
    urlLabel.setLayoutData(gd);

    return controlToReturn;
  }

  @SuppressWarnings("restriction")
  protected Control createExternalRootContentArea(Composite parent) {

    Composite c = SWTFactory.createComposite(parent, 1, 1,
        GridData.FILL_HORIZONTAL);
    SWTFactory.createLabel(c, "External server root:", 1);
    externalUrlPrefixText = SWTFactory.createSingleText(c, 1);
    externalUrlPrefixTextCache = WebAppProjectProperties.getLaunchConfigExternalUrlPrefix(project);
    externalUrlPrefixText.setText(externalUrlPrefixTextCache);
    externalUrlPrefixText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        externalUrlPrefixTextCache = externalUrlPrefixText.getText();
        updateUrlLabelText();
      }
    });

    return c;
  }
  
  @SuppressWarnings("restriction")
  protected void createExternalUI(Composite parent) {
    createExternalRootContentArea(parent);
    SWTFactory.createVerticalSpacer(parent, 16);
    useProjectFileCheckbox = new Button(parent, SWT.CHECK);
    useProjectFileCheckbox.setText("Select an HTML page:");
    useProjectFileCheckbox.setSelection(true);
    useProjectFileCheckbox.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        boolean enabled = useProjectFileCheckbox.getSelection();
        useFile = enabled;
        SWTUtilities.setEnabledRecursive(fileSelectionGroup, enabled);

        if (enabled) {
          updateStatus(oldStatus);
        } else {
          // save the current status so that if the user re-enables the file
          // selector,
          // if it was in a bad state (ie, no selection), that state is
          // restored.
          if (status != null) {
            oldStatus = status;
          } else {
            oldStatus = OK_STATUS;
          }

          // it's ok to blast the current state because it doesn't depend on
          // anything new added to the UI (specifically, the external server
          // text)
          updateStatus(OK_STATUS);
        }

        updateUrlLabelText();
      }
    });
  }

  @Override
  protected ItemsFilter createFilter() {
    return new HostPageFilter();
  }

  protected IFile getSelectedFile(Object[] result) {
    if (result.length == 1 && result[0] instanceof IFile) {
      return (IFile) result[0];
    }
    return null;
  }

  protected String getUrl(IFolder warFolder, IFile selectedFile) {
    if (!isExternal && useFile) {
      if (selectedFile == null) {
        return null;
      }
      return selectedFile.getName();
    }

    String url = "";
    if (useFile && selectedFile != null) {
      url = selectedFile.getFullPath().removeFirstSegments(
          warFolder.getFullPath().segmentCount()).toString();
    }

    String externalURLPrefix = getExternalUrlPrefix();

    if (externalURLPrefix.length() > 0) {
      if (!externalURLPrefix.endsWith("/")) {
        externalURLPrefix += '/';
      }
    }

    return externalURLPrefix + url;
  }

  @Override
  protected void handleSelected(StructuredSelection selection) {
    super.handleSelected(selection);
    Object[] objs = selection.toArray();
    if (objs.length == 1) {
      if (objs[0] instanceof IFile) {
        currentSelection = (IFile) objs[0];
      } else {
        currentSelection = null;
      }
    } else {
      currentSelection = null;
    }
    updateUrlLabelText();
  }

  /**
   * The superclass keeps the status private, and doesn't provide a getter, so
   * we have to override updateStatus and keep track of it ourselves.
   */
  @Override
  protected void updateStatus(IStatus status) {
    this.status = status;
    super.updateStatus(status);
  }

  protected void updateUrlLabelText() {

    String url = getUrl(warFolder, currentSelection);

    if (url == null) {
      url = "";
    }

    urlLabel.setText(url);
  }

  private String getExternalUrlPrefix() {
    if (isExternal) {
      return externalUrlPrefixTextCache.trim();
    }

    return "";
  }
}
