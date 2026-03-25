# Quint IntelliJ IDEA Plugin

## Project Overview
IntelliJ IDEA plugin for the [Quint](https://github.com/informalsystems/quint) specification language. Targets Community Edition, no Node.js/npm dependency.

## Key Files
- `PRD.md` — architecture decisions, alternatives considered, chosen approach
- `TASKS.md` — task breakdown with status, isolation contract, implementation details
- `LEARNINGS.md` — implementation gotchas (read before making changes)
- `src/main/antlr/.../Quint.g4` — canonical grammar (Java target, 4 TS→Java action replacements)
- `src/main/jflex/.../Quint.flex` — JFlex lexer for syntax highlighting
- `src/main/resources/META-INF/plugin.xml` — all extension points

## Architecture
- **Two-lexer design**: JFlex (highlighting, fast, incremental) + ANTLR4 (parsing, PSI tree)
- **Two token type systems**: `QuintTokenTypes.*` (JFlex/highlighting) vs `PSIElementTypeFactory`-generated (ANTLR/parsing). NOT interchangeable.
- **Vendored** `antlr4-intellij-adaptor` at `src/main/java/org/antlr/intellij/adaptor/` (~9 Java files)
- **External annotator**: shells out to `quint typecheck` CLI for diagnostics

## Build & Test
```
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ./gradlew test --no-daemon
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ./gradlew runIde --no-daemon
```
- **JDK 21 required** — Kotlin 2.1.0 crashes on JDK 25. Homebrew JDK 21 at `/opt/homebrew/opt/openjdk@21`. Must pass `JAVA_HOME` explicitly (not registered with macOS `java_home` utility).
- If Gradle daemon cached wrong JDK: `./gradlew --stop` then use `--no-daemon`
- Gradle 9.4, Kotlin 2.1.0, IntelliJ Platform Gradle Plugin 2.12.0
- Platform target: IntelliJ IDEA Community 2025.1
- `instrumentationTools()` does NOT exist in Platform Plugin 2.12.0 — don't add it
- `foojay-resolver-convention` is incompatible with Gradle 9.4 — don't use it

## Package
`com.dearlordylord.quint.idea` — all source under this package.
- Plugin ID: `com.dearlordylord.quint.idea`
- Remote: `git@github.com:dearlordylord/quint-idea.git`

## Sandbox IDE
- Log: `.intellijPlatform/sandbox/IC-2025.1/log/idea.log`
- Config/system/plugins under `.intellijPlatform/sandbox/IC-2025.1/`
- Find log via: `lsof -c java 2>/dev/null | grep idea.log`
- CRITICAL: `TargetElementEvaluatorEx2` and `PsiElementRenameHandler` are in `platform-lang-impl` — NOT accessible to plugins at runtime. Only use `platform-lang-api` / `platform-refactoring` classes.

## References & Navigation
- QuintNamedElement wraps: operDef, typeDef, module, parameter, annotatedParameter, const/var/assume declarations
- QuintQualIdNode wraps qualId with direct `getReference()` — PsiReferenceContributor didn't work in BasePlatformTestCase
- QuintScopeResolver: walk-up algorithm collecting params, lambda params, let-in bindings, module-level declarations
- Forward references work: module-level scope collects ALL declarations regardless of position
- Qualified refs (Foo::bar): split on `::`, find module (same-file then cross-file via imports), search its declarations
- Cross-file imports: QuintImportResolver extracts ImportInfo from importMod PSI nodes, resolves `from "path"` to PsiFile
- Completion uses `parameters.originalPosition` (not `position`) to preserve VFS context for cross-file resolution
- Rename: `setName` on QuintNamedElement replaces qualId via dummy-file parsing; `handleElementRename` preserves qualified prefix
- `renameElementAtCaret` in tests fails on operDef declaration sites (TargetElementUtil can't walk up through normalCallName); place caret at usage site instead

## Critical Rules
- ANTLR grammar: use `-> channel(HIDDEN)` not `-> skip` for WS/comments (see LEARNINGS.md)
- DOCCOMMENT is a parser token — never put it in PsiBuilder's auto-skip comment set
- Don't set ANTLR `outputDirectory` explicitly — causes double nesting
- JFlex `reset()` must convert CharSequence to char[] (JFlex 1.9 incompatibility)
