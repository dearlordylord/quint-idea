# Implementing Lexer

Source: https://plugins.jetbrains.com/docs/intellij/implementing-lexer.html

## Overview

A lexer (lexical analyzer) breaks file contents into tokens. It is the foundation of all language support features. Core API: `Lexer` interface.

## Three Lexer Contexts

1. **Syntax Highlighting** — returned from `SyntaxHighlighterFactory`, registered at `com.intellij.lang.syntaxHighlighterFactory`
2. **Syntax Tree Building** — provided via `ParserDefinition.createLexer()`, registered at `com.intellij.lang.parserDefinition`
3. **Word Indexing** — passed to `DefaultWordsScanner` constructor for Find Usages

## JFlex Implementation (Recommended)

Use JFlex with `FlexAdapter` to create lexers:

### JFlex Grammar File (`MyLanguage.flex`)

```
package com.example;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.example.psi.MyTypes;
import com.intellij.psi.TokenType;

%%

%class MyLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType

WHITE_SPACE=[ \t\n\r]+
IDENTIFIER=[a-zA-Z_][a-zA-Z0-9_]*
NUMBER=[0-9]+
STRING=\"[^\"]*\"
LINE_COMMENT="//"[^\n]*
BLOCK_COMMENT="/\*" [^*]* "\*/"

%%

{WHITE_SPACE}       { return TokenType.WHITE_SPACE; }
{LINE_COMMENT}      { return MyTypes.LINE_COMMENT; }
{BLOCK_COMMENT}     { return MyTypes.BLOCK_COMMENT; }
{STRING}            { return MyTypes.STRING; }
{NUMBER}            { return MyTypes.NUMBER; }
"module"            { return MyTypes.MODULE_KEYWORD; }
"def"               { return MyTypes.DEF_KEYWORD; }
"val"               { return MyTypes.VAL_KEYWORD; }
"{"                 { return MyTypes.LBRACE; }
"}"                 { return MyTypes.RBRACE; }
"("                 { return MyTypes.LPAREN; }
")"                 { return MyTypes.RPAREN; }
{IDENTIFIER}        { return MyTypes.IDENTIFIER; }
[^]                 { return TokenType.BAD_CHARACTER; }
```

### Requirements

- Uses patched JFlex version with `--charat` support
- Uses `idea-flex.skeleton` skeleton file from IntelliJ Platform source
- Grammar-Kit plugin provides syntax highlighting for `.flex` files
- Gradle Grammar-Kit Plugin generates lexer classes from `.flex` files

### Adapter

```kotlin
class MyLexerAdapter : FlexAdapter(MyLexer(null))
```

## Lexer State Management

- `Lexer.getState()` must return a single integer representing lexer state
- State tracks context (inside comment, string literal, etc.)
- Passed back to `Lexer.start()` when resuming from mid-file
- Non-incremental lexers can return `0`
- Syntax highlighting lexers process incrementally; others process complete files

## Token Types

Define using `IElementType`:

```kotlin
class MyTokenType(debugName: String) : IElementType(debugName, MyLanguage.INSTANCE)
```

### Token Sets

Group related tokens:

```kotlin
object MyTokenSets {
    val COMMENTS = TokenSet.create(MyTypes.LINE_COMMENT, MyTypes.BLOCK_COMMENT)
    val STRINGS = TokenSet.create(MyTypes.STRING)
    val KEYWORDS = TokenSet.create(
        MyTypes.MODULE_KEYWORD,
        MyTypes.DEF_KEYWORD,
        MyTypes.VAL_KEYWORD
    )
}
```

All `TokenSet` return values should use constants from a dedicated `$Language$TokenSets` class to avoid unnecessary classloading.

## Critical Rules

1. **No gaps**: Lexers must match the entire file contents without gaps between tokens
2. **Bad characters**: Generate `TokenType.BAD_CHARACTER` for invalid characters at their location
3. **Reuse types**: Return the same `IElementType` instance each time a token type is encountered
4. **Common tokens**: Reuse `TokenType.WHITE_SPACE` and `TokenType.BAD_CHARACTER`

## Embedded Language Support

For mixed languages (e.g., template engines):

- Define chameleon token types implementing `ILazyParseableElementType`
- Lexer returns entire embedded fragments as single chameleon tokens
- IDE calls `ILazyParseableElementType.parseContents()` to parse embedded content

## ParserDefinition Integration

```kotlin
class MyParserDefinition : ParserDefinition {
    override fun createLexer(project: Project): Lexer = MyLexerAdapter()

    override fun getCommentTokens(): TokenSet = MyTokenSets.COMMENTS
    override fun getWhitespaceTokens(): TokenSet = TokenSet.WHITE_SPACE
    override fun getStringLiteralElements(): TokenSet = MyTokenSets.STRINGS

    // ... other methods
}
```
