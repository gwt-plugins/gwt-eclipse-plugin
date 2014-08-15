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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.PeekingIterator;
import com.google.gdt.eclipse.drive.editors.JavaScriptIdentifierNames;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;

import javax.annotation.Nullable;

/**
 * This class handles the autocomplete logic for a particular editor tab.
 *
 * @author nikhilsinghal@google.com (Nikhil Singhal)
 */
public final class Autocompleter {

  /** Maximum depth to be used by recursion to find the type of an expressions  */
  private static final int MAX_RECURSION_DEPTH = 100;

  /** The maximal number of substitutions on an expression, this avoids useless work large ones */
  private static final int MAX_SUBSTITUTIONS = 10;

  private static final String ARRAY_TYPE = "[]";

  /** Characters that cause us to stop parsing the line. */
  private static final CharMatcher DELIMITERS
      = CharMatcher.anyOf(";,") // statement barriers
          .or(CharMatcher.anyOf("+-*/%=<>"))  // arithmetic
          .or(CharMatcher.anyOf("^&|!?:")) // logical
          .or(CharMatcher.anyOf("[({"))  // block start (NOTE: we'll only use these delimiters if
                                         // we know all blocks are closed -- see Segment#isValid())
          .precomputed();

  // Singleton holding autocomplete data for enabled beans.
  private final AutocompleteEntryHolder autocompleteEntryHolder;

  // Maps [variable name => type].
  private final Map<String, Expression> variableTypes;

  // The current type under consideration.
  @Nullable private String currentClass;

  public Autocompleter(String editorId, AutocompleteEntryHolder autocompleteEntryHolder) {
    this.autocompleteEntryHolder = autocompleteEntryHolder;
    this.variableTypes = Maps.newHashMap();
    this.currentClass = null;
  }

  public Result getMatchingEntries(int cursorPos, PeekingIterator<Character> chars) {
    String possibleClass = getPossibleClass(chars);
    if (isClass(possibleClass)) {
      currentClass = possibleClass;
      return Result.withEntries(cursorPos, getEntries(possibleClass));
    } else {
      return Result.empty(cursorPos);
    }
  }

  /**
   * Returns the possible entries for an incomplete line, when the autocomplete isn't triggered by
   * a ., but by ctrl-space. For example, it could be called for a line like "FooClass.ba", and we
   * want to return all entries in FooClass that start with "ba". Similarily, calling this method
   * for a line like "SomeB" should return completions for beans whose names start with that prefix,
   * e.g., "SomeBean".
   * 
   * @param cursorPos
   * @param chars a reverse character iterator
   */
  public Result getEntriesForIncompleteString(int cursorPos, PeekingIterator<Character> chars) {
    // Traverse the chars until you find the period. If you find a non-letter or digit char before
    // the period, attempt top-level completion instead of member completion.
    String suffix = "";
    while (chars.hasNext() && JavaScriptIdentifierNames.isIdentifierNameCharacter(chars.peek())) {
      suffix = chars.next() + suffix;
    }
    while (chars.hasNext() && Character.isWhitespace(chars.peek())) {
      chars.next();
    }
    if (chars.hasNext() && chars.next() == '.') {
      String possibleClass = getPossibleClass(chars);
      if (isClass(possibleClass)) {
        currentClass = possibleClass;
        return getMatchingEntries(cursorPos - suffix.length(), suffix);
      }
      return Result.empty(cursorPos);
    } else {
      currentClass = null;
      return getMatchingEntries(cursorPos - suffix.length(), suffix);
    }
  }

  public Result getMatchingEntries(int replacementStartPos, String prefix) {
    return Result.withEntries(replacementStartPos, getEntriesWithPrefix(currentClass, prefix));
  }


  /**
   * Parse the given line to infer variable types.
   * This handles assignments of the type: var x = foo;
   * and also var x = var y ... = foo;
   * 
   * @param chars a reverse character iterator
   */
  // TODO(ns): Handle multiple statements on the same line like:
  // var x = foo; var y = bar;
  public void parseLine(PeekingIterator<Character> chars) {
    // Consume all trailing whitespace and end-of-statement chars.
    consumeAllMatching(chars, CharMatcher.WHITESPACE.or(CharMatcher.is(';')));

    // Parse the last expression of the line.  We presume it's the right hand side of an assignment.
    Expression rhs = getExpression(chars);

    // Make sure it really was the right hand sign of the assignment (the next non-space char should
    // be an equals sign).
    consumeAllMatching(chars, CharMatcher.WHITESPACE);
    if (!chars.hasNext() || '=' != chars.next()) {
      return;
    }

    // Get the remainder of the line, until the next line-feed (don't want to keep going forever).
    // TODO(gmoura): go until the beginning of the expression instead of beginning of line.
    String line = "";
    while (chars.hasNext() && !CharMatcher.anyOf("\n;{}()").matches(chars.peek())) {
      line = chars.next() + line;
    }
    // Assign each variable to the value of the RHS.
    for (String side : Splitter.on('=').split(line)) {
      String[] pieces = side.trim().split(" ");
      String variable = pieces[pieces.length - 1];
      if (!Strings.isNullOrEmpty(variable)) {
        variableTypes.put(variable, rhs);
      }
    }
  }
  
  public boolean isTypeName(String word) {
    return autocompleteEntryHolder.isTypeName(word);
  }

  /**
   * Consume all characters in the given iterator that match the given matcher.
   */
  private static void consumeAllMatching(PeekingIterator<Character> charIterator,
      CharMatcher charMatcher) {
    while (charIterator.hasNext() && charMatcher.matches(charIterator.peek())) {
      charIterator.next();
    }
  }

  /**
   * Try and return the type for the expression ending at the {@code chars}
   * reverse iterator. This function loops over the characters backwards,
   * building up a potential call stack of the form (foo.bar.blah). Then it
   * looks at each (class.method()) pair to iteratively figure out the final
   * return type.
   */
  @VisibleForTesting String getPossibleClass(PeekingIterator<Character> chars) {
    // Break the line into segments.
    return getPossibleClass(getExpression(chars), MAX_RECURSION_DEPTH);
  }

  /**
   * Gets the possible class from an already parsed expression.
   * Makes sure the recursion for finding the type will not cause an overflow.
   *
   * @param expression the segments for the expression.
   * @param maxRecursionDepth
   */
  private String getPossibleClass(Expression expression, int maxRecursionDepth) {
    if (maxRecursionDepth <= 0) {
      return "";
    }

    expression = expression.makeSubstitutions(MAX_SUBSTITUTIONS, variableTypes);
    Iterator<Segment> iterator = expression.iterator();

    // Now get all the possible class.entry1.entry2... values.
    // Special-case the first piece, because it can be [].
    Segment first = iterator.next();

    // We need to use first.second to get the return type for the first set
    String returnType = getFirstReturnType(first, maxRecursionDepth);

    // Figure out the iterative return type.
    while (iterator.hasNext() && !returnType.isEmpty()) {
      Segment segment = iterator.next();
      returnType = getReturnType(returnType, segment);
    }

    return returnType;
  }

  /**
   * Handling the first segment in an expression.
   * This gets the type of the segment which is first in an expression and accounts for cases in
   * which this segment is an array.
   */
  private String getFirstReturnType(Segment first, int maxRecursionDepth) {
    if (maxRecursionDepth <= 0) {
      return "";
    }
    if (first.isRawArray()) {
      return ARRAY_TYPE;
    }
    String str = first.getValue();
    if (isTopLevelClass(str)) {
      return str;
    } else if (variableTypes.containsKey(str)) {
      return parseReturnType(getPossibleClass(variableTypes.get(str), maxRecursionDepth - 1),
          first.isArrayElement());
    } else {
      return "";
    }
  }

  /**
   * Parses the characters backwards until it hits the beginning of the current
   * expression.  Returns a stack of expression segments, which are parts of the
   * expression separated by a dot.
   *
   * @param chars a reverse character iterator
   * @return a stack of segments.
   */
  private static Expression getExpression(PeekingIterator<Character> chars) {
    Expression expression = new Expression();
    Segment currentSegment = new Segment();
    expression.push(currentSegment);
    while (chars.hasNext()) {
      char ch = chars.peek();

      if (currentSegment.isDefinitelyInvalid()) {
        break;
      }

      // If our segment is valid (has evenly closed blocks and quotes), and we've hit a valid
      // delimiter (a delimiter character or a space separating two alphanumeric chars), finish
      // parsing the expression.
      if ((DELIMITERS.apply(ch) || currentSegment.delimitedBySpace(ch))
          && currentSegment.isValid()) {
        return expression;
      }

      // Check for segment breaks in the expression (periods separating valid segments).
      chars.next();
      if (ch == '.' && currentSegment.isValid()) {
        currentSegment = new Segment();
        expression.push(currentSegment);
      } else {
        currentSegment.prependText(ch);
      }
    }
    return expression;
  }

  private boolean isTopLevelClass(String clazz) {
    return autocompleteEntryHolder.isTopLevelTypeName(clazz);
  }

  private boolean isClass(String clazz) {
    return clazz.endsWith(ARRAY_TYPE) || autocompleteEntryHolder.isTypeName(clazz);
  }

  private SortedSet<AutocompleteEntry> getEntries(String clazz) {
    return ImmutableSortedSet.copyOf(
        autocompleteEntryHolder.getEntriesForTypeName(classOrArray(clazz)));
  }

  @VisibleForTesting
  SortedSet<AutocompleteEntry> getEntriesWithPrefix(@Nullable String clazz, final String prefix) {
    SortedSet<AutocompleteEntry> entries
        = autocompleteEntryHolder.getEntriesForTypeName(classOrArray(clazz));

    return ImmutableSortedSet.copyOf(Iterables.filter(entries, new Predicate<AutocompleteEntry>() {
      @Override public boolean apply(AutocompleteEntry entry) {
        return entry.getEntryName().toLowerCase().startsWith(prefix.toLowerCase());
      }
    }));
  }

  @Nullable private static String classOrArray(@Nullable String clazz) {
    return (clazz != null && clazz.endsWith(ARRAY_TYPE)) ? ARRAY_TYPE : clazz;
  }

  @VisibleForTesting String getReturnType(String className, Segment entry) {
    for (AutocompleteEntry entryObj : autocompleteEntryHolder.getEntriesForTypeName(className)) {
      if (entryObj.getEntryName().equals(entry.getValue())) {
        return parseReturnType(entryObj.getReturnType(), entry.isArrayElement());
      }
    }

    return "";
  }

  private static String parseReturnType(String returnType, boolean isArrayElement) {
    if (returnType.endsWith(ARRAY_TYPE) && isArrayElement) {
      // We found the array index operators, therefore treat this as an
      // element of the array foo[], which is of type foo.
      returnType = returnType.substring(0, returnType.length() - 2);
    }

    return returnType;
  }

  /**
   * Represents an {@link Autocompleter} result consisting of a column index marking the beginning
   * of the autocomplete replacement region in the current editor line and a sorted set of
   * completions possible at the current cursor position.
   */
  public static final class Result {

    private final int replacementStartPos;

    private final SortedSet<AutocompleteEntry> entries;

    /**
     * Returns an {@link Autocompleter.Result} with the specified replacement region start index
     * that contains no autocomplete entries.
     */
    public static Result empty(int replacementStartPos) {
      return new Result(replacementStartPos, ImmutableSortedSet.<AutocompleteEntry>of());
    }

    /**
     * Returns an {@link Autocompleter.Result} with the specified replacement region start index
     * that contains the given autocomplete entries.
     */
    public static Result withEntries(
        int replacementStartPos,
        SortedSet<AutocompleteEntry> entries) {
      return new Result(replacementStartPos, entries);
    }

    /**
     * Instantiates an {@link Autocompleter.Result} with the specified replacement region start
     * index that contains the given autocomplete entries.
     */
    private Result(int replacementStartPos, SortedSet<AutocompleteEntry> entries) {
      Preconditions.checkArgument(replacementStartPos >= 0,
          "replacementStartPos must be nonnegative: %s", replacementStartPos);

      this.replacementStartPos = replacementStartPos;
      this.entries = Preconditions.checkNotNull(entries, "entries must be nonnull");
    }

    /**
     * Returns the replacement region start index associated with this autocomplete result.
     */
    public int getReplacementStartPos() {
      return replacementStartPos;
    }

    /**
     * Returns the possible completions associated with this autocomplete result.
     */
    public SortedSet<AutocompleteEntry> getEntries() {
      return entries;
    }

    /**
     * Checks whether or not this result contains any possible completions.
     */
    public boolean hasEntries() {
      return !entries.isEmpty();
    }

    @Override public boolean equals(Object obj) {
      if (!(obj instanceof Result)) {
        return false;
      }
      Result other = (Result) obj;
      return replacementStartPos == other.replacementStartPos && entries.equals(other.entries);
    }

    @Override public int hashCode() {
      return Objects.hash(replacementStartPos, entries);
    }
  }
}