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
package com.google.gdt.eclipse.gph.egit.wizard;

import com.google.gdt.eclipse.gph.egit.EGitCheckoutProviderPlugin;
import com.google.gdt.eclipse.gph.wizards.AbstractWizardPage;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.op.CloneOperation;
import org.eclipse.egit.core.securestorage.UserPasswordCredentials;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

// TODO: we need to give the user a way to select the initial branch??

// TODO: add green checkmarks next to the three stages to show completeness

// TODO: issues with selection change in the repoList

// TODO: selection should be sticky - unselect doesn't clear folder
// re-select of same doesn't clear folder

// TODO: push asked me for my password -

/**
 * A general error page for wizards.
 */
public class CloneRepositoryWizardPage extends AbstractWizardPage {
  private static final String GIT_DIRECTORY_NAME = ".git";

  private Button cloneButton;

  private CLabel cloneFeedbackLabel;

  private Text destinationText;

  private TableViewer repoList;
  private EGitCheckoutWizard wizard;

  /**
   * Create a new SelectRepositoryPage.
   * 
   * @param wizard
   */
  public CloneRepositoryWizardPage(EGitCheckoutWizard wizard) {
    super("selectRepositoryPage");

    this.wizard = wizard;

    setTitle("Clone the remote Git repository");
    setDescription("Select a remote repository URL, select the local destination directory, and clone the repository");
    setImageDescriptor(ImageDescriptor.createFromImage(wizard.getDefaultPageImage()));
  }

  protected IRunnableWithProgress createCloneRunnable() {
    final String repoStr = getCurrentRepo();
    final File parentDir = getDestinationDirectory();

    return new IRunnableWithProgress() {
      public void run(IProgressMonitor monitor)
          throws InvocationTargetException, InterruptedException {

        URIish urlish = null;

        try {
          urlish = new URIish(repoStr);
        } catch (URISyntaxException exception) {
          throw new InvocationTargetException(exception);
        }

        int timeout = org.eclipse.egit.ui.Activator.getDefault().getPreferenceStore().getInt(
            UIPreferences.REMOTE_CONNECTION_TIMEOUT);

        CloneOperation cloneOperation = new CloneOperation(urlish, true, null,
            parentDir, null, // "refs/heads/trunk", "refs/heads/master"?
            "origin", timeout);

        UserPasswordCredentials credentials = new UserPasswordCredentials(
            wizard.getGPHProject().getUser().getUserName(),
            wizard.getGPHProject().getUser().getRepoPassword());
        cloneOperation.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
            credentials.getUser(), credentials.getPassword()));

        cloneOperation.run(monitor);

        RepositoryUtil util = Activator.getDefault().getRepositoryUtil();

        util.addConfiguredRepository(cloneOperation.getGitDir());
      }
    };
  }

  @Override
  protected Control createPageContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayoutFactory.swtDefaults().margins(10, 10).applyTo(composite);

    Group selectRepoGroup = new Group(composite, SWT.NONE);
    selectRepoGroup.setText("Repository to import from");
    GridDataFactory.fillDefaults().grab(true, false).applyTo(selectRepoGroup);
    GridLayoutFactory.swtDefaults().applyTo(selectRepoGroup);

    repoList = new TableViewer(selectRepoGroup, SWT.SINGLE | SWT.BORDER);
    repoList.setLabelProvider(new RepositoryLabelProvider());
    repoList.setContentProvider(new ArrayContentProvider());
    repoList.setComparator(new ViewerComparator());
    repoList.setInput(wizard.getGPHProject().getRepoUrls());
    Object firstElement = repoList.getElementAt(0);
    if (firstElement != null) {
      repoList.setSelection(new StructuredSelection(firstElement));
    }
    repoList.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        handleRepoSelectionChanged();
      }
    });
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).hint(
        SWT.DEFAULT, 80).applyTo(repoList.getTable());

    Group selectLocalDirGroup = new Group(composite, SWT.NONE);
    selectLocalDirGroup.setText("Select local destination directory");
    GridDataFactory.fillDefaults().grab(true, true).applyTo(selectLocalDirGroup);
    GridLayoutFactory.swtDefaults().numColumns(3).applyTo(selectLocalDirGroup);

    Label label = new Label(selectLocalDirGroup, SWT.NONE);
    label.setText("Destination:");

    destinationText = new Text(selectLocalDirGroup, SWT.BORDER | SWT.SINGLE);
    destinationText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        updateButtons();
      }
    });
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(
        destinationText);

    Button button = new Button(selectLocalDirGroup, SWT.PUSH);
    button.setText("Browse...");
    PixelConverter converter = new PixelConverter(button);
    int widthHint = converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
    GridDataFactory.swtDefaults().hint(widthHint, -1).applyTo(button);
    button.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        performSelectLocalDirectory();
      }
    });

    Group cloneRepoGroup = new Group(composite, SWT.NONE);
    cloneRepoGroup.setText("Clone repository");
    GridDataFactory.fillDefaults().grab(true, true).applyTo(cloneRepoGroup);
    GridLayoutFactory.swtDefaults().numColumns(3).applyTo(cloneRepoGroup);

    cloneFeedbackLabel = new CLabel(cloneRepoGroup, SWT.NONE);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(
        cloneFeedbackLabel);

    cloneButton = new Button(cloneRepoGroup, SWT.PUSH);
    cloneButton.setText("Clone");
    GridDataFactory.swtDefaults().hint(widthHint, -1).applyTo(cloneButton);
    cloneButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        performCloneRepo();
      }
    });

    handleRepoSelectionChanged();

    return composite;
  }

  protected void performSelectLocalDirectory() {
    DirectoryDialog dialog = new DirectoryDialog(getShell());

    dialog.setText("Select Destination Directory");
    dialog.setMessage("Select the destination directory for the repository clone");

    if (destinationText.getText().length() > 0) {
      File file = new File(destinationText.getText());

      if (file.getParentFile() != null && file.getParentFile().exists()) {
        dialog.setFilterPath(file.getParent());
      }
    }

    String path = dialog.open();

    if (path != null) {
      destinationText.setText(path);

      updateButtons();
    }
  }

  protected void performCloneRepo() {
    try {
      wizard.getContainer().run(true, true, createCloneRunnable());
    } catch (InvocationTargetException exception) {
      Throwable ex = exception.getTargetException();

      if (ex == null) {
        ex = exception;
      }

      if (exception.getTargetException() instanceof OperationCanceledException) {
        // ignore
      } else {
        EGitCheckoutProviderPlugin.logError("Error during egit clone", ex);

        ErrorDialog.openError(
            getShell(),
            "Error Performing Git Clone",
            "Unable to clone the repository: " + ex.toString(),
            EGitCheckoutProviderPlugin.createStatus(IStatus.ERROR,
                ex.getMessage(), ex));
      }
    } catch (InterruptedException exception) {
      // ignore
    }

    updateButtons();
  }

  private String getCurrentRepo() {
    IStructuredSelection sel = (IStructuredSelection) repoList.getSelection();

    return (String) sel.getFirstElement();
  }

  private void handleRepoSelectionChanged() {
    String repo = getCurrentRepo();

    if (repo == null) {
      destinationText.setText("");
    } else {
      File dir = getLocalDirForRepo(repo);

      if (dir == null) {
        destinationText.setText("");
      } else {
        destinationText.setText(dir.getAbsolutePath());
      }
    }

    updateButtons();
  }

  private File getLocalDirForRepo(String repoURL) {
    RepositoryUtil repoUtil = Activator.getDefault().getRepositoryUtil();

    for (String configuredRepo : repoUtil.getConfiguredRepositories()) {
      try {
        File repoFile = new File(configuredRepo);

        Repository repository = Activator.getDefault().getRepositoryCache().lookupRepository(
            repoFile);

        try {
          RemoteConfig originConfig = new RemoteConfig(repository.getConfig(),
              "origin");

          for (URIish uri : originConfig.getURIs()) {
            String uriStr = uri.toString();

            if (repoURL.equals(uriStr)) {
              return repoFile.getParentFile();
            }
          }
        } catch (URISyntaxException exception) {

        }
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    }

    return null;
  }

  protected File getDestinationDirectory() {
    if (destinationText.getText().length() > 0) {
      return new File(destinationText.getText());
    } else {
      return null;
    }
  }

  private void updateButtons() {
    String currentRepo = getCurrentRepo();
    File destDir = getDestinationDirectory();

    boolean canTransition = false;
    boolean needsClone = false;

    if (currentRepo != null) {
      if (destDir == null) {
        cloneFeedbackLabel.setImage(null);
        cloneFeedbackLabel.setText("Choose a directory to clone the repository into");
      } else if (isValidClone(destDir)) {
        canTransition = true;

        cloneFeedbackLabel.setImage(EGitCheckoutProviderPlugin.getImage("repository_valid.gif"));
        cloneFeedbackLabel.setText(destDir.getName()
            + " is a valid cloned repository");
      } else {
        if (destDir.exists() && destDir.isDirectory()) {
          needsClone = true;
        }

        cloneFeedbackLabel.setImage(null);
        cloneFeedbackLabel.setText("Clone repository into "
            + destinationText.getText());
      }
    } else {
      cloneFeedbackLabel.setText("");
    }

    cloneButton.setEnabled(needsClone);

    setPageComplete(canTransition);
  }

  private boolean isValidClone(File destDir) {
    if (destDir.exists() && destDir.isDirectory()
        && GIT_DIRECTORY_NAME.equals(destDir.getName())) {
      return true;
    } else {
      File child = new File(destDir, ".git");

      if (child.exists() && child.isDirectory()
          && GIT_DIRECTORY_NAME.equals(child.getName())) {
        return true;
      } else {
        return false;
      }

      // TODO: look for the repo last dir, + .git?

    }
  }
}
