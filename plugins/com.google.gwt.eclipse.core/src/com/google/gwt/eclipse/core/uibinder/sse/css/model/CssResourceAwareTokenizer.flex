// CHECKSTYLE_OFF
/*
 * The original CSSTokenizer.flex is at
 * http://dev.eclipse.org/viewsvn/index.cgi/sourceediting/plugins/org.eclipse.wst.sse.core/DevTimeSupport/SedModel/CSSTokenizer/?root=WebTools_Project.
 * 
 * This contains modifications to allow scanning CSS Resource custom at-rules, which
 * the built-in Eclipse flex file fails at.
 * 
 * To generate the java source code, download JFlex and run it with the default options.
 * The generated java file has some comments before the "// CHECKSTYLE_OFF", make sure
 * you delete them.  Also, fix the local path in the class javadoc.
 */


/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*nlsXXX*/
package com.google.gwt.eclipse.core.uibinder.sse.css.model;

import java.io.CharArrayReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.wst.css.core.internal.parser.CSSRegionUtil;
import org.eclipse.wst.css.core.internal.parser.ICSSTokenizer;
import org.eclipse.wst.css.core.internal.parser.regions.CSSTextRegionFactory;
import org.eclipse.wst.css.core.internal.parserz.CSSRegionContexts;
import org.eclipse.wst.css.core.internal.parserz.CSSTextToken;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;

@SuppressWarnings({"restriction", "unused", "unchecked"})

%%

%public
%class CssResourceAwareTokenizer
%implements CSSRegionContexts, ICSSTokenizer
%function primGetNextToken
%type String
%char
%unicode
%caseless
//%debug
%pack

%{
  private final static String UNDEFINED = "undefined";
  private String fBufferedContext = null;
  private int fBufferedStart;
//  private int fBufferedTextLength;
  private int fBufferedLength;
//  private StringBuffer fBufferedText = null;
  private CSSTextRegionFactory fRegionFactory = CSSTextRegionFactory.getInstance();
  private int fInitialState = YYINITIAL;
  public final static int BUFFER_SIZE_NORMAL = 16384;
  public final static int BUFFER_SIZE_SMALL = 256;
  private int fInitialBufferSize = BUFFER_SIZE_NORMAL;

  public void setInitialState(int state) {
    fInitialState = state;
  }
  
  public void setInitialBufferSize(int size) {
    fInitialBufferSize = size;
  }

  /* user method */
    public final ITextRegion getNextToken() throws IOException {
    String context;
    String nextTokenType;
    boolean spaceFollows;
//    StringBuffer text;
    int start;
    int textLength;
    int length;
    if (fBufferedContext != null) {
      context = fBufferedContext;
//      text = fBufferedText;
      start = fBufferedStart;
      textLength = length = fBufferedLength;

      fBufferedContext = null;
    } else {
      context = primGetNextToken();
//      text = new StringBuffer(yytext());
      start = yychar;
      textLength = length = yylength();
    }

    if (context != null) {
      if (context == UNDEFINED) {
        // undef -> concatenate undef's
        nextTokenType = primGetNextToken();
        while (nextTokenType == UNDEFINED) {
//          text.append(yytext());
          textLength += yylength();
          length = textLength;
          nextTokenType = primGetNextToken();
        }
        fBufferedContext = nextTokenType;
//        fBufferedText = new StringBuffer(yytext());
        fBufferedStart = yychar;
        fBufferedLength = yylength();
      } else {
        nextTokenType = null;
        spaceFollows = false;
        if (CSSRegionUtil.isDeclarationValueType(context)) { // declaration value can contain VALUE_S
          nextTokenType = primGetNextToken();
          spaceFollows = (nextTokenType == CSS_DECLARATION_VALUE_S);
        } else if (canContainSpace(context)) {
          nextTokenType = primGetNextToken();
          spaceFollows = (nextTokenType == CSS_S);
        }
        if (nextTokenType != null) { // nextToken is retrieved
          if (spaceFollows) {
            // next is space -> append
//            text.append(yytext());
            length += yylength();
          } else {
            // next is NOT space -> push this for next time, return itself
            fBufferedContext = nextTokenType;
//            fBufferedText = new StringBuffer(yytext());
            fBufferedStart = yychar;
            fBufferedLength = yylength();
          }
        }
      }
    }

    if (context != null) {
      if (context == UNDEFINED) {
        context = CSS_UNKNOWN;
      }
      return fRegionFactory.createRegion(context, start, textLength, length);
    } else {
      return null;
    }
    }

  /* user method */
  /* for standalone use */
    public final List parseText() throws IOException {
      List tokens = new ArrayList();

      CSSTextToken token;
    for (String kind = primGetNextToken(); kind != null; kind = primGetNextToken()) {
      token = new CSSTextToken();
      token.kind = kind;          
      token.start = yychar;
      token.length = yylength();
      token.image = yytext();
      tokens.add(token);
    }

      return tokens;
    }
    
    /* user method */
    private boolean canContainSpace(String type) {
      if (type == CSS_DELIMITER || type == CSS_RBRACE || type == CSS_DECLARATION_DELIMITER) {
        return false;
      } else {
        return true;
      }
    }

  /* user method */
  public final int getOffset() {
    return yychar;
  }
  
  /* user method */
  public final boolean isEOF() {
    return zzAtEOF;
  }

  /* user method */
  public void reset(char[] charArray) {
    reset(new CharArrayReader(charArray), 0);
  }

  /* user method */
  public final void reset(java.io.Reader in, int newOffset) {
    /** the input device */
    zzReader = in;

    /** the current state of the DFA */
    zzState = 0;

    /** the current lexical state */
    zzLexicalState = fInitialState; //YYINITIAL;

    /** this buffer contains the current text to be matched and is
      the source of the yytext() string */
    if (zzBuffer.length != fInitialBufferSize) {
      zzBuffer = new char[fInitialBufferSize];
    }
    java.util.Arrays.fill(zzBuffer, (char)0);

    /** the textposition at the last accepting state */
    zzMarkedPos = 0;

    /** the textposition at the last state to be included in yytext */
//    yy_pushbackPos = 0;

    /** the current text position in the buffer */
    zzCurrentPos = 0;

    /** startRead marks the beginning of the yytext() string in the buffer */
    zzStartRead = 0;

    /** endRead marks the last character in the buffer, that has been read
      from input */
    zzEndRead = 0;

    /** number of newlines encountered up to the start of the matched text */
    yyline = 0;

    /** the number of characters up to the start of the matched text */
    yychar = 0;

    /**
     * the number of characters from the last newline up to the start of the 
     * matched text
     */
//    yycolumn = 0; 

    /** 
     * yy_atBOL == true <=> the scanner is currently at the beginning of a line
     */
//    yy_atBOL = false;
    
    /** zzAtEOF == true <=> the scanner has returned a value for EOF */
    zzAtEOF = false;

    /* user variables */
    //    fUndefined.delete(0, fUndefined.length());
  }

  /* user method */
  public CssResourceAwareTokenizer() {
    super();
  }

  /**
   * Added to workaround stricter compilation options without creating
   * an alternate skeleton file
   */
  void _usePrivates() {
    System.out.print(yycolumn);
    System.out.print(yyline);
    System.out.print(Boolean.toString(zzAtBOL));
  }
%}

%state ST_CHARSET_NAME
%state ST_CHARSET_DELIMITER
%state ST_IMPORT_URI
%state ST_IMPORT_MEDIUM
%state ST_IMPORT_DELIMITER
%state ST_MEDIA_MEDIUM
%state ST_MEDIA_DELIMITER
%state ST_PAGE_PSEUDO_PAGE
%state ST_PAGE_DELIMITER
%state ST_FONT_FACE_DELIMITER
%state ST_SELECTOR
%state ST_SELECTOR_MODIFIER
%state ST_SELECTOR_ATTRIBUTE_NAME
%state ST_SELECTOR_ATTRIBUTE_OPERATOR
%state ST_SELECTOR_ATTRIBUTE_VALUE
%state ST_SELECTOR_ATTRIBUTE_END
%state ST_DECLARATION
%state ST_DECLARATION_SEPARATOR
%state ST_DECLARATION_PRE_VALUE
%state ST_DECLARATION_VALUE
%state ST_CSSRESOURCE_RULE_MEDIUM

h = [0-9a-f]
nonascii = [\u0080-\uffff]
unicode = \\{h}{1,6}[ \t\r\n\f]?
escape = {unicode}|\\[ -~\u0080-\uffff]



nmstart = [_a-zA-Z]|{nonascii}|{escape}
nmchar = [_a-zA-Z0-9-]|{nonascii}|{escape}
string1 = \"([\t !#$%&(-~]|\\{nl}|\'|{nonascii}|{escape})*\"
string2 = \'([\t !#$%&(-~]|\\{nl}|\"|{nonascii}|{escape})*\'

ident = -?{nmstart}{nmchar}*
value_ident = -?{nmstart}"."?({nmchar}+"."?)*

name = {nmchar}+
num = [+-]?([0-9]+|[0-9]*"."[0-9]+)
string = {string1}|{string2}
url = ([ !#$%&*-~]|{nonascii}|{escape})*
s = [ \t\r\n\f]
w = {s}*
nl = \n|\r\n|\r|\f

//range = \?{1,6}|{h}(\?{0,5}|{h}(\?{0,4}|{h}(\?{0,3}|{h}(\?{0,2}|{h}(\??|{h})))))

hash = "#"{name}
uri = ("url("{w}{string}{w}")"|"url("{w}{url}{w}")")
function = {ident}"("
unicode_range = "U"\+[0-9a-fA-F?]{1,6}("-"[0-9a-fA-F?]{1,6})?

%%

/*
 * *** global ***
 */

{s}+ { return CSS_S; }
"<!--" { return CSS_CDO; }
"-->" { return CSS_CDC; }
"}" { yybegin(YYINITIAL); return CSS_RBRACE; }
\/\*[^*]*\*+([^/*][^*]*\*+)*\/ { return CSS_COMMENT; }

//<YYINITIAL> {
//  "@import" { yybegin(ST_IMPORT_URI); return CSS_IMPORT; }
//}

/*
 * These are the CssResource-specific at-rules.  Without these rules, the base
 * implementation fails miserably.  We cannot return custom tokens since the CSS
 * model parser does not understand them (and we cannot extend that).  Instead,
 * we masquerade custom CssResource at-rules as other tokens.
 */
@def[^;]*; { return CSS_ATKEYWORD; }
@eval[^;]*; { return CSS_ATKEYWORD; }
@url[^;]*; { return CSS_ATKEYWORD; }
@external[^;]*; { return CSS_ATKEYWORD; }

// If adding a new item, make sure to add it to CssResourceAwareModelRepairer too
"@if" { yybegin(ST_CSSRESOURCE_RULE_MEDIUM); return CSS_MEDIA; }
"@elif" { yybegin(ST_CSSRESOURCE_RULE_MEDIUM); return CSS_MEDIA; }
"@else" { yybegin(ST_CSSRESOURCE_RULE_MEDIUM); return CSS_MEDIA; }
"@noflip" { yybegin(ST_CSSRESOURCE_RULE_MEDIUM); return CSS_MEDIA; }
<ST_CSSRESOURCE_RULE_MEDIUM> {
  [^{ \t\r\n\f]* { return CSS_MEDIUM; }
  "{" { yybegin(YYINITIAL); return CSS_LBRACE; }
}

"@sprite" { yybegin(ST_SELECTOR_MODIFIER); return CSS_SELECTOR_ELEMENT_NAME; }

/*
 * *** charset rule ***
 * CHARSET_SYM S* STRING S* ';'
 */

"@charset" { yybegin(ST_CHARSET_NAME); return CSS_CHARSET; }

<ST_CHARSET_NAME> {
  {string} { yybegin(ST_CHARSET_DELIMITER); return CSS_STRING; }
}

<ST_CHARSET_DELIMITER> {
  ";" { yybegin(YYINITIAL); return CSS_DELIMITER; }
}

/*
 * *** import rule ***
 * IMPORT_SYM S* [STRING|URI] S* [ medium [ COMMA S* medium]* ]? ';' S*
 */

"@import" { yybegin(ST_IMPORT_URI); return CSS_IMPORT; }

<ST_IMPORT_URI> {
  {string} { yybegin(ST_IMPORT_MEDIUM); return CSS_STRING; }
  //  "url("{w}{string}{w}")" { yybegin(ST_IMPORT_MEDIUM); return CSS_URI; }
  //  "url("{w}{url}{w}")" { yybegin(ST_IMPORT_MEDIUM); return CSS_URI; }
  {uri} { yybegin(ST_IMPORT_MEDIUM); return CSS_URI; }
  ";" { yybegin(YYINITIAL); return CSS_DELIMITER; }
}

<ST_IMPORT_MEDIUM> {
  {ident} { yybegin(ST_IMPORT_DELIMITER); return CSS_MEDIUM; }
  ";" { yybegin(YYINITIAL); return CSS_DELIMITER; }
}

<ST_IMPORT_DELIMITER> {
  ";" { yybegin(YYINITIAL); return CSS_DELIMITER; }
  "," { yybegin(ST_IMPORT_MEDIUM); return CSS_MEDIA_SEPARATOR; }
}

/*
 * *** media rule ***
 * MEDIA_SYM S* medium [ COMMA S* medium ]* LBRACE S* ruleset* '}' S*
 */

"@media" { yybegin(ST_MEDIA_MEDIUM); return CSS_MEDIA; }

/* 
 * medium
 * IDENT S*
 */
<ST_MEDIA_MEDIUM> {
  {ident} { yybegin(ST_MEDIA_DELIMITER); return CSS_MEDIUM; }
  "{" { yybegin(YYINITIAL); return CSS_LBRACE; }
}

<ST_MEDIA_DELIMITER> {
  "{" { yybegin(YYINITIAL); return CSS_LBRACE; }
  "," { yybegin(ST_MEDIA_MEDIUM); return CSS_MEDIA_SEPARATOR; }
}

/*
 * *** page rule **
 * PAGE_SYM S* pseudo_page? S* LBRACE S* declaration [ ';' S* declaration ]* '}' S*
 */

"@page" { yybegin(ST_PAGE_PSEUDO_PAGE); return CSS_PAGE; }
 
/*
 * pseudo_page
 * ':' IDENT
 */

<ST_PAGE_PSEUDO_PAGE> {
  ":"?{ident} { yybegin(ST_PAGE_DELIMITER); return CSS_PAGE_SELECTOR; }
  "{" { yybegin(ST_DECLARATION); return CSS_LBRACE; }
}

<ST_PAGE_DELIMITER> {
  "{" { yybegin(ST_DECLARATION); return CSS_LBRACE; }
}

/*
 * font-face
 * FONT_FACE_SYM S* '{' S* declaration [ ';' S* declaration '* '}' S*
 */

"@font-face" { yybegin(ST_FONT_FACE_DELIMITER); return CSS_FONT_FACE; }

<ST_FONT_FACE_DELIMITER> {
  "{" { yybegin(ST_DECLARATION); return CSS_LBRACE; }
}

/*
 * selector
 * simple_selector [ combinator simple_selector ]*
 */

/*
 * simple_selector
 * element_name [ HASH | class | attrib | pseudo ]* | [ HASH | class | attrib | pseudo ]+
 */

<YYINITIAL, ST_SELECTOR_MODIFIER, ST_SELECTOR> {
  "*" { yybegin(ST_SELECTOR_MODIFIER); return CSS_SELECTOR_UNIVERSAL; }
  {hash} { yybegin(ST_SELECTOR_MODIFIER); return CSS_SELECTOR_ID; }
//  ":"{ident}("("{s}*{ident}{s}*")")? { yybegin(ST_SELECTOR_MODIFIER); return CSS_SELECTOR_PSEUDO; }
  ":"({ident}("("{s}*{ident}{s}*")")?)? { yybegin(ST_SELECTOR_MODIFIER); return CSS_SELECTOR_PSEUDO; }
  "."{name} { yybegin(ST_SELECTOR_MODIFIER); return CSS_SELECTOR_CLASS; }
  "[" { yybegin(ST_SELECTOR_ATTRIBUTE_NAME); return CSS_SELECTOR_ATTRIBUTE_START; }
}

<YYINITIAL, ST_SELECTOR> {
  {ident} { yybegin(ST_SELECTOR_MODIFIER); return CSS_SELECTOR_ELEMENT_NAME; }
}

<ST_SELECTOR_MODIFIER> {
  "," { yybegin(ST_SELECTOR); return CSS_SELECTOR_SEPARATOR; }
  // using LOOKAHEAD
  {s}+/[^+>\{,/] { yybegin(ST_SELECTOR); return CSS_SELECTOR_COMBINATOR; }
  "+"|">" { yybegin(ST_SELECTOR); return CSS_SELECTOR_COMBINATOR; }
  "{" { yybegin(ST_DECLARATION); return CSS_LBRACE; }
}

/*
 * attrib
 * '[' S* IDENT S* [ [ '=' | INCLUDES | DASHMATCH ] S* [ IDENT | STRING ] S* ]? ']'
 */

<ST_SELECTOR_ATTRIBUTE_NAME> {
  {ident} { yybegin(ST_SELECTOR_ATTRIBUTE_OPERATOR); return CSS_SELECTOR_ATTRIBUTE_NAME; }
}

<ST_SELECTOR_ATTRIBUTE_OPERATOR> {
  "="|"~="|"|=" { yybegin(ST_SELECTOR_ATTRIBUTE_VALUE); return CSS_SELECTOR_ATTRIBUTE_OPERATOR; }
  "]" { yybegin(ST_SELECTOR_MODIFIER); return CSS_SELECTOR_ATTRIBUTE_END; }
}

<ST_SELECTOR_ATTRIBUTE_VALUE> {
  {ident}|{string} { yybegin(ST_SELECTOR_ATTRIBUTE_END); return CSS_SELECTOR_ATTRIBUTE_VALUE; }
}

<ST_SELECTOR_ATTRIBUTE_END> {
  "]" { yybegin(ST_SELECTOR_MODIFIER); return CSS_SELECTOR_ATTRIBUTE_END; }
}

/*
 * declaration
 * property ':' S* expr prio? | // empty //
 */

<ST_DECLARATION> {
  {ident} { yybegin(ST_DECLARATION_SEPARATOR); return CSS_DECLARATION_PROPERTY; }
}

<ST_DECLARATION_SEPARATOR> {
  ":" { yybegin(ST_DECLARATION_PRE_VALUE); return CSS_DECLARATION_SEPARATOR; }
}

<ST_DECLARATION_PRE_VALUE, ST_DECLARATION_VALUE> {
  "!"{s}*"important" { yybegin(ST_DECLARATION_VALUE); return CSS_DECLARATION_VALUE_IMPORTANT; }
  
  
  ")" { yybegin(ST_DECLARATION_VALUE); return CSS_DECLARATION_VALUE_PARENTHESIS_CLOSE; }
  
  // ordered following two rules deliberately, see 
  //  https://bugs.eclipse.org/bugs/show_bug.cgi?id=129902
  {num}{ident} { yybegin(ST_DECLARATION_VALUE); return CSS_DECLARATION_VALUE_DIMENSION; }
  {value_ident} { yybegin(ST_DECLARATION_VALUE); return CSS_DECLARATION_VALUE_IDENT; }


  {num}"%" { yybegin(ST_DECLARATION_VALUE); return CSS_DECLARATION_VALUE_PERCENTAGE; }
    
  {num} { yybegin(ST_DECLARATION_VALUE); return CSS_DECLARATION_VALUE_NUMBER; }
  
  
  
  {function} { yybegin(ST_DECLARATION_VALUE); return CSS_DECLARATION_VALUE_FUNCTION; }
  {string} { yybegin(ST_DECLARATION_VALUE); return CSS_DECLARATION_VALUE_STRING; }
  {uri} { yybegin(ST_DECLARATION_VALUE); return CSS_DECLARATION_VALUE_URI; }
  "#"{name} { yybegin(ST_DECLARATION_VALUE); return CSS_DECLARATION_VALUE_HASH; }
  {unicode_range} { yybegin(ST_DECLARATION_VALUE); return CSS_DECLARATION_VALUE_UNICODE_RANGE; }
  [,/] { yybegin(ST_DECLARATION_VALUE); return CSS_DECLARATION_VALUE_OPERATOR; }
}

<ST_DECLARATION_VALUE> {
  {s}+/[^;}] { return CSS_DECLARATION_VALUE_S; }
}

<ST_DECLARATION, ST_DECLARATION_SEPARATOR, ST_DECLARATION_PRE_VALUE, ST_DECLARATION_VALUE> {
  ";" { yybegin(ST_DECLARATION); return CSS_DECLARATION_DELIMITER; }
  //  "}" { yybegin(YYINITIAL); return CSS_RBRACE; }
}


//<YYINITIAL, ST_IMPORT_URI, ST_IMPORT_MEDIUM, ST_IMPORT_DELIMITER> {
//  \/\*[^*]*\*+([^/*][^*]*\*+)*\/ { return CSS_COMMENT; }
//  {s}+ { return CSS_S; }
//  . { return UNDEFINED; }
//}

//<YYINITIAL, ST_IMPORT_URI, ST_IMPORT_MEDIUM, ST_IMPORT_DELIMITER> {
//  [^ \t\r\n\f]+ { return CSS_UNKNOWN; }
//}

. {
  return UNDEFINED;
}