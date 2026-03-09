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
./gradlew test          # 55 tests (lexer, parser, annotator, completion)
./gradlew runIde        # launch IDE sandbox with plugin
./gradlew build         # full build including buildSearchableOptions
```
- Gradle 9.4, Kotlin 2.1.0, IntelliJ Platform Gradle Plugin 2.12.0
- Platform target: IntelliJ IDEA Community 2025.1
- sourceCompatibility/targetCompatibility = 17

## Package
`com.dearlordylord.quint.idea` — all source under this package.

## Critical Rules
- ANTLR grammar: use `-> channel(HIDDEN)` not `-> skip` for WS/comments (see LEARNINGS.md)
- DOCCOMMENT is a parser token — never put it in PsiBuilder's auto-skip comment set
- Don't set ANTLR `outputDirectory` explicitly — causes double nesting
- JFlex `reset()` must convert CharSequence to char[] (JFlex 1.9 incompatibility)
