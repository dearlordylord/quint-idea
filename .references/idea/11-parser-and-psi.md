# Implementing Parser and PSI

Source: https://plugins.jetbrains.com/docs/intellij/implementing-parser-and-psi.html

## Overview

Parsing in IntelliJ is a two-step process:

1. **AST (Abstract Syntax Tree)** — Built from tokens using `ASTNode` instances with `IElementType`
2. **PSI Layer** — Wraps AST, adding semantic meaning via `PsiElement` implementations

AST nodes map directly to text ranges. Operations (insert, remove, reorder) immediately reflect in text.

## Grammar-Kit Plugin (Recommended)

Generate parsers from BNF grammars. Provides:
- Automatic code generation for parsers and PSI classes
- Syntax highlighting for `.bnf` files
- Quick navigation and refactoring
- Gradle integration via Gradle Grammar-Kit Plugin

### BNF Grammar Example (`MyLanguage.bnf`)

```
{
    parserClass="com.example.parser.MyParser"
    extends="com.intellij.extapi.psi.ASTWrapperPsiElement"

    psiClassPrefix="My"
    psiImplClassSuffix="Impl"
    psiPackage="com.example.psi"
    psiImplPackage="com.example.psi.impl"

    elementTypeHolderClass="com.example.psi.MyTypes"
    elementTypeClass="com.example.psi.MyElementType"
    tokenTypeClass="com.example.psi.MyTokenType"
}

// Grammar rules
myFile ::= item_*

private item_ ::= (module_declaration | COMMENT | CRLF)

module_declaration ::= 'module' IDENTIFIER '{' module_body '}'

module_body ::= (definition)*

definition ::= val_definition | def_definition

val_definition ::= 'val' IDENTIFIER '=' expression

def_definition ::= 'def' IDENTIFIER '(' parameter_list? ')' '=' expression

parameter_list ::= IDENTIFIER (',' IDENTIFIER)*

expression ::= IDENTIFIER | NUMBER | STRING | call_expression | binary_expression

call_expression ::= IDENTIFIER '(' argument_list? ')'

argument_list ::= expression (',' expression)*

binary_expression ::= expression operator expression

operator ::= '+' | '-' | '*' | '/' | '==' | '!='
```

### Generated Classes

Grammar-Kit generates:
- `MyParser` — the parser class
- `MyTypes` — element type holder (all `IElementType` constants)
- PSI interfaces (`MyModuleDeclaration`, `MyDefinition`, etc.)
- PSI implementations (`MyModuleDeclarationImpl`, etc.)

### ANTLR4 Alternative

For reusing ANTLR4 grammars, use `antlr4-intellij-adaptor` third-party library.

## Manual Parser Implementation

### PsiBuilder

The parser receives a `PsiBuilder` that manages the token stream:

```kotlin
class MyParser : PsiParser {
    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        val rootMarker = builder.mark()
        while (!builder.eof()) {
            parseStatement(builder)
        }
        rootMarker.done(root)
        return builder.treeBuilt
    }

    private fun parseStatement(builder: PsiBuilder) {
        val marker = builder.mark()
        // ... parse logic
        marker.done(MyTypes.STATEMENT)
    }
}
```

### PsiBuilder.Marker System

Markers define token ranges for AST nodes:

```kotlin
val marker = builder.mark()

// Success: complete marker with element type
marker.done(MyTypes.EXPRESSION)

// Failure: drop marker without creating node
marker.drop()

// Rollback: revert lexer position to marker start
marker.rollbackTo()

// Precede: create new marker before this one (for left-to-right parsing)
val precedingMarker = marker.precede()
```

**`precede()` example** — parsing `a + b + c` as `((a+b)+c)`:

```kotlin
fun parseExpression(builder: PsiBuilder) {
    var left = builder.mark()
    parsePrimary(builder)  // parses 'a'
    left.done(MyTypes.EXPRESSION)

    while (builder.tokenType == MyTypes.PLUS) {
        val composite = left.precede()  // new marker before 'a'
        builder.advanceLexer()          // consume '+'
        parsePrimary(builder)           // parse 'b'
        left = composite
        left.done(MyTypes.BINARY_EXPRESSION)
    }
}
```

## Whitespace and Comment Handling

`PsiBuilder` automatically handles whitespace and comments:

```kotlin
class MyParserDefinition : ParserDefinition {
    override fun getWhitespaceTokens(): TokenSet = TokenSet.WHITE_SPACE

    override fun getCommentTokens(): TokenSet = MyTokenSets.COMMENTS

    // PsiBuilder automatically omits these from the token stream
    // Node ranges exclude leading/trailing whitespace
}
```

Comment token set is also used for TODO item detection.

## ParserDefinition Implementation

```kotlin
class MyParserDefinition : ParserDefinition {

    override fun createLexer(project: Project): Lexer = MyLexerAdapter()

    override fun createParser(project: Project): PsiParser = MyParser()

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getCommentTokens(): TokenSet = MyTokenSets.COMMENTS

    override fun getWhitespaceTokens(): TokenSet = TokenSet.WHITE_SPACE

    override fun getStringLiteralElements(): TokenSet = MyTokenSets.STRINGS

    override fun createElement(node: ASTNode): PsiElement = MyTypes.Factory.createElement(node)

    override fun createFile(viewProvider: FileViewProvider): PsiFile = MyFile(viewProvider)

    companion object {
        val FILE = IFileElementType(MyLanguage.INSTANCE)
    }
}
```

Register:

```xml
<extensions defaultExtensionNs="com.intellij">
    <lang.parserDefinition
        language="MyLanguage"
        implementationClass="com.example.MyParserDefinition"/>
</extensions>
```

## PSI Element Implementation

### File

```kotlin
class MyFile(viewProvider: FileViewProvider) :
    PsiFileBase(viewProvider, MyLanguage.INSTANCE) {

    override fun getFileType(): FileType = MyFileType.INSTANCE

    override fun toString(): String = "My File"
}
```

### Named Element (for Rename/Find Usages)

```kotlin
interface MyNamedElement : PsiNamedElement, PsiNameIdentifierOwner

class MyNamedElementImpl(node: ASTNode) :
    ASTWrapperPsiElement(node), MyNamedElement {

    override fun getName(): String? = nameIdentifier?.text

    override fun setName(name: String): PsiElement {
        val newNode = MyElementFactory.createIdentifier(project, name)
        nameIdentifier?.replace(newNode)
        return this
    }

    override fun getNameIdentifier(): PsiElement? =
        findChildByType(MyTypes.IDENTIFIER)
}
```

## Debugging

Use PsiViewer plugin or built-in PSI Viewer (Tools > View PSI Structure) to inspect PSI trees.
