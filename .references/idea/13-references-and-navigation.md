# References, Resolve, and Navigation

Sources:
- https://plugins.jetbrains.com/docs/intellij/references-and-resolve.html
- https://plugins.jetbrains.com/docs/intellij/navigation.html

## Overview

References connect code usages to declarations, enabling:
- Navigate > Declaration (Ctrl/Cmd+B)
- Find Usages
- Rename Refactoring
- Code Completion

## PsiReference

### Basic Implementation

```kotlin
class MyReference(element: PsiElement, rangeInElement: TextRange) :
    PsiReferenceBase<PsiElement>(element, rangeInElement) {

    override fun resolve(): PsiElement? {
        val name = element.text
        // Walk up the tree to find declarations
        return findDeclaration(element, name)
    }

    override fun getVariants(): Array<Any> {
        // Return completion variants
        val declarations = collectVisibleDeclarations(element)
        return declarations.map { decl ->
            LookupElementBuilder.create(decl)
                .withTypeText(decl.containingFile.name)
                .withIcon(MyIcons.FILE)
        }.toTypedArray()
    }

    private fun findDeclaration(context: PsiElement, name: String): PsiElement? {
        // Search in containing file
        val file = context.containingFile
        return PsiTreeUtil.collectElementsOfType(file, MyNamedElement::class.java)
            .firstOrNull { it.name == name }
    }
}
```

### Providing References from PSI Elements

```kotlin
class MyReferenceElement(node: ASTNode) : ASTWrapperPsiElement(node) {
    override fun getReference(): PsiReference {
        return MyReference(this, TextRange(0, textLength))
    }
}
```

### Multi-Target Resolution (PsiPolyVariantReference)

```kotlin
class MyPolyReference(element: PsiElement, rangeInElement: TextRange) :
    PsiReferenceBase.Poly<PsiElement>(element, rangeInElement, true) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val name = element.text
        val declarations = findAllDeclarations(element, name)
        return declarations.map { PsiElementResolveResult(it) }.toTypedArray()
    }
}
```

## Reference Contributor

Register references for elements via patterns:

```kotlin
class MyReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(MyTypes.IDENTIFIER),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext
                ): Array<PsiReference> {
                    return arrayOf(MyReference(element, TextRange(0, element.textLength)))
                }
            }
        )
    }
}
```

```xml
<extensions defaultExtensionNs="com.intellij">
    <psi.referenceContributor
        language="MyLanguage"
        implementation="com.example.MyReferenceContributor"/>
</extensions>
```

## Resolve Process

### PsiScopeProcessor

For complex resolve logic — walks up the PSI tree gathering declarations:

```kotlin
class MyResolveProcessor(private val name: String) : PsiScopeProcessor {
    var result: PsiElement? = null

    override fun execute(element: PsiElement, state: ResolveState): Boolean {
        if (element is MyNamedElement && element.name == name) {
            result = element
            return false  // found, stop searching
        }
        return true  // continue searching
    }
}
```

### Cross-File References

```kotlin
// Find files by name
val files = FilenameIndex.getFilesByName(project, "config.my", GlobalSearchScope.allScope(project))

// Iterate project content
ProjectFileIndex.getInstance(project).iterateContent { virtualFile ->
    // process file
    true
}
```

## Navigation

### Direct Navigation

```kotlin
class MyDirectNavigationProvider : DirectNavigationProvider {
    override fun getNavigationElement(element: PsiElement): PsiElement? {
        // Return target element for direct jump
        if (element is MyReference) {
            return element.resolve()
        }
        return null
    }
}
```

```xml
<extensions defaultExtensionNs="com.intellij">
    <lang.directNavigationProvider
        implementation="com.example.MyDirectNavigationProvider"/>
</extensions>
```

### Go to Symbol

```kotlin
class MySymbolNavigationContributor : ChooseByNameContributorEx {
    override fun processNames(
        processor: Processor<in String>,
        scope: GlobalSearchScope,
        filter: IdFilter?
    ) {
        // Add all symbol names
        StubIndex.getInstance().processAllKeys(
            MyStubIndex.KEY,
            processor,
            scope,
            filter
        )
    }

    override fun processElementsWithName(
        name: String,
        processor: Processor<in NavigationItem>,
        parameters: FindSymbolParameters
    ) {
        StubIndex.getElements(
            MyStubIndex.KEY,
            name,
            parameters.project,
            parameters.searchScope,
            MyNamedElement::class.java
        ).forEach { processor.process(it) }
    }
}
```

```xml
<extensions defaultExtensionNs="com.intellij">
    <gotoSymbolContributor
        implementation="com.example.MySymbolNavigationContributor"/>
</extensions>
```

### Structure View

```kotlin
class MyStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder {
        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel {
                return MyStructureViewModel(psiFile)
            }
        }
    }
}
```

```xml
<extensions defaultExtensionNs="com.intellij">
    <lang.psiStructureViewFactory
        language="MyLanguage"
        implementationClass="com.example.MyStructureViewFactory"/>
</extensions>
```

## Highlighted References

Implement `HighlightedReference` to visually highlight non-obvious references (e.g., within strings).

## Target Elements

The resolved target should implement `PsiNamedElement` or `PsiNameIdentifierOwner` for rename support.
