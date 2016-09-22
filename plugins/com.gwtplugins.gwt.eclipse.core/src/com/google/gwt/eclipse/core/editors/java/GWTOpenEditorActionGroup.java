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
package com.google.gwt.eclipse.core.editors.java;

import com.google.gdt.eclipse.core.AdapterUtilities;
import com.google.gwt.eclipse.core.validators.java.JsniJavaRef;
import com.google.gwt.eclipse.core.validators.java.JsniParser;
import com.google.gwt.eclipse.core.validators.java.UnresolvedJsniJavaRefException;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jdt.ui.actions.JdtActionConstants;
import org.eclipse.jdt.ui.actions.OpenAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.OpenWithMenu;

/**
 * Replaces {@link org.eclipse.jdt.ui.actions#OpenEditorActionGroup} and adds
 * behavior to open declarations on Java references within JSNI blocks. All of
 * the code besides the inner GWTOpenAction class and the references to it from
 * the GWTOpenEditorActionGroup constructors are identical to the original
 * OpenEditorActionGroup.
 * 
 * Action group that adds the actions opening a new editor to the context menu
 * and the action bar's navigate menu.
 */
@SuppressWarnings("restriction")
public class GWTOpenEditorActionGroup extends ActionGroup {

  private static class GWTOpenAction extends OpenAction {

    private JavaEditor editor;

    public GWTOpenAction(IWorkbenchSite site) {
      super(site);
    }

    public GWTOpenAction(JavaEditor editor) {
      super(editor);
      this.editor = editor;
    }

    // TODO: may be refactored to also support Ctrl-click hyperlink navigation
    @Override
    public void run(ITextSelection selection) {
      IDocument document = editor.getDocumentProvider().getDocument(
          editor.getEditorInput());

      ITypedRegion jsniRegion = JsniParser.getEnclosingJsniRegion(selection,
          document);
      if (jsniRegion == null) {
        // Let the Java Editor do its thing outside of JSNI blocks
        super.run(selection);
        return;
      }

      ITextSelection jsniBlock = new TextSelection(jsniRegion.getOffset(),
          jsniRegion.getLength());

      // TODO: once we cache Java references in JSNI blocks (for Java search and
      // refactoring integration) we can just query that to figure out if a Java
      // reference contains the current selection

      // Figure out if the selection is inside a Java reference
      JsniJavaRef javaRef = JsniJavaRef.findEnclosingJavaRef(selection,
          jsniBlock, document);
      if (javaRef == null) {
        return;
      }

      IJavaProject project = EditorUtility.getEditorInputJavaElement(editor,
          false).getJavaProject();

      // Finally, try to resolve the Java reference
      try {
        IJavaElement element = javaRef.resolveJavaElement(project);

        // We found a match so delegate to the OpenAction.run(Object[]) method
        run(new Object[] {element});

      } catch (UnresolvedJsniJavaRefException e) {
        // Could not resolve the Java reference, so do nothing
      }
    }
  }

  private boolean isEditorOwner;
  private OpenAction openAction;
  private IWorkbenchSite site;

  /**
   * Creates a new <code>GWTOpenEditorActionGroup</code>. The group requires
   * that the selection provided by the part's selection provider is of type
   * <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
   * 
   * @param part the view part that owns this action group
   */
  public GWTOpenEditorActionGroup(IViewPart part) {
    site = part.getSite();
    openAction = new GWTOpenAction(site);
    openAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_EDITOR);
    initialize(site.getSelectionProvider());
  }

  /**
   * Note: This constructor is for internal use only. Clients should not call
   * this constructor.
   * 
   * @param editor the Java editor
   */
  public GWTOpenEditorActionGroup(JavaEditor editor) {
    isEditorOwner = true;
    openAction = new GWTOpenAction(editor);
    openAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_EDITOR);
    editor.setAction("OpenEditor", openAction); //$NON-NLS-1$
    site = editor.getEditorSite();
    initialize(site.getSelectionProvider());
  }

  /*
   * @see ActionGroup#dispose()
   */
  @Override
  public void dispose() {
    ISelectionProvider provider = site.getSelectionProvider();
    provider.removeSelectionChangedListener(openAction);
    super.dispose();
  }

  /*
   * (non-Javadoc) Method declared in ActionGroup
   */
  @Override
  public void fillActionBars(IActionBars actionBar) {
    super.fillActionBars(actionBar);
    setGlobalActionHandlers(actionBar);
  }

  /*
   * (non-Javadoc) Method declared in ActionGroup
   */
  @Override
  public void fillContextMenu(IMenuManager menu) {
    super.fillContextMenu(menu);
    appendToGroup(menu, openAction);
    if (!isEditorOwner) {
      addOpenWithMenu(menu);
    }
  }

  /**
   * Returns the open action managed by this action group.
   * 
   * @return the open action. Returns <code>null</code> if the group doesn't
   *         provide any open action
   */
  public IAction getOpenAction() {
    return openAction;
  }

  private void addOpenWithMenu(IMenuManager menu) {
    ISelection selection = getContext().getSelection();
    if (selection.isEmpty() || !(selection instanceof IStructuredSelection)) {
      return;
    }
    IStructuredSelection ss = (IStructuredSelection) selection;
    if (ss.size() != 1) {
      return;
    }

    Object o = ss.getFirstElement();
    IFile file = AdapterUtilities.getAdapter(o, IFile.class);
    if (file == null) {
      return;
    }
    
    // Create a menu.
    IMenuManager submenu = new MenuManager(ActionMessages.OpenWithMenu_label);
    submenu.add(new OpenWithMenu(site.getPage(), file));

    // Add the submenu.
    menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, submenu);
  }

  private void appendToGroup(IMenuManager menu, IAction action) {
    if (action.isEnabled()) {
      menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, action);
    }
  }

  private void initialize(ISelectionProvider provider) {
    ISelection selection = provider.getSelection();
    openAction.update(selection);
    if (!isEditorOwner) {
      provider.addSelectionChangedListener(openAction);
    }
  }

  private void setGlobalActionHandlers(IActionBars actionBars) {
    actionBars.setGlobalActionHandler(JdtActionConstants.OPEN, openAction);
  }
}
