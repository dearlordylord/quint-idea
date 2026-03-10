# Learnings — Quint IntelliJ IDEA Plugin

Things we wish we knew before implementing. Organized by topic.

---

## Gradle / Build System

### IntelliJ Platform Gradle Plugin 2.12.0 requires Gradle 9.0+
The reference docs suggested Gradle 8.13, but plugin 2.12.0 throws `IntelliJ Platform Gradle Plugin requires Gradle 9.0.0 and higher`. Must use Gradle 9.x.

### `instrumentationTools()` does not exist in plugin 2.12.0
TASKS.md and reference docs mentioned `instrumentationTools()` as a dependency. It doesn't exist (likely renamed or removed). The build works without it — instrumentation happens automatically via the `instrumentCode` task.

### `TestFrameworkType` needs explicit import
```kotlin
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
```
Without this import at the top of `build.gradle.kts`, you get `Unresolved reference 'TestFrameworkType'`.

### `intellijIdeaCommunity()` is the correct function, not `intellijIdea()`
Reference docs show `intellijIdea("2024.3")` but the actual function for Community Edition is `intellijIdeaCommunity()`.

### foojay-resolver-convention is incompatible with Gradle 9.4
All tested versions (0.9.0, 0.10.0) fail with `NoSuchFieldError: IBM_SEMERU`. Don't use it with Gradle 9.x. Instead, rely on locally installed JDKs or set JAVA_HOME.

### JDK version matters for Kotlin compilation
- Kotlin 1.9.25 cannot parse Java version "25.0.2" — its `JavaVersion.parse()` crashes
- Kotlin 2.1.0 handles Java 25 fine
- IntelliJ 2025.1 libs are compiled with Kotlin 2.1.0 metadata — Kotlin 1.9.25 can't read them
- **Conclusion**: Use Kotlin 2.1.0 (matching IntelliJ 2025.1), and sourceCompatibility/targetCompatibility = 17

### ANTLR output directory: don't set `outputDirectory` explicitly
The ANTLR Gradle plugin generates code to `build/generated-src/antlr/main/` with subdirectories matching the source file's path. If you set `outputDirectory` to include the package path, you get doubled nesting:
```
build/generated-src/antlr/main/com/.../parser/com/.../parser/QuintParser.java
```
Just pass `-package com.dearlordylord.quint.idea.parser` in `arguments` and let the plugin handle the rest.

---

## JFlex

### JFlex 1.9 uses `char[]` buffers, IntelliJ expects `CharSequence`
The `FlexLexer` interface requires `reset(CharSequence, int, int, int)`, but JFlex 1.9 generates `zzBuffer` as `char[]`. The `reset()` method must copy the CharSequence to a char array:
```java
char[] chars = new char[len];
for (int i = 0; i < len; i++) {
    chars[i] = buffer.charAt(start + i);
}
zzBuffer = chars;
```
IntelliJ's own plugins use a patched JFlex that supports CharSequence natively, but standard JFlex does not.

### `%public` directive is required
Without `%public`, the generated class is package-private. Cross-package access (e.g., from `QuintSyntaxHighlighter` in the `highlighting` package) fails at compile time.

### `%char` directive needed for position tracking
Without `%char`, the `yychar` variable (character position) isn't available, and `getTokenStart()`/`getTokenEnd()` can't work correctly. This directive adds the `yychar` field that tracks the character offset.

### JFlex constructor takes `Reader`, not no-arg
When constructing the lexer for `FlexAdapter`:
```kotlin
FlexAdapter(QuintLexer(null as java.io.Reader?))
```
Not `QuintLexer()` — there's no no-arg constructor.

### Stub lexer causes SEVERE errors during `buildSearchableOptions`
The `buildSearchableOptions` Gradle task starts an IDE instance and iterates all color settings pages. If the syntax highlighter's `getHighlightingLexer()` throws (e.g., `TODO("stub")`), it produces SEVERE log entries. The build still succeeds, but it's noisy. **Fix**: even in stubs, return a working (if minimal) lexer rather than throwing.

---

## ANTLR4 Grammar (Java target)

### Single-quoted strings in rule arguments become char literals in Java
ANTLR4 inlines parameterized rule arguments directly into generated Java code. The original Quint.g4 uses:
```
simpleId['variant label']
```
This generates `simpleId('variant label')` in Java — a char literal, not a string. Must use double quotes:
```
simpleId["variant label"]
```

### Parameter syntax differs from TypeScript target
TypeScript: `simpleId[context: string]`
Java: `simpleId[String context]`

### `notifyErrorListeners()` works directly in Java actions
No need for `this.` prefix. Just call `notifyErrorListeners("message")`.

---

## Architecture

### Two-lexer design works well
JFlex for highlighting (fast, incremental, simple) + ANTLR4 for parsing (full grammar, PSI tree) is a clean separation. The JFlex lexer has ~180 lines; the ANTLR grammar is the canonical upstream file with minimal edits. No confusion about which lexer does what.

### Vendoring antlr4-intellij-adaptor is necessary
It's not on Maven Central (v0.2.0), JitPack builds fail, and you need to fix deprecations for newer IntelliJ versions. Vendoring ~9 Java files is manageable. Key classes:
- `PSIElementTypeFactory` — the bridge between ANTLR token/rule indices and IntelliJ IElementType
- `ANTLRLexerAdaptor` — wraps ANTLR Lexer as IntelliJ LexerBase
- `ANTLRParserAdaptor` — wraps ANTLR Parser as IntelliJ PsiParser
- `ANTLRParseTreeToPSIConverter` — walks ANTLR parse tree, creates PsiBuilder markers

### Parallel task isolation contract worked perfectly
Pre-registering ALL extension points in plugin.xml during T0, plus strict file ownership rules, meant all three merges had zero conflicts. This pattern is highly recommended for plugin development.

### `QuintTokenTypes` vs ANTLR-generated token types
These are two separate type systems:
- `QuintTokenTypes.*` — hand-written `IElementType` instances, used by JFlex lexer for syntax highlighting
- `PSIElementTypeFactory`-generated types — created from ANTLR token/rule indices, used by ANTLR parser for PSI tree

They are NOT interchangeable. The highlighting pipeline uses JFlex tokens; the parser pipeline uses ANTLR tokens. This is by design.

---

## Testing

### `ParsingTestCase` is the right base for parser tests
Extend it, override `getTestDataPath()` to point at `src/test/testData/parser`, and use `doTest(true)` to compare PSI tree dumps against `.txt` expectation files.

### Lexer tests don't need IDE fixtures
Create the JFlex lexer directly, feed it text, collect token type/text pairs. No `BasePlatformTestCase` needed. Much faster.

### Completion tests can work without a real parser
The `QuintCompletionTest` in T3 tested completion logic (keyword lists, builtin operators) as unit tests without IDE fixtures, avoiding the problem of the parser stub throwing errors.

### ANTLR `-> skip` vs `-> channel(HIDDEN)` — critical for IntelliJ
ANTLR's `-> skip` completely discards tokens — `Lexer.nextToken()` never emits them. IntelliJ's PsiBuilder needs ALL characters accounted for. If whitespace/comments are skipped, PsiBuilder's token text doesn't cover the full file, causing `LazyParseableElement` "Text mismatch in FILE" errors at runtime (during background highlighting).

**Fix**: Use `-> channel(HIDDEN)` instead of `-> skip` for WS, LINE_COMMENT, COMMENT. Hidden-channel tokens are returned by `Lexer.nextToken()` (so IntelliJ sees them) but filtered by `CommonTokenStream` (so ANTLR's parser ignores them).

### DOCCOMMENT must NOT be in PsiBuilder's comment auto-skip set
`DOCCOMMENT` is used in parser rules (`module : DOCCOMMENT* 'module' ...`). If it's in `ParserDefinition.getCommentTokens()`, PsiBuilder auto-skips it. Then the parse tree walk calls `builder.advanceLexer()` for a DOCCOMMENT terminal, but PsiBuilder already skipped past it — causing desynchronization. Only put tokens on `channel(HIDDEN)` into the comment/whitespace TokenSets.

### `LINE_COMMENT` and `DOCCOMMENT`: don't require trailing `\n`
Original rules `'//' .*? '\n'` and `'///' .*? '\n'` fail to match a comment on the last line of a file (no trailing newline). Use `'//' ~[\n]*` and `'///' ~[\n]*` instead — the trailing newline becomes a WS token.

---

## Sandbox IDE Troubleshooting

### Finding the sandbox log
The sandbox IDE logs to a path under the project's `.intellijPlatform/` directory:
```
.intellijPlatform/sandbox/IC-2025.1/log/idea.log
```
Config, system, and plugins directories are siblings under the same `IC-2025.1/` root.

If you can't remember the path, find it from a running sandbox via:
```bash
lsof -c java 2>/dev/null | grep idea.log
```

### `platform-lang-impl` classes are NOT accessible to plugins
Classes in `platform-lang-impl` compile fine (they're on the test classpath) but cause `ClassNotFoundException` at runtime, crashing plugin loading silently. Known impl-only classes:
- `com.intellij.codeInsight.TargetElementEvaluatorEx2`
- `com.intellij.refactoring.rename.PsiElementRenameHandler`

Only use classes from `platform-lang-api` / `platform-refactoring` in plugin production code.

---

## Deferred / Future Work

### `./gradlew runIde` not tested programmatically
The `buildSearchableOptions` task validates plugin loading, but actual editor behavior (highlighting, structure view, folding, completion popups) requires manual testing in the IDE sandbox.

### `verifyPlugin` needs explicit IDE configuration
Running `./gradlew verifyPlugin` fails with "No IDEs Found" unless you configure IDE versions in `intellijPlatform { pluginVerification { ... } }`. Not critical for development, needed for CI/publishing.
