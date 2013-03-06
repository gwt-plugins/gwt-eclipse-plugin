// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.appengine.eclipse.core.markers.quickfixes;

import com.google.appengine.eclipse.core.AppEngineCorePluginLog;
import com.google.appengine.eclipse.core.sql.SqlUtilities;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

/**
 * Correction proposal for quick fixing wrong Google Cloud SQL JDBC url.
 */
public class UrlCorrectionProposal  implements IJavaCompletionProposal {

  IProblemLocation problem;
  IJavaProject javaProject;
  int relevance;

  public UrlCorrectionProposal(IJavaProject javaProject, IProblemLocation problem, int relevance) {
    this.javaProject = javaProject;
    this.problem = problem;
    this.relevance = relevance;
  }

  public void apply(IDocument document) {
    // Since its a string literal first char is " 
    int stringStart = problem.getOffset() + 1;
    String correctUrl = SqlUtilities.getProdJdbcUrl(javaProject.getProject()); 
    try {
      document.replace(stringStart, problem.getLength() - 1, correctUrl);
    } catch (BadLocationException e) {
      AppEngineCorePluginLog.logError(e);
    }
  }

  public String getAdditionalProposalInfo() {
    return null;
  }

  public IContextInformation getContextInformation() {
    return null;
  }

  public String getDisplayString() {
    return "Fix JDBC URL to match the configured JDBC URL for Google Cloud SQL instance";
  }

  public Image getImage() {
    return null;
  }

  public int getRelevance() {
    return relevance;
  }

  public Point getSelection(IDocument document) {
    return null;
  }

}
