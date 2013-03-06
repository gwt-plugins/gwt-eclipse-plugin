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

import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.formatter.AbstractFormattingStrategy;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.JavaScriptCore;

/**
 * Formatting strategy for JSNI methods.
 */
public class JsniFormattingStrategy extends AbstractFormattingStrategy {

  @Override
  public void format(IDocument document, TypedPosition partition) {
    if (!partition.getType().equals(GWTPartitions.JSNI_METHOD)) {
      return;
    }

    try {
      /*
       * JavaScriptCore.getOptions() will return the global options, and this
       * is ok because GWT projects are Java projects and not JavaScript projects,
       * and only JavaScript projects can have project-specific JS formatting
       * options.
       */
      TextEdit edit = JsniFormattingUtil.format(document, partition,
          getPreferences(), JavaScriptCore.getOptions(), null);
      if (edit != null) {
        edit.apply(document);
      }
    } catch (MalformedTreeException exception) {
      GWTPluginLog.logError(exception);
    } catch (BadLocationException exception) {
      GWTPluginLog.logError(exception);
    }
  }
}
