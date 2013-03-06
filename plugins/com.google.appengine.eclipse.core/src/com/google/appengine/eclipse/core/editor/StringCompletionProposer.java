/*
 * Copyright 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.appengine.eclipse.core.editor;

import com.google.appengine.eclipse.core.AppEngineCorePluginLog;

import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

/**
 * Completes a string in the editor based on the current cursor position.
 */
public class StringCompletionProposer {

  ContentAssistInvocationContext context;

  public StringCompletionProposer(ContentAssistInvocationContext context) {
    this.context = context;
  }
  
  /**
   * Gets the string before the invocation point and calculates the suffix to be added to the
   * already typed string.
   * @param proposal The string to auto-complete. 
   * @param completionTriggerInt Return null proposal if completionTriggerInt number of characters 
   * of the string proposal have not been completed.
   * @param replacementLength Set replacement length to -1 if you want 
   * the function to compute replacement length. 
   */
  public JavaCompletionProposal getCompletionProposal(String proposal, 
      int completionTriggerInt, int replacementLength) {
    int invocationOffset = context.getInvocationOffset();
    String prefixToComplete = getPrefixToComplete(proposal, completionTriggerInt);
    
    if (prefixToComplete == null) {
      return null;
    }
    
    return new JavaCompletionProposal(prefixToComplete, invocationOffset,
        replacementLength, null, proposal, 0);
  }

  
  /**
   * Gets the string before the invocation point and calculates the suffix to be added to the
   * already typed string.
   * @param proposal The string to auto-complete. 
   * @param completionTriggerInt Return null proposal if completionTriggerInt number of characters 
   * of the string proposal have not been completed.
   */
  public JavaCompletionProposal getCompletionProposal(String proposal, 
      int completionTriggerInt) {
    int invocationOffset = context.getInvocationOffset();
    String prefixToComplete = getPrefixToComplete(proposal, completionTriggerInt);
    
    if (prefixToComplete == null) {
      return null;
    }
    
    int replacementLength = getReplacementLength(prefixToComplete);
    return new JavaCompletionProposal(prefixToComplete, invocationOffset,
        replacementLength, null, proposal, 0);
  }
  
  /**
   * Gets the string need to be completed by looking at already typed string 
   */
  public String getPrefixToComplete(String proposal, int completionTriggerInt) {
    JavaContentAssistInvocationContext assistContext = (JavaContentAssistInvocationContext) context;
    
    // Find the length of the string before invocation point relevant to us.
    int strBeforeOffsetLength = proposal.length();
    
    // If the invocation point is too near the start of the document we should 
    // reduce the length of the string needed from the doc.
    int invocationOffset = context.getInvocationOffset();
    if (invocationOffset < strBeforeOffsetLength) {
      strBeforeOffsetLength = invocationOffset;
    }
    
    IDocument doc = context.getDocument();
    String strBeforeOffset = null;
    try {
      // Get the required string before invocation point from the document.
      strBeforeOffset = doc.get(invocationOffset - strBeforeOffsetLength, strBeforeOffsetLength);
    } catch (BadLocationException e) {
      AppEngineCorePluginLog.logError(e, "Bad location exception while reading string from doc");
      return null;
    }
    
    int prefixStartPos = getLengthOfPartialUrlTyped(strBeforeOffset, proposal, 
        completionTriggerInt);

    if (prefixStartPos == -1) {
      return null;
    }
    
    if (proposal.length() == prefixStartPos) {
      return null;
    }
    
    return proposal.substring(prefixStartPos, proposal.length());
  }
  
  /**
   * @return Returns the length of the completion string already typed. This string is the suffix 
   * of string before invocation point.
   */
  private static int getLengthOfPartialUrlTyped(String strBeforeOffset, String proposal, 
      int completionTriggerInt) {
    int index = -1; 
    String completionTriggerString = proposal.substring(0, completionTriggerInt);
    // Check every occurrence of completionTriggerString to be doubly sure you have not 
    // missed anything.
    do {
      index = strBeforeOffset.indexOf(completionTriggerString, index + 1);
      if (index != -1) {
        if (strBeforeOffset.endsWith(proposal.substring(0, strBeforeOffset.length() - index))) {
          return strBeforeOffset.length() - index;
        }
      }
    } while (index != -1);
    return -1;
  }
  
  /**
   * @return Return computed length of the text to replace. That is the number of letters after the 
   * invocation point already typed. This works as desired on when Auto-Completion mode 
   * is Completion Overwrites.
   */
  private int getReplacementLength(String prefixToComplete) {
    int invocationOffset = context.getInvocationOffset();
    IDocument doc = context.getDocument();
    int strAfterInvocationLength = prefixToComplete.length();
    if (prefixToComplete.length() + invocationOffset > doc.getLength()) {
      strAfterInvocationLength = doc.getLength() - invocationOffset;
    }
    
    String strAfterInvocation = null;
    
    try {
      strAfterInvocation = doc.get(invocationOffset, strAfterInvocationLength);
    } catch (BadLocationException e) {
      AppEngineCorePluginLog.logError(e, "Bad location exception while reading string from doc");
      strAfterInvocation = "";
    }

    int replacementLength = 0;
    for (int i = 0; i < strAfterInvocationLength; i++) {
      if (prefixToComplete.charAt(i) != strAfterInvocation.charAt(i)) {
        break;
      }
      replacementLength++;
    }
    
    return replacementLength;
  }
}

