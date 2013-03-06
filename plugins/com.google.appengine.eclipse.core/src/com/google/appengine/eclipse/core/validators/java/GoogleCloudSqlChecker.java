// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.appengine.eclipse.core.validators.java;

import com.google.appengine.eclipse.core.markers.AppEngineJavaProblem;
import com.google.appengine.eclipse.core.properties.GoogleCloudSqlProperties;
import com.google.appengine.eclipse.core.sql.SqlUtilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.StringLiteral;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks whether the URL in DriverManager.getConnection(URL) is according to
 * project preferences. It flags a warning only when the URL is specified as a string literal.
 */
public class GoogleCloudSqlChecker {

  /**
   * Visitor to check whether getConnection has right parameter is the string
   * literal is the parameter.
   */
  public static class UrlValidationVisitor extends ASTVisitor {
    IJavaProject javaProject;
    List<CategorizedProblem> problems;
    public UrlValidationVisitor(IJavaProject javaProject, List<CategorizedProblem> problems) {
      this.javaProject = javaProject;
      this.problems = problems;
    }

    @Override
    public boolean visit(MethodInvocation node) {
      
      if (node.resolveMethodBinding() == null || node.resolveMethodBinding().getName() == null
          || node.resolveMethodBinding().getDeclaringClass() == null) {
        return true;
      }
      
      if (!node.resolveMethodBinding().getName().equals("getConnection")
              && !node.resolveMethodBinding().getName().equals("getDriver")) {
        return true;
      }
      
      String qualifiedName = node.resolveMethodBinding().getDeclaringClass().getQualifiedName();
      if (qualifiedName != null && qualifiedName.equals("java.sql.DriverManager")) {
        
        if (!node.arguments().isEmpty() && (node.arguments().get(0) instanceof StringLiteral)) {
          StringLiteral arg1 = (StringLiteral) node.arguments().get(0);
          String url = arg1.getLiteralValue();
          String jdbcUrl = SqlUtilities.getProdJdbcUrl(javaProject.getProject());
          
          int questionMarkIndex = url.indexOf('?');
          int length;
          if (questionMarkIndex != -1) {
            length = questionMarkIndex + 1;
          } else {
            length = url.length() + 1;
          }
          
          if (!url.equals(jdbcUrl) && !url.startsWith(jdbcUrl + "?")) {
            CategorizedProblem problem = 
                AppEngineJavaProblem.createWrongJdbcUrlError(arg1, length, jdbcUrl); 
            problems.add(problem);
          }
        }
      }
      return true;
    }
  }
  
  public static List<CategorizedProblem> check(CompilationUnit compilationUnit, 
      IJavaProject javaProject) {
    
    List<CategorizedProblem> problems = new ArrayList<CategorizedProblem>();
    
    IProject project = javaProject.getProject();    
    if (GoogleCloudSqlProperties.getGoogleCloudSqlEnabled(project)) {
      UrlValidationVisitor visitor = new UrlValidationVisitor(javaProject, problems);
      compilationUnit.accept(visitor);
    }
    return problems;
  }
}
