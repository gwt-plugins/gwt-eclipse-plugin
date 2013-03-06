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
package com.google.gdt.eclipse.core.formatter;

import org.eclipse.jface.text.IDocument;

/**
 * Clones an IDocument. The clone should be as similar to the original as
 * possible. For example, besides having the same text (obviously), they should
 * also have the same partitionings, and if they are backed by models, they
 * should also be backed by equivalent models.
 */
public interface IDocumentCloner {

  /**
   * Returns a clone of the original IDocument.
   */
  IDocument clone(IDocument original);

  /**
   * Release the cloned document. This may not be required for all
   * implementations.
   */
  void release(IDocument clone);

}
