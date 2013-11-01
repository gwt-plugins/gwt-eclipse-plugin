/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.drive.editors.webautocomplete;

//Copyright 2011 Google Inc. All Rights Reserved.

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
* Encapsulates the concept of a parsing expression
*
* @author ostrulovich@google.com (Omer Strulovich)
*/
class Expression implements Iterable<Segment> {
private final List<Segment> segments = Lists.newArrayList();

/**
* Adds a new segment in the start of the expression
* @param segment the segment to push into the expression
*/
void push(Segment segment) {
 segments.add(0, segment);
}

@Override public Iterator<Segment> iterator() {
 return segments.iterator();
}

/**
* Creates a string which rebuilds the expression as a javascript identifier made of several
* fields under an object. For example [a, b, c] will be built to "a.b.c"
*/
String rebuildAsVariable() {
 return Joiner.on('.').join(segments).trim();
}

/**
* Substitutes a sub-expression of this expression in the given sub-expression.
*
* Replaces the sub-expression of this expression, starting in fromIndex (inclusive) and ending
* in toIndex (exclusive)
* @return a new expression with the substitution made
*/
@VisibleForTesting Expression substitute(int from, int to, Expression subExpression) {
 Preconditions.checkArgument(from <= to && from >= 0 && to <= segments.size());

 Expression result = new Expression();
 result.segments.addAll(segments.subList(0, from));
 result.segments.addAll(subExpression.segments);
 result.segments.addAll(segments.subList(to, segments.size()));
 return result;
}

int size() {
 return segments.size();
}

/**
* Returns a sub-expression of this expression, starting in fromIndex (inclusive) and ending
* in toIndex (exclusive)
*/
private Expression subExpression(int fromIndex, int toIndex) {
 Expression result = new Expression();
 result.segments.addAll(segments.subList(fromIndex, toIndex));
 return result;
}

@Override public String toString() {
 return segments.toString();
}

/**
* Makes substitutions in the expression according to available variable information
* For example, if it's known that x.y = z then [x, y, foo] will be transformed to [z, foo]
*/
Expression makeSubstitutions(
   int maxSubstitutions,
   Map<String, Expression> knownSubstitutions) {
 Expression expression = this;
 int substitutions = 0;
 boolean substituted = true;
 while (substitutions++ < maxSubstitutions && substituted) {
   substituted = false;
   for (int i = expression.size(); i >= 0; i--) {
     String variable = expression.subExpression(0, i).rebuildAsVariable();
     if (knownSubstitutions.containsKey(variable)) {
       expression = expression.substitute(0, i, knownSubstitutions.get(variable));
       substituted = true;
       break;
     }
   }
 }
 return expression;
}
}