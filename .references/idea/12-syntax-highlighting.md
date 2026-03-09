# Syntax Highlighting

Source: https://plugins.jetbrains.com/docs/intellij/syntax-highlighting-and-error-highlighting.html

## Three Levels of Highlighting

1. **Lexer-based** — Fast, token-level coloring
2. **Parser-based** — Error highlighting from parsing
3. **Annotator-based** — Semantic analysis, complex highlighting

## TextAttributesKey

Foundation of all highlighting. One instance per distinct item type (keyword, string, number, etc.).

```kotlin
object MyHighlightingColors {
    val KEYWORD = TextAttributesKey.createTextAttributesKey(
        "MY_KEYWORD",
        DefaultLanguageHighlighterColors.KEYWORD
    )
    val STRING = TextAttributesKey.createTextAttributesKey(
        "MY_STRING",
        DefaultLanguageHighlighterColors.STRING
    )
    val NUMBER = TextAttributesKey.createTextAttributesKey(
        "MY_NUMBER",
        DefaultLanguageHighlighterColors.NUMBER
    )
    val COMMENT = TextAttributesKey.createTextAttributesKey(
        "MY_COMMENT",
        DefaultLanguageHighlighterColors.LINE_COMMENT
    )
    val IDENTIFIER = TextAttributesKey.createTextAttributesKey(
        "MY_IDENTIFIER",
        DefaultLanguageHighlighterColors.IDENTIFIER
    )
    val BAD_CHARACTER = TextAttributesKey.createTextAttributesKey(
        "MY_BAD_CHARACTER",
        HighlighterColors.BAD_CHARACTER
    )
}
```

Common parent keys from `DefaultLanguageHighlighterColors`:
- `KEYWORD`, `STRING`, `NUMBER`, `LINE_COMMENT`, `BLOCK_COMMENT`
- `IDENTIFIER`, `FUNCTION_DECLARATION`, `FUNCTION_CALL`
- `CONSTANT`, `LOCAL_VARIABLE`, `PARAMETER`
- `CLASS_NAME`, `INTERFACE_NAME`
- `OPERATION_SIGN`, `BRACES`, `BRACKETS`, `PARENTHESES`
- `COMMA`, `DOT`, `SEMICOLON`

## Level 1: Lexer-Based (SyntaxHighlighter)

```kotlin
class MySyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer(): Lexer = MyLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when (tokenType) {
            MyTypes.MODULE_KEYWORD,
            MyTypes.DEF_KEYWORD,
            MyTypes.VAL_KEYWORD -> pack(MyHighlightingColors.KEYWORD)

            MyTypes.STRING -> pack(MyHighlightingColors.STRING)
            MyTypes.NUMBER -> pack(MyHighlightingColors.NUMBER)
            MyTypes.LINE_COMMENT -> pack(MyHighlightingColors.COMMENT)
            MyTypes.IDENTIFIER -> pack(MyHighlightingColors.IDENTIFIER)
            TokenType.BAD_CHARACTER -> pack(MyHighlightingColors.BAD_CHARACTER)

            else -> EMPTY_KEYS
        }
    }
}
```

### Factory Registration

```kotlin
class MySyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter {
        return MySyntaxHighlighter()
    }
}
```

```xml
<extensions defaultExtensionNs="com.intellij">
    <lang.syntaxHighlighterFactory
        language="MyLanguage"
        implementationClass="com.example.MySyntaxHighlighterFactory"/>
</extensions>
```

## Color Settings Page

Allow users to configure colors:

```kotlin
class MyColorSettingsPage : ColorSettingsPage {

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName(): String = "My Language"

    override fun getIcon(): Icon = MyIcons.FILE

    override fun getHighlighter(): SyntaxHighlighter = MySyntaxHighlighter()

    override fun getDemoText(): String = """
        // Comment
        module MyModule {
            val x = 42
            def foo(a, b) = a + b
        }
    """.trimIndent()

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null

    companion object {
        private val DESCRIPTORS = arrayOf(
            AttributesDescriptor("Keyword", MyHighlightingColors.KEYWORD),
            AttributesDescriptor("String", MyHighlightingColors.STRING),
            AttributesDescriptor("Number", MyHighlightingColors.NUMBER),
            AttributesDescriptor("Comment", MyHighlightingColors.COMMENT),
            AttributesDescriptor("Identifier", MyHighlightingColors.IDENTIFIER),
            AttributesDescriptor("Bad character", MyHighlightingColors.BAD_CHARACTER),
        )
    }
}
```

```xml
<extensions defaultExtensionNs="com.intellij">
    <colorSettingsPage implementation="com.example.MyColorSettingsPage"/>
</extensions>
```

## Level 2: Parser-Based

Parser errors automatically create error highlights. Use `PsiBuilder.error()` during parsing:

```kotlin
builder.error("Expected expression")
```

## Level 3: Annotator

For semantic highlighting — analyzes PSI, not just tokens.

```kotlin
class MyAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is MyFunctionCall -> {
                // Highlight function call name
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(element.nameIdentifier)
                    .textAttributes(DefaultLanguageHighlighterColors.FUNCTION_CALL)
                    .create()
            }
            is MyDefinition -> {
                // Check for errors
                if (element.name == null) {
                    holder.newAnnotation(HighlightSeverity.ERROR, "Missing name")
                        .create()
                }
            }
        }
    }
}
```

### With Quick Fix

```kotlin
holder.newAnnotation(HighlightSeverity.WARNING, "Unused variable")
    .range(element.textRange)
    .withFix(RemoveUnusedVariableFix(element))
    .create()
```

Register:

```xml
<extensions defaultExtensionNs="com.intellij">
    <annotator language="MyLanguage"
               implementationClass="com.example.MyAnnotator"/>
</extensions>
```

## External Annotator

For integration with external tools (linters, type checkers):

```kotlin
class MyExternalAnnotator :
    ExternalAnnotator<PsiFile, List<MyDiagnostic>>() {

    override fun collectInformation(file: PsiFile): PsiFile = file

    override fun doAnnotate(file: PsiFile): List<MyDiagnostic> {
        // Run external tool, return diagnostics
    }

    override fun apply(file: PsiFile, diagnostics: List<MyDiagnostic>, holder: AnnotationHolder) {
        for (diagnostic in diagnostics) {
            holder.newAnnotation(diagnostic.severity, diagnostic.message)
                .range(diagnostic.range)
                .create()
        }
    }
}
```

```xml
<extensions defaultExtensionNs="com.intellij">
    <externalAnnotator language="MyLanguage"
                       implementationClass="com.example.MyExternalAnnotator"/>
</extensions>
```

## Semantic Highlighting (Rainbow)

```kotlin
class MyRainbowVisitor : RainbowVisitor() {
    override fun suitableForFile(file: PsiFile): Boolean =
        file is MyFile

    override fun visit(element: PsiElement) {
        if (element is MyParameter) {
            addInfo(getInfo(element.containingFile, element, element.name, null))
        }
    }
}
```

## Performance

Annotators run in parallel. Produce highlights as close as possible to the relevant `PsiElement` for faster incremental updates.
