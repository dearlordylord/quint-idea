# Testing IntelliJ Plugins

Sources:
- https://plugins.jetbrains.com/docs/intellij/testing-plugins.html
- https://plugins.jetbrains.com/docs/intellij/tests-and-fixtures.html

## Philosophy

- **Model-level functional testing** rather than isolated unit tests
- Tests run in a **headless environment** (no UI) using actual production implementations
- Focus on **complete features** rather than individual functions
- Source files serve as input; output compared against expected results
- Tests are stable and require little maintenance regardless of refactoring
- Mocking is **discouraged** — use real components

## Test Frameworks

JUnit 3, JUnit 4, JUnit 5, and TestNG are supported. Most platform tests use JUnit 3 style.

## Base Test Classes

### BasePlatformTestCase (Recommended)

For most plugin tests. Uses a lightweight in-memory project.

```kotlin
class MyTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "src/test/testData"

    fun testHighlighting() {
        myFixture.configureByFile("test.my")
        myFixture.checkHighlighting(
            true,   // check errors
            false,  // check warnings
            true    // check infos
        )
    }

    fun testCompletion() {
        myFixture.configureByText("test.my", "val x = mo<caret>")
        myFixture.completeBasic()
        val lookupStrings = myFixture.lookupElementStrings
        assertContainsElements(lookupStrings!!, "module")
    }

    fun testRename() {
        myFixture.configureByText("test.my", """
            val <caret>foo = 42
            val bar = foo
        """.trimIndent())
        myFixture.renameElementAtCaret("baz")
        myFixture.checkResult("""
            val baz = 42
            val bar = baz
        """.trimIndent())
    }

    fun testReference() {
        myFixture.configureByText("test.my", """
            val foo = 42
            val bar = <caret>foo
        """.trimIndent())
        val ref = myFixture.getReferenceAtCaretPosition()
        val resolved = ref?.resolve()
        assertNotNull(resolved)
        assertInstanceOf(resolved, MyValDefinition::class.java)
    }
}
```

### HeavyPlatformTestCase

For tests requiring a full project on disk (file system operations, project model).

### CodeInsightFixtureTestCase

Provides `myFixture` of type `CodeInsightTestFixture` — the primary test interface.

## CodeInsightTestFixture Methods

```kotlin
// Configure
myFixture.configureByFile("file.my")           // From testData
myFixture.configureByText("file.my", "content") // Inline
myFixture.configureByFiles("a.my", "b.my")      // Multiple files

// Highlighting
myFixture.checkHighlighting()                    // Check all highlighting
myFixture.testHighlighting("file.my")           // One-liner

// Completion
myFixture.completeBasic()                        // Basic completion
myFixture.completeSmart()                        // Smart completion
myFixture.lookupElementStrings                   // Get completion items

// Navigation
myFixture.getReferenceAtCaretPosition()          // Get reference at caret
myFixture.getElementAtCaret()                    // Get PSI at caret

// Inspections
myFixture.enableInspections(MyInspection::class.java)
myFixture.launchAction(myFixture.findSingleIntention("Fix name"))

// Formatting
myFixture.configureByText("test.my", "unformatted code")
WriteCommandAction.runWriteCommandAction(project) {
    CodeStyleManager.getInstance(project).reformat(myFixture.file)
}
myFixture.checkResult("expected formatted code")

// Refactoring
myFixture.renameElementAtCaret("newName")

// Result verification
myFixture.checkResult("expected content")
myFixture.checkResultByFile("expected.my")
```

## Test Data

### Directory Structure

```
src/test/testData/
├── highlighting/
│   ├── test1.my
│   └── test2.my
├── completion/
│   └── basic.my
├── rename/
│   ├── before.my
│   └── after.my
└── inspection/
    ├── before.my
    └── after.my
```

### Highlighting Test Data Markers

```
val <error descr="Undefined variable">undeclared</error> = 42
val <warning descr="Unused variable">unused</warning> = 42
val <info descr="null">normal</info> = 42
```

### Caret Position

Use `<caret>` in test files:

```
val foo = <caret>bar
```

## Light vs Heavy Tests

| Feature | Light | Heavy |
|---------|-------|-------|
| Project | In-memory | Disk-based |
| Speed | Fast | Slower |
| File system | Limited | Full |
| Use case | Most tests | Project structure tests |

## Fixture Approach (Framework-Independent)

```kotlin
class MyTest {
    private lateinit var fixture: CodeInsightTestFixture

    @Before
    fun setUp() {
        val factory = IdeaTestFixtureFactory.getFixtureFactory()
        val projectBuilder = factory.fixtureBuilder()
        fixture = factory.createCodeInsightFixture(projectBuilder.fixture)
        fixture.setUp()
    }

    @After
    fun tearDown() {
        fixture.tearDown()
    }

    @Test
    fun testSomething() {
        fixture.configureByText("test.my", "content")
        // assertions
    }
}
```

## Testing Specific Features

### Testing Parser

```kotlin
fun testParser() {
    myFixture.configureByFile("parser/test.my")
    val psiFile = myFixture.file
    assertNoErrors(psiFile)  // custom helper
}
```

### Testing Inspections

```kotlin
fun testInspection() {
    myFixture.enableInspections(MyInspection::class.java)
    myFixture.testHighlighting("inspection/test.my")
}

fun testQuickFix() {
    myFixture.enableInspections(MyInspection::class.java)
    myFixture.configureByFile("inspection/before.my")
    myFixture.launchAction(myFixture.findSingleIntention("Fix name"))
    myFixture.checkResultByFile("inspection/after.my")
}
```

### Testing Find Usages

```kotlin
fun testFindUsages() {
    myFixture.configureByText("test.my", """
        val <caret>foo = 42
        val bar = foo + foo
    """.trimIndent())
    val usages = myFixture.findUsages(myFixture.elementAtCaret)
    assertEquals(2, usages.size)
}
```

## Gradle Test Configuration

```kotlin
tasks {
    test {
        systemProperty("idea.home.path", project.intellijPlatform.platformPath.get().toString())
    }
}
```
