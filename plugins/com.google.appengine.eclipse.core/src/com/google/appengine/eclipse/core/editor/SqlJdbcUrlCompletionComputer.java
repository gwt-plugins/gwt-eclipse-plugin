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
import com.google.appengine.eclipse.core.properties.GoogleCloudSqlProperties;
import com.google.appengine.eclipse.core.sql.SqlUtilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Computer for auto completing a JDBC URL string based on the Google Cloud SQL preferences.
 */
public class SqlJdbcUrlCompletionComputer implements IJavaCompletionProposalComputer {

  private static final List<ICompletionProposal> NO_PROPOSALS = Collections.emptyList();
  
  private static final int COMPLETION_TRIGGER_INT = 5;

  public void sessionStarted() {
    // Do nothing.
  }

  public List<ICompletionProposal> computeCompletionProposals(
      ContentAssistInvocationContext context, IProgressMonitor monitor) {

    if (!(context instanceof JavaContentAssistInvocationContext)) {
      return NO_PROPOSALS;
    }
    
    JavaContentAssistInvocationContext assistContext = (JavaContentAssistInvocationContext) context;
    IProject project = assistContext.getProject().getProject();
    if (!GoogleCloudSqlProperties.getGoogleCloudSqlEnabled(project)) {
      return NO_PROPOSALS;
    }
    
    String prodUrl = SqlUtilities.getProdJdbcUrl(project);
    if (prodUrl == null) {
      return NO_PROPOSALS;
    }
    
    int completionTrigger = COMPLETION_TRIGGER_INT;
    int replacementLength = -1;
    if (GetConnectionCallChecker.isInGetConnectionArgument(assistContext)) {
      completionTrigger = 0;
      // We are sure that completion is a java string. But we are dont know whether 
      // there is a question mark somewhere.
      prodUrl = "\"" + prodUrl + "\"";
      IRegion region = GetConnectionCallChecker.getRegionOfStringLiteral(assistContext, false);
      if (region != null) {
        String url = null;
        try {
          IDocument doc = context.getDocument();
          url = doc.get(region.getOffset(), region.getLength());
        } catch (BadLocationException e) {
          // Should never happen.
          AppEngineCorePluginLog.logError(e);
          return NO_PROPOSALS;
        } 
        
        int questionMarkIndex = url.indexOf('?');
        if (questionMarkIndex == -1) {
          // If the invocation point is the string we should replace the right half of the string.
          replacementLength = region.getLength() - (context.getInvocationOffset() - 
              region.getOffset());
        } else {
          // Replace until the question mark.
          replacementLength = questionMarkIndex - (context.getInvocationOffset() - 
              region.getOffset());
          if (replacementLength < 0) {
            return NO_PROPOSALS;
          }
          prodUrl = prodUrl.substring(0, prodUrl.length() - 1);
        }
      }
      
      region = GetConnectionCallChecker.getRegionOfStringLiteral(assistContext, true);
      if (region != null && context.getInvocationOffset() != region.getOffset()) {
        // Fair to expect at-least quotes as the starting point.
        completionTrigger = 1;
      }
    }
    
    StringCompletionProposer proposer = new StringCompletionProposer(context);
    
    JavaCompletionProposal prodProposal;
    if (replacementLength != -1) {
      prodProposal = proposer.getCompletionProposal(prodUrl, completionTrigger, replacementLength);
    } else {
      prodProposal = proposer.getCompletionProposal(prodUrl, completionTrigger);      
    }
    
    if (prodProposal == null) {
      return NO_PROPOSALS;
    }
    
    List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
    proposals.add(prodProposal);
    
    return proposals;
  }

  public List<IContextInformation> computeContextInformation(
      ContentAssistInvocationContext context, IProgressMonitor monitor) {
    return Collections.emptyList();
  }

  public String getErrorMessage() {
    // Do nothing.
    return null;
  }

  public void sessionEnded() {
    // Do nothing.
  }
}
