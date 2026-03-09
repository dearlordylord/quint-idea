# Code Formatting

Source: https://plugins.jetbrains.com/docs/intellij/code-formatting.html

## Architecture

Formatting uses a tree of nested `Block` objects. Each block specifies whitespace constraints (indents, wraps, spacing, alignments). The engine calculates minimal modifications needed.

## FormattingModelBuilder

Entry point. Register at `com.intellij.lang.formatter`.

```kotlin
class MyFormattingModelBuilder : FormattingModelBuilder {
    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val codeStyleSettings = formattingContext.codeStyleSettings
        return FormattingModelProvider.createFormattingModelForPsiFile(
            formattingContext.containingFile,
            MyBlock(
                formattingContext.node,
                Wrap.createWrap(WrapType.NONE, false),
                Alignment.createAlignment(),
                createSpacingBuilder(codeStyleSettings)
            ),
            codeStyleSettings
        )
    }
}
```

```xml
<extensions defaultExtensionNs="com.intellij">
    <lang.formatter
        language="MyLanguage"
        implementationClass="com.example.MyFormattingModelBuilder"/>
</extensions>
```

## Block Implementation

Extend `AbstractBlock`:

```kotlin
class MyBlock(
    node: ASTNode,
    wrap: Wrap?,
    alignment: Alignment?,
    private val spacingBuilder: SpacingBuilder
) : AbstractBlock(node, wrap, alignment) {

    override fun buildChildren(): List<Block> {
        val blocks = mutableListOf<Block>()
        var child = myNode.firstChildNode
        while (child != null) {
            if (child.elementType != TokenType.WHITE_SPACE) {
                blocks.add(MyBlock(
                    child,
                    Wrap.createWrap(WrapType.NONE, false),
                    null,
                    spacingBuilder
                ))
            }
            child = child.treeNext
        }
        return blocks
    }

    override fun getIndent(): Indent? {
        // Indent children of blocks/bodies
        return when (myNode.elementType) {
            MyTypes.MODULE_BODY -> Indent.getNormalIndent()
            MyTypes.BLOCK -> Indent.getNormalIndent()
            else -> Indent.getNoneIndent()
        }
    }

    override fun getSpacing(child1: Block?, child2: Block): Spacing? {
        return spacingBuilder.getSpacing(this, child1, child2)
    }

    override fun getChildAttributes(newChildIndex: Int): ChildAttributes {
        // Determines indent when Enter is pressed
        return when (myNode.elementType) {
            MyTypes.MODULE_BODY, MyTypes.BLOCK ->
                ChildAttributes(Indent.getNormalIndent(), null)
            else ->
                ChildAttributes(Indent.getNoneIndent(), null)
        }
    }

    override fun isLeaf(): Boolean = myNode.firstChildNode == null
}
```

**Critical rule**: The block tree must cover all non-whitespace characters, or the formatter may delete characters between blocks.

## SpacingBuilder

Declarative spacing rules:

```kotlin
fun createSpacingBuilder(settings: CodeStyleSettings): SpacingBuilder {
    return SpacingBuilder(settings, MyLanguage.INSTANCE)
        // Space after keywords
        .after(MyTypes.MODULE_KEYWORD).spaces(1)
        .after(MyTypes.DEF_KEYWORD).spaces(1)
        .after(MyTypes.VAL_KEYWORD).spaces(1)
        // Space around operators
        .around(MyTypes.OPERATOR).spaces(1)
        .around(MyTypes.EQUALS).spaces(1)
        // No space inside parentheses
        .after(MyTypes.LPAREN).none()
        .before(MyTypes.RPAREN).none()
        // Newline after opening brace
        .after(MyTypes.LBRACE).lineBreakInCode()
        // Newline before closing brace
        .before(MyTypes.RBRACE).lineBreakInCode()
        // Space after comma
        .after(MyTypes.COMMA).spaces(1)
        .before(MyTypes.COMMA).none()
}
```

## Indent Types

| Factory Method | Purpose |
|---------------|---------|
| `Indent.getNoneIndent()` | No indent relative to parent |
| `Indent.getNormalIndent()` | Standard indent per code style |
| `Indent.getContinuationIndent()` | Multi-line statement indent |
| `Indent.getAbsoluteNoneIndent()` | Absolute zero indent |

## Spacing

```kotlin
Spacing.createSpacing(
    minSpaces,        // Minimum spaces
    maxSpaces,        // Maximum spaces
    minLineFeeds,     // Minimum line breaks
    keepLineBreaks,   // Preserve existing line breaks
    keepBlankLines    // Max blank lines to preserve
)
```

## Wrap

```kotlin
Wrap.createWrap(WrapType.NONE, false)         // Never wrap
Wrap.createWrap(WrapType.NORMAL, false)       // Wrap if exceeds margin
Wrap.createWrap(WrapType.ALWAYS, false)       // Always wrap
Wrap.createWrap(WrapType.CHOP_DOWN_IF_LONG, false)  // Chop all if any exceeds
```

## Alignment

Blocks returning the same `Alignment` instance align together:

```kotlin
val alignment = Alignment.createAlignment()
// Pass same instance to related blocks
MyBlock(child1, wrap, alignment, spacingBuilder)
MyBlock(child2, wrap, alignment, spacingBuilder)
```

## Code Style Settings

```kotlin
class MyCodeStyleSettings(container: CodeStyleSettings) :
    CustomCodeStyleSettings("MyCodeStyleSettings", container) {
    @JvmField var SPACE_AFTER_COLON = true
    @JvmField var INDENT_SIZE = 4
}
```

### Code Style Settings Provider

```kotlin
class MyLanguageCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {
    override fun getLanguage(): Language = MyLanguage.INSTANCE

    override fun getCodeSample(settingsType: SettingsType): String = """
        module Example {
            val x = 42
            def foo(a, b) = a + b
        }
    """.trimIndent()
}
```

### Indent Options

```kotlin
class MyFileTypeIndentOptionsProvider : FileTypeIndentOptionsProvider {
    override fun getIndentOptions(): CommonCodeStyleSettings.IndentOptions {
        return CommonCodeStyleSettings.IndentOptions().apply {
            INDENT_SIZE = 2
            TAB_SIZE = 2
            USE_TAB_CHARACTER = false
        }
    }

    override fun getFileType(): FileType = MyFileType.INSTANCE
}
```

```xml
<extensions defaultExtensionNs="com.intellij">
    <fileTypeIndentOptionsProvider
        implementation="com.example.MyFileTypeIndentOptionsProvider"/>
</extensions>
```

## External Formatter

Delegate to external tools:

```kotlin
class MyExternalFormatter : AsyncDocumentFormattingService() {
    override fun createFormattingTask(request: AsyncFormattingRequest): FormattingTask {
        // Launch external formatter
    }

    override fun getNotificationGroupId(): String = "My Formatter"
    override fun getName(): String = "My External Formatter"
    override fun getFeatures(): Set<Feature> = setOf()
}
```

```xml
<extensions defaultExtensionNs="com.intellij">
    <formattingService implementation="com.example.MyExternalFormatter"/>
</extensions>
```

## Pre/Post Processors

```xml
<extensions defaultExtensionNs="com.intellij">
    <preFormatProcessor implementation="com.example.MyPreFormatProcessor"/>
    <postFormatProcessor implementation="com.example.MyPostFormatProcessor"/>
</extensions>
```
