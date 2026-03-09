package com.dearlordylord.quint.idea.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.dearlordylord.quint.idea.QuintTokenTypes;

%%

%class QuintLexer
%public
%implements FlexLexer
%unicode
%function advance
%type IElementType

%{
  private CharSequence zzBufferSequence;

  public void reset(CharSequence buffer, int start, int end, int initialState) {
    zzBufferSequence = buffer;
    char[] chars = new char[buffer.length()];
    for (int i = 0; i < buffer.length(); i++) {
      chars[i] = buffer.charAt(i);
    }
    zzBuffer = chars;
    zzCurrentPos = zzMarkedPos = zzStartRead = start;
    zzAtEOF = false;
    zzAtBOL = true;
    zzEndRead = end;
    yybegin(initialState);
  }

  public int getTokenStart() {
    return zzStartRead;
  }

  public int getTokenEnd() {
    return zzMarkedPos;
  }
%}

%%

[^]                     { return QuintTokenTypes.BAD_CHARACTER; }
