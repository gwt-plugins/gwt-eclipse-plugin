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

import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;

/**
 * Scans for JSNI blocks, identified by the beginning and end tokens.
 */
public class GWTPartitionScanner extends RuleBasedPartitionScanner {

  public GWTPartitionScanner() {
    IToken jsniMethod = new Token(GWTPartitions.JSNI_METHOD);
    MultiLineRule jsniRule = new MultiLineRule("/*-{", "}-*/", jsniMethod);

    IPredicateRule[] result = new IPredicateRule[] {jsniRule};
    setPredicateRules(result);
  }

}
