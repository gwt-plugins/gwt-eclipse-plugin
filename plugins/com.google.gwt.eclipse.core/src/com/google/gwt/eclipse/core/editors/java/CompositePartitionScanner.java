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

import org.eclipse.jdt.ui.text.IJavaPartitions;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.IPartitionTokenScanner;
import org.eclipse.jface.text.rules.IToken;

/**
 * Scans documents looking for Java and JSNI partition tokens.
 */
public class CompositePartitionScanner implements IPartitionTokenScanner,
    IJavaPartitions {

  private IDocument document;

  private final IPartitionTokenScanner javaScanner;

  private final IPartitionTokenScanner jsniScanner;

  public CompositePartitionScanner(IPartitionTokenScanner javaScanner,
      IPartitionTokenScanner jsniScanner) {
    this.javaScanner = javaScanner;
    this.jsniScanner = jsniScanner;
  }

  public int getTokenLength() {
    return javaScanner.getTokenLength();
  }

  public int getTokenOffset() {
    return javaScanner.getTokenOffset();
  }

  public IToken nextToken() {
    IToken token = javaScanner.nextToken();
    Object data = token.getData();
    if (JAVA_MULTI_LINE_COMMENT.equals(data)) {
      jsniScanner.setRange(document, javaScanner.getTokenOffset(),
          javaScanner.getTokenLength());
      IToken jsniToken = jsniScanner.nextToken();
      if (jsniToken.getData() != null) {
        return jsniToken;
      }
    }
    return token;
  }

  public void setPartialRange(IDocument document, int offset, int length,
      String contentType, int partitionOffset) {
    this.document = document;
    if (GWTPartitions.JSNI_METHOD.equals(contentType)) {
      javaScanner.setPartialRange(document, offset, length,
          JAVA_MULTI_LINE_COMMENT, partitionOffset);
    } else {
      javaScanner.setPartialRange(document, offset, length, contentType,
          partitionOffset);
    }
    jsniScanner.setPartialRange(document, offset, length, contentType,
        partitionOffset);
  }

  public void setRange(IDocument document, int offset, int length) {
    this.document = document;
    javaScanner.setRange(document, offset, length);
    jsniScanner.setRange(document, offset, length);
  }
}
