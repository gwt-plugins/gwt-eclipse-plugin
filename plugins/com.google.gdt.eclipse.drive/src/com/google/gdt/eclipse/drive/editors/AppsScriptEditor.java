/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/

package com.google.gdt.eclipse.drive.editors;

import com.google.gdt.eclipse.drive.DrivePlugin;
import com.google.gdt.eclipse.drive.editors.webautocomplete.AutocompleteEntryHolder;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.CompilationUnitEditor;

/**
 * The Eclipse editor associated with Apps Script (.gs) files. It is based on the default
 * JavaScript editor, {@link CompilationUnitEditor}, but overrides saving behavior and content
 * assist.
 */
@SuppressWarnings("restriction") // CompilationUnitEditor
public class AppsScriptEditor extends CompilationUnitEditor {
  
  public static final String CONTENT_TYPE = "com.google.gdt.eclipse.drive.appsScriptContentType";

  @Override
  protected void initializeEditor() {
    super.initializeEditor();
    SourceViewerConfiguration parent = getSourceViewerConfiguration();
    setSourceViewerConfiguration(
        new DelegatingSourceViewerConfiguration(parent){
          @Override public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
            ContentAssistant assistant = new ContentAssistant();
            configureContentAssistant(assistant);
            return assistant;
          }
        });
  }
  
  private static void configureContentAssistant(ContentAssistant assistant) {
    AutocompleteEntryHolder apiData =
        ApiDocumentationService.translateBeanDocumentations(HardCodedApiInfo.getApiInfo());
    assistant.setContentAssistProcessor(
        WebEditorCompletionProcessor.make(apiData), IDocument.DEFAULT_CONTENT_TYPE);
    assistant.setInformationControlCreator(
        new IInformationControlCreator() {
          @Override public IInformationControl createInformationControl(Shell parent) {
            return new DefaultInformationControl(parent, true);
          }
        });
    assistant.enableAutoActivation(true);
    assistant.setAutoActivationDelay(0);
  }
  
  /**
   * Reports the editor input as dirty even if it is an unmodified copy of the Eclipse file, if that
   * file has not been saved in Drive. This enables the Save button, allowing the user to retry a
   * failed save to Drive without modifying the file.
   * 
   * @return
   *     {@code true} if either the file being edited has been modified since it was last opened or
   *     saved, or if the version saved in Eclipse has not been saved to Drive; {@code false}
   *     otherwise
   */
  @Override
  public boolean isDirty() {
    if (super.isDirty()) {
      return true;
    }
    IFile file = ((IFileEditorInput) getEditorInput()).getFile();
    return DrivePlugin.getDefault().getPendingSaveManager().isUnsaved(file);
  }
}
