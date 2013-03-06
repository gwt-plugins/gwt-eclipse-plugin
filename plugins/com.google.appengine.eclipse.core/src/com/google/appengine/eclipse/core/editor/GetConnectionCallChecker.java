// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.appengine.eclipse.core.editor;

import com.google.appengine.eclipse.core.AppEngineCorePluginLog;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * Class to check whether the completion is invoked at the state DriverManager.getConnection( or
 *  or getDriver( or sql.DriverManager.getConnection(STRING_LITERAL) or sql.DriverManager.getDriver(
 *  STRING_LITERAL);
 */
public class GetConnectionCallChecker {
  
  /**
   * Returns the region formed by a string literal if the string literal is at the 
   * invocation position else returns null.
   * @param context
   * @param enableMalformed If set to true it will try to recover the malformed string literal as 
   * well.
   * @return IRegion which contains the start offset and the length of the region.  
   */
  public static IRegion getRegionOfStringLiteral(JavaContentAssistInvocationContext context, 
      boolean enableMalformed) {
    IScanner scanner = ToolFactory.createScanner(false, false, false, false);
    IDocument doc = context.getDocument();
    
    int invocationOffset = context.getInvocationOffset();
    int lineOffset = 0;
    try {
      int lineNo = doc.getLineOfOffset(invocationOffset);
      IRegion region = doc.getLineInformation(lineNo);
      String lineSource = doc.get(region.getOffset(), region.getLength());
      scanner.setSource(lineSource.toCharArray());
      lineOffset = doc.getLineOffset(lineNo);
    } catch (BadLocationException e) {
      // Should not happen.
      AppEngineCorePluginLog.logError(e, "Bad location exception while getting the cursor region");
      return null;
    }
    
    int stringStartPos = -1;

    int token = -1; 
    while (token != ITerminalSymbols.TokenNameEOF) {
      
      try {
        token = scanner.getNextToken();
        if (token == ITerminalSymbols.TokenNameStringLiteral) {
          if (lineOffset + scanner.getCurrentTokenStartPosition() <= invocationOffset 
              && lineOffset + scanner.getCurrentTokenEndPosition() >= invocationOffset) {
            return new Region(lineOffset + scanner.getCurrentTokenStartPosition(), 
                scanner.getCurrentTokenSource().length);
          }
        }
      } catch (InvalidInputException e) {
        if (enableMalformed) {
          // User might not have completely typed the string. In such cases the user has not 
          // terminated the string and the recovered token begins with a double qoute.
          String faultyString = new String(scanner.getCurrentTokenSource()).trim();
          if (faultyString.length() > 0 && faultyString.charAt(0) == '"') {
            if (lineOffset + scanner.getCurrentTokenStartPosition() <= invocationOffset 
                && lineOffset + scanner.getCurrentTokenEndPosition() >= invocationOffset - 1) {
              return new Region(lineOffset + scanner.getCurrentTokenStartPosition(), 
                 faultyString.length());
            }
          }          
        }
        // Else encountered some mal-formed token. We should not be bothered here.
      }
    }
    return null;
  }
  
  /**
   * If the invocation offset in the context is inside a string literal it returns offset before
   * the string literal else it returns the the invocationPoint.
   * @param context
   * @return the corrected invocation point
   */
  private static int mayBeNewInvocationOffset(JavaContentAssistInvocationContext context) {
    int invocationOffset = context.getInvocationOffset();  
    // If there is no string literal at cursor there could be mal-formed string literal.
    IRegion region = getRegionOfStringLiteral(context, true);
    if (region != null) {
        return region.getOffset();
    }
    return invocationOffset;
  }

  public static boolean isInGetConnectionArgument(JavaContentAssistInvocationContext  context) {
    ICompilationUnit compilationUnit = context.getCompilationUnit();

    GetConnectionCompletionRequestor completionRequestor = new GetConnectionCompletionRequestor();
    try {
      compilationUnit.codeComplete(mayBeNewInvocationOffset(context), completionRequestor);
    } catch (JavaModelException e) {
      AppEngineCorePluginLog.logError(e, 
          "Error while calling code complete for Google Cloud JDBC SQL url");
    }

    return completionRequestor.isProposable();
  }

}
