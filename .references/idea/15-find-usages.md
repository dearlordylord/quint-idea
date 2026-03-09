# Find Usages

Source: https://plugins.jetbrains.com/docs/intellij/find-usages.html

## Core Components

### FindUsagesProvider

Primary extension point. Register at `com.intellij.lang.findUsagesProvider`.

```kotlin
class MyFindUsagesProvider : FindUsagesProvider {

    override fun getWordsScanner(): WordsScanner {
        return DefaultWordsScanner(
            MyLexerAdapter(),
            MyTokenSets.IDENTIFIERS,   // identifier tokens
            MyTokenSets.COMMENTS,      // comment tokens
            MyTokenSets.STRINGS        // string literal tokens
        )
    }

    override fun canFindUsagesFor(element: PsiElement): Boolean {
        return element is MyNamedElement
    }

    override fun getHelpId(element: PsiElement): String? = null

    override fun getType(element: PsiElement): String {
        return when (element) {
            is MyModuleDeclaration -> "module"
            is MyValDefinition -> "value"
            is MyDefDefinition -> "function"
            is MyParameter -> "parameter"
            else -> "element"
        }
    }

    override fun getDescriptiveName(element: PsiElement): String {
        return (element as? MyNamedElement)?.name ?: ""
    }

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String {
        return (element as? MyNamedElement)?.name ?: element.text
    }
}
```

Register:

```xml
<extensions defaultExtensionNs="com.intellij">
    <lang.findUsagesProvider
        language="MyLanguage"
        implementationClass="com.example.MyFindUsagesProvider"/>
</extensions>
```

## Prerequisites

- PSI elements must implement `PsiNamedElement` (and `PsiReference` for referencing elements)
- Override `PsiElement.getUseScope()` to narrow search scope for locals/parameters (performance)
- Override `PsiNamedElement.getTextOffset()` when text range includes extra tokens beyond identifier

## WordsScanner

Breaks file contents into categorized words for indexing:

```kotlin
DefaultWordsScanner(
    lexer,          // your lexer
    identifiers,    // TokenSet of identifier token types
    comments,       // TokenSet of comment token types
    strings         // TokenSet of string literal token types
)
```

## ElementDescriptionProvider

Provides descriptions for Find Usages tool window title:

```kotlin
class MyElementDescriptionProvider : ElementDescriptionProvider {
    override fun getElementDescription(
        element: PsiElement,
        location: ElementDescriptionLocation
    ): String? {
        if (element is MyNamedElement && location is UsageViewLongNameLocation) {
            return element.name
        }
        return null
    }
}
```

## UsageTypeProvider

Groups results by custom usage types:

```kotlin
class MyUsageTypeProvider : UsageTypeProvider {
    override fun getUsageType(element: PsiElement): UsageType? {
        val parent = element.parent
        return when {
            parent is MyValDefinition -> UsageType("Value definition")
            parent is MyFunctionCall -> UsageType("Function call")
            else -> null
        }
    }
}
```

```xml
<extensions defaultExtensionNs="com.intellij">
    <usageTypeProvider implementation="com.example.MyUsageTypeProvider"/>
</extensions>
```
