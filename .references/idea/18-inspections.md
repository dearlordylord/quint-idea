# Code Inspections and Intentions

Sources:
- https://plugins.jetbrains.com/docs/intellij/code-inspections.html
- https://plugins.jetbrains.com/docs/intellij/code-inspections-and-intentions.html

## Extension Points

- `com.intellij.localInspection` — Per-file, on-the-fly analysis
- `com.intellij.globalInspection` — Cross-file analysis

## LocalInspectionTool

```kotlin
class MyInspection : LocalInspectionTool() {

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is MyNamedElement) {
                    val name = element.name ?: return
                    if (name.startsWith("_")) {
                        holder.registerProblem(
                            element,
                            "Names should not start with underscore",
                            ProblemHighlightType.WARNING,
                            RenameQuickFix()
                        )
                    }
                }
            }
        }
    }
}
```

Register:

```xml
<extensions defaultExtensionNs="com.intellij">
    <localInspection
        language="MyLanguage"
        implementationClass="com.example.MyInspection"
        displayName="Naming convention"
        groupName="My Language"
        shortName="MyNamingConvention"
        enabledByDefault="true"
        level="WARNING"/>
</extensions>
```

## QuickFix

```kotlin
class RenameQuickFix : LocalQuickFix {
    override fun getFamilyName(): String = "Rename element"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement as? MyNamedElement ?: return
        val oldName = element.name ?: return
        val newName = oldName.removePrefix("_")

        WriteCommandAction.runWriteCommandAction(project) {
            element.setName(newName)
        }
    }
}
```

### Priority Control

Implement `HighPriorityAction` or `LowPriorityAction` to control fix ordering.

## Inspection Description

HTML file at: `$RESOURCES_ROOT$/inspectionDescriptions/$SHORT_NAME.html`

Example: `src/main/resources/inspectionDescriptions/MyNamingConvention.html`

```html
<html>
<body>
Reports names that start with an underscore.
<p>Names starting with underscore are discouraged in this language.</p>
<!-- tip -->
<p>Use the quick-fix to rename the element.</p>
<!-- tooltip end -->
<pre><code>
// Bad
val _foo = 42

// Good
val foo = 42
</code></pre>
</body>
</html>
```

Supports code snippets with language-specific syntax highlighting:
```html
<pre><code lang="MyLanguage">val example = 42</code></pre>
```

Settings links:
```html
<a href="settings://MyLanguageSettings">Configure settings</a>
```

## Annotator vs Inspection

| Feature | Annotator | Inspection |
|---------|-----------|------------|
| Scope | Per-element | Per-file visitor |
| User toggle | No | Yes (can be disabled) |
| Severity | Fixed | Configurable |
| Use case | Core syntax/semantic errors | Warnings, best practices |

## Suppressing Inspections

```kotlin
class MyInspectionSuppressor : InspectionSuppressor {
    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        // Check for suppression comments, annotations, etc.
        return false
    }

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> {
        return arrayOf(
            object : SuppressQuickFix {
                override fun getFamilyName(): String = "Suppress for this element"
                override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                    // Add suppression comment
                }
                override fun isAvailable(project: Project, context: PsiElement): Boolean = true
                override fun isSuppressAll(): Boolean = false
            }
        )
    }
}
```

## Intention Actions

Lightweight alternatives to inspections — offer code transformations without reporting problems:

```kotlin
class MyIntentionAction : IntentionAction {
    override fun getText(): String = "Convert to val"
    override fun getFamilyName(): String = "My Language intentions"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        val element = file?.findElementAt(editor?.caretModel?.offset ?: return false)
        return element?.parent is MyVarDefinition
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        // Transform code
    }

    override fun startInWriteAction(): Boolean = true
}
```

```xml
<extensions defaultExtensionNs="com.intellij">
    <intentionAction>
        <language>MyLanguage</language>
        <className>com.example.MyIntentionAction</className>
        <category>My Language</category>
    </intentionAction>
</extensions>
```

## Testing Inspections

```kotlin
class MyInspectionTest : BasePlatformTestCase() {
    override fun getTestDataPath(): String = "src/test/testData"

    fun testNamingConvention() {
        myFixture.enableInspections(MyInspection::class.java)
        myFixture.configureByFile("NamingConvention.my")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testQuickFix() {
        myFixture.enableInspections(MyInspection::class.java)
        myFixture.configureByFile("NamingConventionBefore.my")
        myFixture.launchAction(myFixture.findSingleIntention("Rename element"))
        myFixture.checkResultByFile("NamingConventionAfter.my")
    }
}
```

Test data files use `<warning>` and `<error>` markers:
```
val <warning descr="Names should not start with underscore">_foo</warning> = 42
```
