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
package com.google.gdt.eclipse.platform.jdt.text.correction.proposals;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.swt.graphics.Image;

/**
 * @see org.eclipse.jdt.internal.ui.text.correction.proposals.ASTRewriteCorrectionProposal
 */
@SuppressWarnings("restriction")
public class ASTRewriteCorrectionProposal
    extends
    org.eclipse.jdt.internal.ui.text.correction.proposals.ASTRewriteCorrectionProposal {

  public ASTRewriteCorrectionProposal(String name, ICompilationUnit cu,
      ASTRewrite rewrite, int relevance, Image image) {
    super(name, cu, rewrite, relevance, image);
  }

}
