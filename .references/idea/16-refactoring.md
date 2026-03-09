# Rename Refactoring

Source: https://plugins.jetbrains.com/docs/intellij/rename-refactoring.html

## How It Works

Rename mirrors Find Usages. When renaming:

1. `PsiNamedElement.setName()` is called on the declaration
2. `PsiReference.handleElementRename()` is called on all references

Both replace the underlying AST node with new text.

## setName Implementation

Create a dummy file, parse it, extract the replacement node:

```kotlin
class MyNamedElementImpl(node: ASTNode) :
    ASTWrapperPsiElement(node), MyNamedElement {

    override fun setName(name: String): PsiElement {
        val newIdentifier = MyElementFactory.createIdentifier(project, name)
        nameIdentifier?.replace(newIdentifier)
        return this
    }

    override fun getName(): String? = nameIdentifier?.text

    override fun getNameIdentifier(): PsiElement? =
        findChildByType(MyTypes.IDENTIFIER)
}
```

### Element Factory

```kotlin
object MyElementFactory {
    fun createIdentifier(project: Project, name: String): PsiElement {
        val dummyFile = PsiFileFactory.getInstance(project)
            .createFileFromText(
                "dummy.my",
                MyFileType.INSTANCE,
                "val $name = 0"
            )
        // Extract the identifier from parsed dummy file
        return PsiTreeUtil.findChildOfType(dummyFile, MyIdentifier::class.java)!!
    }
}
```

## Name Validation

### NamesValidator

```kotlin
class MyNamesValidator : NamesValidator {
    override fun isKeyword(name: String, project: Project?): Boolean {
        return name in setOf("module", "def", "val", "var", "type", "import")
    }

    override fun isIdentifier(name: String, project: Project?): Boolean {
        return name.matches(Regex("[a-zA-Z_][a-zA-Z0-9_]*"))
    }
}
```

```xml
<extensions defaultExtensionNs="com.intellij">
    <lang.namesValidator
        language="MyLanguage"
        implementationClass="com.example.MyNamesValidator"/>
</extensions>
```

If not provided, Java validation rules apply.

### RenameInputValidator

More flexible validation with element pattern matching:

```kotlin
class MyRenameInputValidator : RenameInputValidator {
    override fun getPattern(): ElementPattern<out PsiElement> {
        return PlatformPatterns.psiElement(MyNamedElement::class.java)
    }

    override fun isInputValid(newName: String, element: PsiElement, context: ProcessingContext): Boolean {
        return newName.matches(Regex("[a-zA-Z_][a-zA-Z0-9_]*"))
    }
}
```

### RenameInputValidatorEx

With custom error messages:

```kotlin
class MyRenameInputValidatorEx : RenameInputValidatorEx {
    override fun getPattern(): ElementPattern<out PsiElement> { /* ... */ }

    override fun isInputValid(newName: String, element: PsiElement, context: ProcessingContext): Boolean {
        return getErrorMessage(newName, element.project) == null
    }

    override fun getErrorMessage(newName: String, project: Project): String? {
        if (newName.startsWith("_")) return "Names cannot start with underscore"
        if (!newName.matches(Regex("[a-zA-Z_][a-zA-Z0-9_]*"))) return "Invalid identifier"
        return null
    }
}
```

```xml
<extensions defaultExtensionNs="com.intellij">
    <renameInputValidator implementation="com.example.MyRenameInputValidator"/>
</extensions>
```

## Full Customization: RenameHandler

Completely replace rename UI and workflow:

```kotlin
class MyRenameHandler : RenameHandler {
    override fun isAvailableOnDataContext(dataContext: DataContext): Boolean { /* ... */ }
    override fun isRenaming(dataContext: DataContext): Boolean { /* ... */ }
    override fun invoke(project: Project, editor: Editor?, file: PsiFile?, dataContext: DataContext) { /* ... */ }
    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext) { /* ... */ }
}
```

## Partial Customization: RenamePsiElementProcessor

Extend default logic:

```kotlin
class MyRenamePsiElementProcessor : RenamePsiElementProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean {
        return element is MyNamedElement
    }

    override fun prepareRenaming(
        element: PsiElement,
        newName: String,
        allRenames: MutableMap<PsiElement, String>
    ) {
        // Add additional elements to rename together
        // e.g., rename related declarations
    }

    override fun findReferences(
        element: PsiElement,
        searchScope: SearchScope,
        searchInCommentsAndStrings: Boolean
    ): Collection<PsiReference> {
        // Custom reference search
        return super.findReferences(element, searchScope, searchInCommentsAndStrings)
    }
}
```

## Disabling Rename

```kotlin
class MyVetoRenameCondition : Condition<PsiElement> {
    override fun value(element: PsiElement): Boolean {
        // return true to PREVENT renaming
        return element is MyBuiltinElement
    }
}
```

```xml
<extensions defaultExtensionNs="com.intellij">
    <vetoRenameCondition implementation="com.example.MyVetoRenameCondition"/>
</extensions>
```

## Reference Handling

For references extending `PsiReferenceBase`, use `ElementManipulator.handleContentChange()` for content changes and text range calculation.
