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
package com.google.gwt.eclipse.core.dialogs;

import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.modules.IModule;
import com.google.gwt.eclipse.core.modules.ModuleFile;
import com.google.gwt.eclipse.core.modules.ModuleJarResource;
import com.google.gwt.eclipse.core.modules.ModuleUtils;
import com.google.gwt.eclipse.core.resources.GWTImages;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;
import org.eclipse.ui.dialogs.SearchPattern;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import java.text.Collator;
import java.util.Comparator;

/**
 * Dialog used for choosing a GWT Module. When the OK button is pressed, an
 * IModule is returned to the caller.
 */
@SuppressWarnings("restriction")
public class ModuleSelectionDialog extends FilteredItemsSelectionDialog {

  /**
   * A label provider for details of IModule objects.
   * 
   * This code was adapted from the
   * FilteredTypesSelectionDialog.TypeItemDetailsLabelProvider class.
   */
  private class ModuleItemDetailsLabelProvider extends ModuleItemLabelProvider {

    @Override
    public Image getImage(Object element) {
      if (!(element instanceof IModule)) {
        return super.getImage(element);
      }

      Image image = null;

      IModule module = (IModule) element;
      if (!module.isBinary()) {
        ModuleFile moduleFile = (ModuleFile) module;
        IFile file = moduleFile.getFile();
        IPackageFragment pkFrag;
        try {
          /*
           * TODO: Move logic for retrieving a module's package fragment into
           * IModule.
           */
          pkFrag = javaProject.findPackageFragment(file.getParent().getFullPath());
          image = provider.getImage(pkFrag);
        } catch (JavaModelException e) {
          GWTPluginLog.log(IStatus.WARNING, IStatus.OK,
              "Unable to retrieve image for module "
                  + module.getQualifiedName() + " package.", e);
        }
      }

      return image;
    }

    @Override
    public String getText(Object element) {
      if (!(element instanceof IModule)) {
        return super.getText(element);
      }
      String text = null;

      IModule module = (IModule) element;
      String packageName = module.getPackageName();

      if (!module.isBinary()) {
        ModuleFile moduleFile = (ModuleFile) module;
        IFile file = moduleFile.getFile();
        String modulePath = file.getFullPath().makeRelative().toString();
        text = packageName + " - " + modulePath;
      } else {
        ModuleJarResource moduleJarResource = (ModuleJarResource) module;
        IJarEntryResource jarEntryResource = moduleJarResource.getJarEntryResource();
        String jarPath = jarEntryResource.getPackageFragmentRoot().getPath().makeRelative().toString();
        text = packageName + " - " + jarPath;
      }

      return text;
    }
  }

  /**
   * A label provider for IModule objects.
   * 
   * This code was adapted from the
   * FilteredTypesSelectionDialog.TypeItemLabelProvider class.
   */
  private static class ModuleItemLabelProvider extends LabelProvider {

    /*
     * Provides basic labels for workbench resources that implement
     * IWorkbenchAdapter
     */
    WorkbenchLabelProvider provider = new WorkbenchLabelProvider();

    public ModuleItemLabelProvider() {
      super();
    }

    @Override
    public void dispose() {
      provider.dispose();
      super.dispose();
    }

    @Override
    public Image getImage(Object element) {
      if (!(element instanceof IModule)) {
        return super.getImage(element);
      }
      return GWTPlugin.getDefault().getImage(GWTImages.MODULE_ICON);
    }

    @Override
    public String getText(Object element) {
      if (!(element instanceof IModule)) {
        return super.getText(element);
      }

      IModule module = (IModule) element;

      return module.getSimpleName() + " - " + module.getPackageName();
    }
  }

  /**
   * Filters the items in the dialog list based on the pattern entered in the
   * filter text field of the dialog.
   */
  private class ModuleItemsFilter extends ItemsFilter {

    private SearchPattern packageMatcher;

    public ModuleItemsFilter() {
      super(new ModuleSearchPattern());

      /*
       * If there is no filter pattern present, initialize the pattern to '*'.
       * This has the nice property of pre-populating the dialog list with all
       * possible matches when it is first shown.
       */
      if (patternMatcher.getPattern() == null
          || patternMatcher.getPattern().length() == 0) {
        patternMatcher.setPattern("*");
      }

      // If a package pattern is present in the filter text, then set up
      // a packageMatcher to do matching based on the module's package.
      String stringPackage = ((ModuleSearchPattern) patternMatcher).getPackagePattern();
      if (stringPackage != null) {
        packageMatcher = new SearchPattern();
        packageMatcher.setPattern(stringPackage);
      } else {
        packageMatcher = null;
      }
    }

    @Override
    public boolean equalsFilter(ItemsFilter iFilter) {
      if (!super.equalsFilter(iFilter)) {
        return false;
      }
      if (!(iFilter instanceof ModuleItemsFilter)) {
        return false;
      }
      ModuleItemsFilter moduleTypeItemsFilter = (ModuleItemsFilter) iFilter;
      String packagePattern = getPackagePattern();
      String filterPackagePattern = moduleTypeItemsFilter.getPackagePattern();
      if (packagePattern == null) {
        return filterPackagePattern == null;
      }
      return packagePattern.equals(filterPackagePattern);
    }

    public String getPackagePattern() {
      if (packageMatcher == null) {
        return null;
      }
      return packageMatcher.getPattern();
    }

    @Override
    public boolean isConsistentItem(Object item) {
      /*
       * TODO: For now, let's say that everything is consistent. That is, don't
       * worry about file deletions happening while the dialog is open (it is a
       * modal dialog; can this happen? What if there are changes from the
       * shell? how does this thing know about them?).
       */
      return true;
    }

    @Override
    public boolean isSubFilter(ItemsFilter filter) {
      if (!super.isSubFilter(filter)) {
        return false;
      }
      ModuleItemsFilter moduleTypeItemsFilter = (ModuleItemsFilter) filter;
      String packagePattern = getPackagePattern();
      String filterPackagePattern = moduleTypeItemsFilter.getPackagePattern();
      if (filterPackagePattern == null) {
        return packagePattern == null;
      } else if (packagePattern == null) {
        return true;
      }
      return filterPackagePattern.startsWith(packagePattern)
          && filterPackagePattern.indexOf('.', packagePattern.length()) == -1;
    }

    public boolean matchesRawNamePattern(IModule module) {
      return Strings.startsWithIgnoreCase(module.getSimpleName(), getPattern());
    }

    @Override
    public boolean matchesRawNamePattern(Object item) {
      IModule module = (IModule) item;
      return matchesRawNamePattern(module);
    }

    @Override
    public boolean matchItem(Object item) {

      IModule module = (IModule) item;
      if (!matchesPackage(module)) {
        return false;
      }
      return matchesName(module);
    }

    private boolean matchesName(IModule module) {
      return matches(module.getSimpleName());
    }

    private boolean matchesPackage(IModule module) {
      if (packageMatcher == null) {
        return true;
      }
      return packageMatcher.matches(module.getPackageName());
    }
  }

  /**
   * Extends functionality of SearchPatterns.
   * 
   * This code was adapted from the
   * FilteredTypesSelectionDialog.TypeSearchPattern class.
   */
  private static class ModuleSearchPattern extends SearchPattern {

    private String packagePattern;

    public String getPackagePattern() {
      return packagePattern;
    }

    @Override
    public void setPattern(String stringPattern) {
      String pattern = stringPattern;
      String packPattern = null;
      int index = stringPattern.lastIndexOf("."); //$NON-NLS-1$
      if (index != -1) {
        packPattern = evaluatePackagePattern(stringPattern.substring(0, index));
        pattern = stringPattern.substring(index + 1);
        if (pattern.length() == 0) {
          pattern = "**"; //$NON-NLS-1$
        }
      }
      super.setPattern(pattern);
      packagePattern = packPattern;
    }

    /*
     * Transforms o.e.j to o.e.j
     */
    private String evaluatePackagePattern(String s) {
      StringBuffer buf = new StringBuffer();
      boolean hasWildCard = false;
      for (int i = 0; i < s.length(); i++) {
        char ch = s.charAt(i);
        if (ch == '.') {
          if (!hasWildCard) {
            buf.append('*');
          }
          hasWildCard = false;
        } else if (ch == '*' || ch == '?') {
          hasWildCard = true;
        }
        buf.append(ch);
      }
      if (!hasWildCard) {
        buf.append('*');
      }
      return buf.toString();
    }
  }

  /*
   * Key used for the storage/retrieval of this dialog's settings from the IDE's
   * persistent data store.
   */
  private static final String DIALOG_SETTINGS = GWTPlugin.PLUGIN_ID
      + ".dialogs.ModuleInheritsSelectionDialog";

  /**
   * Create a new ModuleSelectionDialog and display it.
   * 
   * @param shell The current display shell
   * @param project The Java project that should be searched for GWT Modules
   * @param showModulesInJars true if modules that are located in jars on the
   *          project's classpath should be displayed
   * 
   * @return the IModule corresponding to the module chosen from the list by
   *         pressing the OK button, null if CANCEL was pressed.
   */
  public static IModule show(Shell shell, IJavaProject project,
      boolean showModulesInJars) {
    ModuleSelectionDialog dialog = new ModuleSelectionDialog(shell, project,
        showModulesInJars);
    if (dialog.open() == OK) {
      Object[] result = dialog.getResult();
      assert (result.length == 1 && result[0] instanceof IModule);
      IModule module = (IModule) result[0];
      return module;
    }

    return null;
  }

  /**
   * Helper method to return the absolute workspace path of a GWT Module.
   * 
   * If the module file is located in a JAR, then the absolute path of the JAR
   * on the file system is returned.
   */
  private static IPath getPathForModule(IModule module) {

    if (module == null) {
      return null;
    }

    if (!module.isBinary()) {
      ModuleFile moduleFile = (ModuleFile) module;
      IFile file = moduleFile.getFile();
      return file.getFullPath();
    }

    ModuleJarResource moduleJarResource = (ModuleJarResource) module;
    IJarEntryResource jarEntryResource = moduleJarResource.getJarEntryResource();
    return jarEntryResource.getPackageFragmentRoot().getPath();
  }

  private IJavaProject javaProject;
  private boolean showModulesInJars;

  private ModuleSelectionDialog(Shell shell, IJavaProject javaProject,
      boolean showModulesInJars) {
    super(shell);
    this.javaProject = javaProject;
    this.showModulesInJars = showModulesInJars;
    setTitle("GWT Module Selection");
    setListLabelProvider(new ModuleItemLabelProvider());
    setDetailsLabelProvider(new ModuleItemDetailsLabelProvider());
  }

  @Override
  public String getElementName(Object item) {
    IModule module = (IModule) item;
    return module.getSimpleName();
  }

  @Override
  protected Control createExtendedContentArea(Composite parent) {
    // No need for an extended content area.
    return null;
  }

  @Override
  protected ItemsFilter createFilter() {
    return new ModuleItemsFilter();
  }

  @Override
  protected void fillContentProvider(AbstractContentProvider contentProvider,
      ItemsFilter itemsFilter, IProgressMonitor progressMonitor) {

    /*
     * TODO: Should probably try and use the progressMonitor appropriately here.
     * If we end up searching the whole workspace for modules, this could take a
     * while, and we need some accurate indication of progress.
     */

    IModule[] projectModules = ModuleUtils.findAllModules(javaProject,
        showModulesInJars);

    for (IModule module : projectModules) {
      contentProvider.add(module, itemsFilter);
    }
  }

  @Override
  protected IDialogSettings getDialogSettings() {
    IDialogSettings settings = IDEWorkbenchPlugin.getDefault().getDialogSettings().getSection(
        DIALOG_SETTINGS);

    if (settings == null) {
      settings = IDEWorkbenchPlugin.getDefault().getDialogSettings().addNewSection(
          DIALOG_SETTINGS);
    }
    return settings;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected Comparator<?> getItemsComparator() {
    return new Comparator() {
      public int compare(Object o1, Object o2) {
        Collator collator = Collator.getInstance();
        IModule module1 = (IModule) o1;
        IModule module2 = (IModule) o2;

        // Compare module names

        String s1 = module1.getSimpleName();
        String s2 = module2.getSimpleName();

        int comparability = collator.compare(s1, s2);

        // If module names are identical, then compare
        // fully-qualified module names

        if (comparability == 0) {
          s1 = module1.getQualifiedName();
          s2 = module2.getQualifiedName();
          comparability = collator.compare(s1, s2);
        }

        // If fully-qualified module names are identical, then
        // compare file paths.

        if (comparability == 0) {
          s1 = getPathForModule(module1).toString();
          s2 = getPathForModule(module2).toString();
          comparability = collator.compare(s1, s2);
        }

        return comparability;
      }
    };
  }

  @Override
  protected IStatus validateItem(Object item) {
    /*
     * There is nothing to validate. As long as an item has been selected when
     * the OK button is pressed (and the dialog will not let you press the OK
     * button unless an item has been selected), there is nothing else to check.
     */
    return new Status(Status.OK_STATUS.getSeverity(), GWTPlugin.PLUGIN_ID, 0,
        "", null);
  }

}
