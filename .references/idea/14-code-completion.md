# Code Completion

Source: https://plugins.jetbrains.com/docs/intellij/code-completion.html

## Two Approaches

### 1. Reference-Based Completion (Simple)

Completion from `PsiReference.getVariants()`:

```kotlin
class MyReference(element: PsiElement, range: TextRange) :
    PsiReferenceBase<PsiElement>(element, range) {

    override fun resolve(): PsiElement? { /* ... */ }

    override fun getVariants(): Array<Any> {
        val declarations = collectVisibleDeclarations(element)
        return declarations.map { decl ->
            LookupElementBuilder.create(decl)
                .withIcon(MyIcons.SYMBOL)
                .withTypeText(decl.containingFile.name)
        }.toTypedArray()
    }
}
```

### 2. Contributor-Based Completion (Flexible)

Supports all completion types with full control.

## CompletionContributor

```kotlin
class MyCompletionContributor : CompletionContributor() {
    init {
        // Keyword completion
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(MyLanguage.INSTANCE),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    result.addElement(
                        LookupElementBuilder.create("module")
                            .bold()
                            .withTypeText("keyword")
                    )
                    result.addElement(
                        LookupElementBuilder.create("def")
                            .bold()
                            .withTypeText("keyword")
                    )
                    result.addElement(
                        LookupElementBuilder.create("val")
                            .bold()
                            .withTypeText("keyword")
                    )
                }
            }
        )

        // Context-specific completion
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withParent(MyExpression::class.java),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val file = parameters.originalFile
                    val defs = PsiTreeUtil.collectElementsOfType(
                        file, MyDefinition::class.java
                    )
                    for (def in defs) {
                        result.addElement(
                            LookupElementBuilder.create(def)
                                .withIcon(MyIcons.DEFINITION)
                                .withTailText("(${def.parameterList})", true)
                                .withTypeText(def.returnType)
                        )
                    }
                }
            }
        )
    }
}
```

Register:

```xml
<extensions defaultExtensionNs="com.intellij">
    <completion.contributor
        language="MyLanguage"
        implementationClass="com.example.MyCompletionContributor"/>
</extensions>
```

**Important**: Element patterns match against leaf PSI elements. Use `withParent()` or `withSuperParent()` to match composite elements.

## LookupElement Configuration

```kotlin
LookupElementBuilder.create("myFunction")
    .withIcon(MyIcons.FUNCTION)               // Left icon
    .withTypeText("String")                    // Right-aligned type text
    .withTailText("(a: Int, b: Int)", true)   // Grayed tail text
    .bold()                                    // Bold text
    .withInsertHandler { context, item ->       // Post-insert action
        context.document.insertString(
            context.tailOffset, "()"
        )
        context.editor.caretModel.moveToOffset(
            context.tailOffset - 1
        )
    }
```

### Advanced Rendering

```kotlin
LookupElementBuilder.create("item")
    .withRenderer(object : LookupElementRenderer<LookupElement>() {
        override fun renderElement(
            element: LookupElement,
            presentation: LookupElementPresentation
        ) {
            presentation.itemText = "Custom rendered text"
            presentation.isItemTextBold = true
        }
    })
```

### Expensive Rendering

```kotlin
LookupElementBuilder.create("item")
    .withExpensiveRenderer(object : LookupElementRenderer<LookupElement>() {
        override fun renderElement(
            element: LookupElement,
            presentation: LookupElementPresentation
        ) {
            // Background-calculated presentation
            presentation.typeText = computeExpensiveTypeInfo()
        }
    })
```

## Priority Control

```kotlin
PrioritizedLookupElement.withPriority(lookupElement, 100.0)
PrioritizedLookupElement.withGrouping(lookupElement, 1)
PrioritizedLookupElement.withExplicitProximity(lookupElement, 5)
```

## Completion Confidence

Suppress auto-popup in certain contexts:

```kotlin
class MyCompletionConfidence : CompletionConfidence() {
    override fun shouldSkipAutopopup(
        element: PsiElement,
        editor: Editor,
        offset: Int
    ): ThreeState {
        if (element.parent is MyComment) {
            return ThreeState.YES  // Skip in comments
        }
        return ThreeState.UNSURE  // Default behavior
    }
}
```

```xml
<extensions defaultExtensionNs="com.intellij">
    <completion.confidence
        language="MyLanguage"
        implementationClass="com.example.MyCompletionConfidence"/>
</extensions>
```

## Programmatic Auto-Popup

```kotlin
class MyTypedHandler : TypedHandlerDelegate() {
    override fun checkAutoPopup(
        charTyped: Char,
        project: Project,
        editor: Editor,
        file: PsiFile
    ): Result {
        if (charTyped == '.') {
            AutoPopupController.getInstance(project).scheduleAutoPopup(editor)
            return Result.STOP
        }
        return Result.CONTINUE
    }
}
```

```xml
<extensions defaultExtensionNs="com.intellij">
    <typedHandler implementation="com.example.MyTypedHandler"/>
</extensions>
```

## Dumb Mode

Keyword completions not depending on indexes can be marked dumb-aware by implementing `DumbAware` on the `CompletionContributor`.
