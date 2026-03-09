# Quint IntelliJ IDEA Plugin — Product Requirements Document

## Business Goal

Build an IntelliJ IDEA plugin that provides language support for [Quint](https://github.com/informalsystems/quint), a specification language for distributed systems and protocols. Quint files use the `.qnt` extension.

The plugin must work on **IntelliJ IDEA Community Edition** (not only commercial IDEs). Users should not be required to install Node.js, npm, or any JavaScript toolchain. The only acceptable external dependency is the `quint` CLI binary, which is already distributed as a standalone executable on GitHub releases.

Target minimum IDE version: **2025.1+**.

---

## Existing Ecosystem

### Quint VS Code Extension (reference implementation)

- Repository: https://github.com/informalsystems/quint/tree/main/vscode/quint-vscode
- Local copy: `/Users/firfi/work/typescript/quint-connect/.references/quint/vscode/quint-vscode/`
- Architecture: LSP client/server in TypeScript
  - **Client** (`client/src/extension.ts`): thin VS Code LSP client, launches server process
  - **Server** (`server/src/server.ts`): full LSP server, directly imports `@informalsystems/quint` compiler
  - **Worker** (`server/src/worker.ts`): background thread for CPU-heavy parse/analyze via Node.js `worker_threads`
- Published on npm as `@informalsystems/quint-language-server` (v0.19.0)
- Has a `bin` entry (`quint-language-server`) but **no standalone binary release** — requires Node.js runtime
- Features: completion (dot-trigger, type-aware), hover (types + docs + effects), go-to-definition, document symbols (outline), rename, diagnostics (parse + typecheck errors), code actions (quick fixes), signature help
- TextMate grammar (`syntaxes/quint.tmLanguage.json`) for syntax highlighting
- The server embeds the Quint compiler's 4-phase parse pipeline: `parsePhase1fromText` → `parsePhase2sourceResolution` → `parsePhase3importAndNameResolution` → `parsePhase4toposort`

### Quint CLI Binary

- Repository: https://github.com/informalsystems/quint
- Releases: https://github.com/informalsystems/quint/releases
- **Standalone binaries exist** — built via `deno compile npm:@informalsystems/quint@VERSION`
- Platforms: linux-amd64, linux-arm64, macos-amd64, macos-arm64, windows-amd64 (~100MB each, Deno runtime bundled)
- Subcommands: `parse`, `typecheck`, `compile`, `repl`, `run`, `test`, `verify`, `docs`
- **No LSP subcommand** — the CLI has no language server mode
- Structured JSON output available:
  - `quint parse <file> --out <json> --source-map <json>` → full IR + source map with line/col positions
  - `quint typecheck <file> --out <json>` → IR + per-expression types + effects + errors with source locations
  - Error format: `{ "explanation": "...", "locs": [{ "source": "...", "start": {"line": N, "col": N}, "end": {"line": N, "col": N} }] }`

### Quint ANTLR4 Grammar

- Location: `quint/src/generated/Quint.g4` (canonical, ~370 lines)
- Copied to: `.references/quint/Quint.g4`
- ANTLR4 grammar with 4 embedded TypeScript actions (error messages + 1 header import) — must be stripped for Java/Kotlin target
- Parameterized rules (`simpleId[context: string]`) — supported in ANTLR4 Java target
- Also: `Effect.g4` (~30 lines) for the effect type system

### Other Editor Plugins

- Existing plugins for Emacs, Vim, highlight.js in `editor-plugins/` directory of the quint repo
- No existing IntelliJ/JetBrains plugin

---

## Alternatives Considered

### A1: LSP-Only Plugin (NOT VIABLE)

**Approach**: Reuse the existing Quint LSP server (`@informalsystems/quint-language-server`) from IntelliJ's built-in LSP support.

**Why considered**: Minimal code (~3 Kotlin files), immediate feature parity with VS Code.

**Why rejected**:
1. **Community Edition not supported** — IntelliJ's LSP support (`com.intellij.modules.lsp`) requires commercial IDEs only. This is a hard requirement violation.
2. **No standalone LSP binary** — The language server is a Node.js package. No binary is published on GitHub releases (only the CLI gets `deno compile`'d). Users would need Node.js/npm installed, violating the "no npm" constraint.
3. **Bundling impractical** — Bundling Node.js + server code in the plugin adds ~50-80MB. Alternatively, `deno compile`-ing the server ourselves is fragile: the server uses `worker_threads` which has uncertain Deno compatibility.
4. **Upstream cooperation unavailable** — Requesting informalsystems to publish a standalone LSP server binary is not an option.

### A2: LSP as Optional Tier for Commercial IDEs (VIABLE but DEFERRED)

**Approach**: Register LSP support as an optional dependency (`<depends optional="true">com.intellij.modules.lsp</depends>`) that activates only in commercial IDEs, on top of a native implementation.

**Why considered**: Gives commercial IDE users richer features from the real Quint compiler.

**Status**: Viable in principle but deferred. The LSP binary distribution problem (no standalone binary, can't require npm) remains unsolved. Could revisit if upstream starts publishing server binaries. Not in initial scope.

### A3: Full Native PSI with Grammar-Kit BNF (VIABLE, NOT CHOSEN)

**Approach**: Translate the ANTLR4 `Quint.g4` grammar to Grammar-Kit BNF format. Grammar-Kit auto-generates parser + PSI interfaces + implementation classes.

**Pros**:
- JetBrains' officially recommended tool
- Generates type-safe PSI classes
- Deep integration with stub indexes, find usages, etc.
- Well-documented in IntelliJ SDK docs

**Cons**:
- Manual translation of ~370-line ANTLR4 grammar to a different format
- Grammar drift risk: when upstream updates `Quint.g4`, we must manually re-translate
- Quint grammar is moderately complex (operator precedence, multiple expression alternatives, embedded error recovery)

**Why not chosen**: The grammar drift maintenance burden is significant. The ANTLR4 grammar IS the canonical source of truth and is actively maintained.

### A4: JFlex Lexer Only + CLI for Everything Else (VIABLE, NOT CHOSEN)

**Approach**: Only implement tokenization (JFlex lexer). For all intelligence features, shell out to `quint parse`/`quint typecheck` CLI.

**Pros**: Minimal code, no parser to maintain, leverages real Quint compiler.

**Cons**:
- No PSI tree → no native navigation, refactoring, structure view without CLI calls
- Process spawn per operation → slow, poor UX for real-time features
- No offline capability without `quint` binary
- Can't provide completion or references without a parse tree

**Why not chosen**: Too limited for a production IDE plugin. Users expect sub-second responsiveness for highlighting and structure.

### A5: ANTLR4 with `antlr4-intellij-adaptor` (CHOSEN)

**Approach**: Use the canonical `Quint.g4` grammar with ANTLR4 Java target. Bridge to IntelliJ's PSI system via the `antlr4-intellij-adaptor` library (vendored).

**Pros**:
- Direct reuse of the canonical grammar — when upstream updates `Quint.g4`, we copy the new file and strip the same 4 TS actions
- ANTLR4 Java target generates a proper parser with error recovery
- The adaptor library bridges ANTLR4 parse trees to IntelliJ PSI trees
- Works on Community Edition
- No runtime dependencies beyond the JVM

**Cons**:
- PSI classes are generic (from adaptor's `ANTLRParseTreeToPSIConverter`) rather than generated typed interfaces like Grammar-Kit
- Intelligence features (references, find usages) require manual PSI tree navigation utilities
- The adaptor library must be vendored (see below)

**Compatibility assessment**:
- `antlr4-intellij-adaptor` v0.2.0 (Dec 2024) — uses stable IntelliJ APIs (`PsiParser`, `PsiBuilder`, `LexerBase`, `IElementType`)
- **NOT on Maven Central** (only v0.1/2019 there). JitPack build fails (requires IntelliJ SDK download)
- **Must vendor** the source (~17 Java files, ~50KB, BSD 2-Clause license). This is standard practice — the ANTLR4 IntelliJ Plugin itself does this.
- Vendoring allows: fixing any 2025.1 deprecations, stripping unused modules (`xpath/`, `SymtabUtils`), optional Kotlin conversion
- ANTLR4 runtime 4.13 IS on Maven Central — no distribution issues

---

## Chosen Architecture

### Two-component design

```
Component 1: Native Parser (in-process, fast)
├── JFlex lexer (tokens only, for syntax highlighting — required by IntelliJ's incremental re-lexing)
├── ANTLR4 parser (from Quint.g4, via antlr4-intellij-adaptor → PSI tree)
└── Provides: highlighting, structure view, folding, brace matching, commenter,
    basic completion (keywords + builtins), navigation within file

Component 2: External Annotator (out-of-process, for deep analysis)
├── Calls `quint typecheck` CLI binary
├── Parses JSON output for errors with source locations
└── Provides: diagnostics (parse errors, type errors), type information
```

### Why JFlex + ANTLR4 (two lexers)?

IntelliJ's syntax highlighting pipeline requires incremental re-lexing: the ability to restart lexing from any position in the file using a single integer state. JFlex supports this natively. ANTLR4's lexer doesn't expose state as a single integer cleanly (the `ANTLRLexerAdaptor` works around this but JFlex is more reliable for highlighting). The JFlex lexer is simple (~100 lines, just tokens, derived mechanically from `Quint.g4` token rules). The ANTLR4 parser handles the full grammar for PSI tree construction.

### External dependency: `quint` CLI binary

- Required for: diagnostics (type errors), run configurations
- NOT required for: syntax highlighting, structure view, basic navigation, completion
- Distribution: user-configured path in plugin settings (with potential auto-download from GitHub releases as future enhancement)
- The `quint` binary is ~100MB, standalone (Deno-compiled), available for all major platforms

### Plugin language

**Kotlin.** Recommended by JetBrains, first-class support, modern APIs assume it. All reference docs and templates use Kotlin.

Alternative considered: Java (fully supported but no advantage over Kotlin for new code).

---

## Project Structure

```
quint-idea/
├── build.gradle.kts                           # IntelliJ Platform Gradle Plugin 2.x + ANTLR4
├── settings.gradle.kts
├── src/main/
│   ├── resources/
│   │   ├── META-INF/plugin.xml
│   │   └── icons/quint.svg
│   │
│   ├── antlr/                                 # ANTLR4 grammar (cleaned from TS actions)
│   │   └── Quint.g4
│   │
│   └── kotlin/com/github/quint/idea/
│       │
│       ├── QuintLanguage.kt                   # Language singleton
│       ├── QuintFileType.kt                   # .qnt file type registration
│       ├── QuintIcons.kt                      # Icon holder
│       │
│       ├── lexer/
│       │   ├── Quint.flex                     # JFlex grammar (tokens only, for highlighting)
│       │   └── QuintTokenTypes.kt             # IElementType constants + TokenSets
│       │
│       ├── parser/
│       │   ├── QuintParserDefinition.kt       # Bridges ANTLR4 parser → IntelliJ PsiParser
│       │   └── QuintPsiFactory.kt             # PSI element creation
│       │
│       ├── psi/
│       │   └── QuintFile.kt                   # PsiFile implementation
│       │
│       ├── highlighting/
│       │   ├── QuintSyntaxHighlighter.kt      # Lexer-based token → TextAttributesKey mapping
│       │   ├── QuintSyntaxHighlighterFactory.kt
│       │   └── QuintColorSettingsPage.kt      # User-configurable colors
│       │
│       ├── editor/
│       │   ├── QuintBraceMatcher.kt           # {}, [], () matching
│       │   ├── QuintCommenter.kt              # // and /* */ toggle
│       │   └── QuintFoldingBuilder.kt         # Fold modules, block expressions
│       │
│       ├── structure/
│       │   └── QuintStructureViewFactory.kt   # Outline: modules, defs, vals, vars, actions
│       │
│       ├── completion/
│       │   └── QuintCompletionContributor.kt  # Keywords + builtin operators
│       │
│       ├── annotator/
│       │   └── QuintExternalAnnotator.kt      # Calls `quint typecheck`, parses JSON errors
│       │
│       ├── settings/
│       │   └── QuintSettingsConfigurable.kt   # quint binary path configuration
│       │
│       ├── run/
│       │   ├── QuintRunConfigurationType.kt   # run/test/verify configurations
│       │   └── QuintRunConfiguration.kt
│       │
│       └── vendor/                            # Vendored antlr4-intellij-adaptor (BSD 2-Clause)
│           └── org/antlr/intellij/adaptor/
│               ├── lexer/
│               │   ├── ANTLRLexerAdaptor.java
│               │   ├── ANTLRLexerState.java
│               │   ├── CharSequenceCharStream.java
│               │   ├── PSIElementTypeFactory.java
│               │   ├── PSITokenSource.java
│               │   ├── RuleIElementType.java
│               │   └── TokenIElementType.java
│               ├── parser/
│               │   ├── ANTLRParserAdaptor.java
│               │   ├── ANTLRParseTreeToPSIConverter.java
│               │   ├── ErrorStrategyAdaptor.java
│               │   ├── SyntaxError.java
│               │   └── SyntaxErrorListener.java
│               └── psi/
│                   └── Trees.java
│
├── src/test/
│   ├── testData/                              # .qnt fixture files
│   │   ├── highlighting/
│   │   ├── parser/
│   │   └── completion/
│   └── kotlin/com/github/quint/idea/
│       ├── QuintLexerTest.kt
│       ├── QuintParserTest.kt
│       └── QuintHighlightingTest.kt
│
├── .references/                               # Reference materials (not shipped)
│   ├── README.md                              # Index of all references
│   ├── idea/                                  # IntelliJ SDK docs (25 files)
│   └── quint/                                 # Quint.g4, Effect.g4
│
└── PRD.md                                     # This document
```

---

## Implementation Phases

### Phase 1 — Syntax Highlighting + Editor Basics

Deliverables:
- Gradle project scaffold with IntelliJ Platform Gradle Plugin 2.x
- `QuintLanguage`, `QuintFileType`, `QuintIcons` — register `.qnt` files
- JFlex lexer — tokenize all Quint tokens (keywords, operators, types, strings, numbers, comments, identifiers)
- `QuintSyntaxHighlighter` + `QuintSyntaxHighlighterFactory` — map tokens to color attributes
- `QuintBraceMatcher` — `{}`, `[]`, `()` matching
- `QuintCommenter` — toggle `//` and `/* */`
- `QuintColorSettingsPage` — user-configurable color scheme

User value: `.qnt` files recognized by IDE, syntax-colored, brace-matched, comment toggling works. Immediately useful.

### Phase 2 — Parser + PSI + Structure

Deliverables:
- Vendor `antlr4-intellij-adaptor` source (stripped to essential files)
- Clean `Quint.g4` (strip 4 TS actions, replace with Java equivalents for error messages)
- ANTLR4 Gradle integration — generate parser from `.g4` at build time
- `QuintParserDefinition` — bridge ANTLR4 parser to IntelliJ PSI
- `QuintFile` PSI file implementation
- `QuintFoldingBuilder` — fold modules, block expressions
- `QuintStructureViewFactory` — outline view showing modules, defs, vals, vars, actions, types

User value: structure view (outline), code folding, breadcrumbs. Parser errors shown inline.

### Phase 3 — Diagnostics via External Annotator

Deliverables:
- `QuintSettingsConfigurable` — settings page for `quint` binary path
- `QuintExternalAnnotator` — on file save/change, run `quint typecheck --out <tempfile>`, parse JSON, map errors to editor annotations
- Error/warning highlighting with messages from Quint's type checker

User value: real type errors and parse errors from the actual Quint compiler shown inline in the editor. This is the most impactful feature for daily use.

### Phase 4 — Completion + Basic Navigation

Deliverables:
- `QuintCompletionContributor` — keyword completion (all Quint keywords + qualifiers), builtin operator completion (set operators, list operators, etc.)
- Basic go-to-definition within a single file using PSI tree navigation
- `FindUsagesProvider` for in-file symbol usage

User value: autocomplete for keywords and builtins, navigate to definitions within a file.

### Phase 5 — Run Configurations

Deliverables:
- `QuintRunConfigurationType` + `QuintRunConfiguration` — run configurations for `quint run`, `quint test`, `quint verify`
- Settings editor for: main module, init/step actions, invariants, backend selection (typescript/rust)
- Gutter run icons for test/run definitions (if feasible from PSI)

User value: run/test/verify Quint specs directly from the IDE with output in the run tool window.

---

## Quint Language Quick Reference (for implementers)

### Keywords
`module`, `import`, `export`, `from`, `as`, `const`, `var`, `assume`, `type`, `val`, `def`, `pure`, `action`, `run`, `temporal`, `nondet`, `if`, `else`, `match`, `and`, `or`, `all`, `any`, `iff`, `implies`, `Set`, `List`

### Operators
`+`, `-`, `*`, `/`, `%`, `^`, `>`, `<`, `>=`, `<=`, `!=`, `==`, `=` (assignment), `=>` (lambda/type), `->` (function type/pair), `::` (qualified name), `'` (primed variable), `...` (record spread)

### Types
`int`, `str`, `bool`, `Set[T]`, `List[T]`, tuples `(T1, T2)`, records `{ field: T }`, function types `T1 -> T2`, operator types `(T1) => T2`, sum types (`| Variant1(T) | Variant2`)

### Comments
`//` line comment, `/* */` block comment, `///` doc comment

### Literals
Strings (`"..."`), booleans (`true`, `false`), integers (decimal, hex `0x...`, underscore separators)

### File structure
```
module ModuleName {
  const MY_CONST: int
  var myVar: Set[int]

  pure def add(x: int, y: int): int = x + y

  action init = myVar' = Set(1, 2, 3)
  action step = myVar' = myVar.union(Set(4))

  val invariant = myVar.size() < 10

  temporal property = always(invariant)
}
```

---

## Dependencies

| Dependency | Version | Source | Purpose |
|---|---|---|---|
| IntelliJ Platform SDK | 2025.1+ | Gradle Plugin 2.x | IDE platform |
| ANTLR4 Runtime | 4.13.x | Maven Central | Parser runtime |
| antlr4-intellij-adaptor | 0.2.0 | Vendored source (BSD 2-Clause) | ANTLR4 → PSI bridge |
| ANTLR4 Tool | 4.13.x | Gradle ANTLR plugin | Code generation at build time |
| `quint` CLI | 0.31+ | User-installed binary | Diagnostics, run configs |

---

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| `Quint.g4` grammar changes upstream | Parser breaks | Grammar is mature, changes are infrequent. Copy + strip is mechanical. Monitor upstream releases. |
| `antlr4-intellij-adaptor` uses deprecated IntelliJ APIs in future | Build breaks | Vendored source — we control and fix. APIs used (`PsiParser`, `PsiBuilder`) are foundational and stable. |
| `quint typecheck` JSON output format changes | Diagnostics break | Pin to minimum quint version in docs. JSON format has been stable. |
| ANTLR4 PSI tree is less type-safe than Grammar-Kit | More manual work for intelligence features | Accept the tradeoff — grammar sync benefit outweighs. Can add typed wrapper utilities as needed. |
| `quint` binary is ~100MB, users may not have it | External annotator + run configs non-functional | Plugin works without it (highlighting, structure, completion still work). Show helpful message when binary not configured. |
