// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.appengine.eclipse.core.editor;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.CompletionRequestor;
import org.eclipse.jdt.core.compiler.IProblem;

/**
 * CompletionRequestor to check whether we are at getConnection(^ or getDriver(^.
 * Makes sure getConnection is from java.sql.DriverManager class.
 */
public class GetConnectionCompletionRequestor extends CompletionRequestor {

  private boolean proposable = false;
  private boolean hasCompletionFailed = false;

  /**
   * Check whether the requester has found that we are at "getConnection(^" or "getDriver(^".
   */
  public boolean isProposable() {
    return proposable;
  }

  private void setProposable(boolean proposable) {
    this.proposable = proposable;
  }
  
  /**
   * Return whether the completion proposal generation has failed.
   */
  public boolean hasFailed() {
    return hasCompletionFailed;
  }
  
  GetConnectionCompletionRequestor() {
    // Ignore all proposals except method references
    super(true);
    super.setIgnored(CompletionProposal.METHOD_REF, false);
  }

  @Override
  public void accept(CompletionProposal proposal) {

    if (proposal == null) {
      return;
    }
    
    // Check whether the declaring class is java.sql.DriverManager.
    if (!isParentDriverManager(proposal)) {
      return;
    }
    
    if (proposal.getName() == null) {
      return;
    }
    
    // Check whether the function name is right. 
    if (!new String(proposal.getName()).equals("getConnection")
        && !new String(proposal.getName()).equals("getDriver")) {
      return;
    }

    // Check whether the completion is empty. The only possibility right now, we assume, is the 
    // state "DriverManager.getConnection(^" or "DriverManager.getDriver(^". Similar logic is used 
    // in org.eclipse.jdt.internal.ui.text.java.FillArgumentNamesCompletionProposalCollector.java
    if (proposal.getCompletion() == null || 
        !(new String(proposal.getCompletion())).trim().isEmpty()) {
      return;
    }
    
    setProposable(true);
  }
  
  @Override
  public void completionFailure(IProblem problem) {
    hasCompletionFailed = true;
  }

  /**
   * Check whether the declaring class is of the type java.sql.DriverManager.
   */
  static boolean isParentDriverManager(CompletionProposal proposal) {
    if (proposal == null) {
      return false;
    }
    
    if (proposal.getDeclarationSignature() == null) {
      return false;
    }
    
    // Check whether the declaring class is of the type java.sql.DriverManager
    return new String("Ljava.sql.DriverManager;").equals(new String(proposal.
        getDeclarationSignature()));
  }
}
