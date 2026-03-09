# Program Structure Interface (PSI)

Source: https://plugins.jetbrains.com/docs/intellij/psi.html

## Overview

PSI is the layer responsible for parsing files and creating the syntactic and semantic code model. It powers code analysis, navigation, refactoring, completion, and most IDE features.

## PSI Files

- `PsiFile` is the root of a PSI tree, representing a file's contents as a hierarchy of elements
- Created via `PsiManager.getInstance(project).findFile(virtualFile)`
- Or via `PsiDocumentManager.getInstance(project).getPsiFile(document)`
- Each language provides its own `PsiFile` subclass (e.g., `PsiJavaFile`)
- `FileViewProvider` manages PSI for files with multiple languages (e.g., JSP)

## PSI Elements

- `PsiElement` is the base interface for all PSI tree nodes
- Elements have a parent, children, siblings
- Elements map to text ranges in the document
- Common operations:
  - `getText()` — element text
  - `getTextRange()` — text range in document
  - `getParent()`, `getChildren()`, `getNextSibling()`, `getPrevSibling()`
  - `getContainingFile()` — the `PsiFile` this element belongs to
  - `findElementAt(offset)` — find leaf element at offset
  - `accept(PsiElementVisitor)` — visitor pattern

## PSI Tree Structure

```
PsiFile
├── PsiClass
│   ├── PsiModifierList
│   ├── PsiIdentifier (class name)
│   ├── PsiMethod
│   │   ├── PsiModifierList
│   │   ├── PsiIdentifier (method name)
│   │   ├── PsiParameterList
│   │   └── PsiCodeBlock
│   └── PsiField
└── ...
```

## PSI References

- `PsiReference` connects a usage to its declaration
- `PsiElement.getReference()` / `getReferences()` — get references from an element
- `PsiReference.resolve()` — find the target declaration
- `PsiReference.getVariants()` — get completion variants
- Used for Go to Declaration, Find Usages, Rename, Completion

## Virtual File System (VFS)

- `VirtualFile` — abstraction over physical files
- Provides a snapshot of files accessed via IDE
- Changes tracked asynchronously via `VirtualFileListener`
- Get VFS instance: `LocalFileSystem.getInstance()`
- Convert: `PsiFile.getVirtualFile()`, `PsiManager.findFile(virtualFile)`

## Documents

- `Document` — editable sequence of Unicode characters, line-based
- Maps 1:1 to VFS files loaded in memory
- Get: `FileDocumentManager.getDocument(virtualFile)`
- Convert: `PsiDocumentManager.getDocument(psiFile)`
- Modifications require write action on EDT

## Utility Classes

- `PsiTreeUtil` — tree traversal utilities
  - `getParentOfType()`, `findChildOfType()`, `getChildrenOfType()`
  - `collectElementsOfType()`, `findFirstParent()`
- `PsiUtilCore` — core PSI utilities
- `PsiManager` — PSI lifecycle management

## Navigating PSI

```kotlin
// Find element at caret
val element = psiFile.findElementAt(editor.caretModel.offset)

// Walk up to find containing class
val containingClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)

// Find all elements of type
val allMethods = PsiTreeUtil.collectElementsOfType(psiFile, PsiMethod::class.java)
```

## Modifying PSI

All PSI modifications require write actions:

```kotlin
WriteCommandAction.runWriteCommandAction(project) {
    // Create new element
    val factory = PsiElementFactory.getInstance(project)
    val newElement = factory.createExpressionFromText("newValue", null)

    // Replace
    oldElement.replace(newElement)

    // Add
    parentElement.add(newElement)

    // Delete
    element.delete()
}
```

## PSI Viewer

Use the built-in PSI Viewer (Tools > View PSI Structure) or the PsiViewer plugin to inspect PSI trees for debugging.
