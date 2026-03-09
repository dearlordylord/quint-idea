package com.dearlordylord.quint.idea.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;
import com.dearlordylord.quint.idea.QuintTokenTypes;

%%

%class QuintLexer
%public
%implements FlexLexer
%unicode
%function advance
%type IElementType
%char

%state BLOCK_COMMENT

%{
  private CharSequence zzBufferSequence;
  private int blockCommentDepth = 0;
  private int zzOffsetInBuffer = 0;

  /** An empty reader that always returns EOF, used when operating from a char buffer. */
  private static final java.io.Reader EMPTY_READER = new java.io.Reader() {
    @Override public int read(char[] buf, int off, int len) { return -1; }
    @Override public void close() {}
  };

  public void reset(CharSequence buffer, int start, int end, int initialState) {
    zzBufferSequence = buffer;
    zzOffsetInBuffer = start;
    int len = end - start;
    char[] chars = new char[len];
    for (int i = 0; i < len; i++) {
      chars[i] = buffer.charAt(start + i);
    }
    zzBuffer = chars;
    zzCurrentPos = zzMarkedPos = zzStartRead = 0;
    zzAtEOF = false;
    zzAtBOL = true;
    zzEndRead = len;
    yychar = 0;
    zzReader = EMPTY_READER;
    yybegin(initialState);
  }

  public int getTokenStart() {
    return (int) yychar + zzOffsetInBuffer;
  }

  public int getTokenEnd() {
    return (int) yychar + yylength() + zzOffsetInBuffer;
  }
%}

WHITE_SPACE     = [ \t\r\n]+
HASHBANG_LINE   = "#!" [^\n]* \n?

// Comments
DOC_COMMENT     = "///" [^\n]*
LINE_COMMENT    = "//" [^\n]*

// String literal
STRING          = \" [^\"]* \"

// Integer literals
DEC_INT         = "0" | [1-9]([0-9] | "_"[0-9])*
HEX_INT         = "0x" [0-9a-fA-F]([0-9a-fA-F] | "_"[0-9a-fA-F])*
INT             = {DEC_INT} | {HEX_INT}

// Identifiers
LOW_ID          = [a-z][a-zA-Z0-9_]* | [_][a-zA-Z0-9_]+
CAP_ID          = [A-Z][a-zA-Z0-9_]*

%%

<YYINITIAL> {

  // Whitespace
  {WHITE_SPACE}             { return TokenType.WHITE_SPACE; }

  // Hashbang
  {HASHBANG_LINE}           { return QuintTokenTypes.HASHBANG; }

  // Comments — doc comments must come before line comments
  {DOC_COMMENT}             { return QuintTokenTypes.DOCCOMMENT; }
  {LINE_COMMENT}            { return QuintTokenTypes.LINE_COMMENT; }
  "/*"                      { blockCommentDepth = 1; yybegin(BLOCK_COMMENT); return QuintTokenTypes.COMMENT; }

  // String literal
  {STRING}                  { return QuintTokenTypes.STRING; }

  // Integer literal
  {INT}                     { return QuintTokenTypes.INT; }

  // Boolean literals (before identifiers)
  "true"                    { return QuintTokenTypes.BOOL; }
  "false"                   { return QuintTokenTypes.BOOL; }

  // Keywords (before identifiers)
  "module"                  { return QuintTokenTypes.MODULE; }
  "import"                  { return QuintTokenTypes.IMPORT; }
  "export"                  { return QuintTokenTypes.EXPORT; }
  "from"                    { return QuintTokenTypes.FROM; }
  "as"                      { return QuintTokenTypes.AS; }
  "const"                   { return QuintTokenTypes.CONST; }
  "var"                     { return QuintTokenTypes.VAR; }
  "assume"                  { return QuintTokenTypes.ASSUME; }
  "type"                    { return QuintTokenTypes.TYPE; }
  "val"                     { return QuintTokenTypes.VAL; }
  "def"                     { return QuintTokenTypes.DEF; }
  "pure"                    { return QuintTokenTypes.PURE; }
  "action"                  { return QuintTokenTypes.ACTION; }
  "run"                     { return QuintTokenTypes.RUN; }
  "temporal"                { return QuintTokenTypes.TEMPORAL; }
  "nondet"                  { return QuintTokenTypes.NONDET; }
  "if"                      { return QuintTokenTypes.IF; }
  "else"                    { return QuintTokenTypes.ELSE; }
  "match"                   { return QuintTokenTypes.MATCH; }

  // Keyword operators
  "and"                     { return QuintTokenTypes.AND; }
  "or"                      { return QuintTokenTypes.OR; }
  "iff"                     { return QuintTokenTypes.IFF; }
  "implies"                 { return QuintTokenTypes.IMPLIES; }
  "all"                     { return QuintTokenTypes.ALL; }
  "any"                     { return QuintTokenTypes.ANY; }

  // Type keywords
  "Set"                     { return QuintTokenTypes.SET; }
  "List"                    { return QuintTokenTypes.LIST; }

  // Multi-character operators (longer first)
  "..."                     { return QuintTokenTypes.SPREAD; }
  "::"                      { return QuintTokenTypes.COLONCOLON; }
  ">="                      { return QuintTokenTypes.GE; }
  "<="                      { return QuintTokenTypes.LE; }
  "!="                      { return QuintTokenTypes.NE; }
  "=="                      { return QuintTokenTypes.EQ; }
  "->"                      { return QuintTokenTypes.ARROW; }
  "=>"                      { return QuintTokenTypes.FAT_ARROW; }

  // Single-character operators
  "+"                       { return QuintTokenTypes.PLUS; }
  "-"                       { return QuintTokenTypes.MINUS; }
  "*"                       { return QuintTokenTypes.MUL; }
  "/"                       { return QuintTokenTypes.DIV; }
  "%"                       { return QuintTokenTypes.MOD; }
  "^"                       { return QuintTokenTypes.POW; }
  ">"                       { return QuintTokenTypes.GT; }
  "<"                       { return QuintTokenTypes.LT; }
  "="                       { return QuintTokenTypes.ASGN; }

  // Brackets
  "("                       { return QuintTokenTypes.LPAREN; }
  ")"                       { return QuintTokenTypes.RPAREN; }
  "{"                       { return QuintTokenTypes.LBRACE; }
  "}"                       { return QuintTokenTypes.RBRACE; }
  "["                       { return QuintTokenTypes.LBRACKET; }
  "]"                       { return QuintTokenTypes.RBRACKET; }

  // Punctuation
  ","                       { return QuintTokenTypes.COMMA; }
  ":"                       { return QuintTokenTypes.COLON; }
  "."                       { return QuintTokenTypes.DOT; }
  ";"                       { return QuintTokenTypes.SEMICOLON; }
  "|"                       { return QuintTokenTypes.PIPE; }
  "'"                       { return QuintTokenTypes.PRIME; }
  "_"                       { return QuintTokenTypes.UNDERSCORE; }

  // Identifiers (after keywords)
  {LOW_ID}                  { return QuintTokenTypes.LOW_ID; }
  {CAP_ID}                  { return QuintTokenTypes.CAP_ID; }

  // Bad character
  [^]                       { return QuintTokenTypes.BAD_CHARACTER; }
}

<BLOCK_COMMENT> {
  "/*"                      { blockCommentDepth++; return QuintTokenTypes.COMMENT; }
  "*/"                      { blockCommentDepth--; if (blockCommentDepth == 0) { yybegin(YYINITIAL); } return QuintTokenTypes.COMMENT; }
  [^]                       { return QuintTokenTypes.COMMENT; }
}
