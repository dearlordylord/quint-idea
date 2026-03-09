# References

Reference materials for building the Quint IntelliJ IDEA plugin.

## idea/

IntelliJ Platform SDK documentation (25 files). Covers plugin structure, custom language support (lexer, parser, PSI, highlighting, completion, references, refactoring, formatting, inspections, stub indexes), LSP, tool windows, run configurations, testing, publishing.

Source: https://plugins.jetbrains.com/docs/intellij/

## quint/

### Quint.g4
Canonical ANTLR4 grammar for the Quint specification language. ~370 lines.
Source: https://github.com/informalsystems/quint/blob/main/quint/src/generated/Quint.g4

Contains 4 embedded TypeScript actions (error messages + header import) that must be stripped or replaced for Java/Kotlin target.

### Effect.g4
ANTLR4 grammar for Quint's effect system. ~30 lines.
Source: https://github.com/informalsystems/quint/blob/main/quint/src/generated/Effect.g4

## Key external references (not copied)

- Quint VS Code extension: https://github.com/informalsystems/quint/tree/main/vscode/quint-vscode
  - LSP server at `server/src/server.ts`, uses `@informalsystems/quint` compiler directly
  - Features: completion, hover, go-to-def, rename, diagnostics, code actions, document symbols, signature help
  - Local copy at: /Users/firfi/work/typescript/quint-connect/.references/quint/vscode/quint-vscode/

- antlr4-intellij-adaptor: https://github.com/antlr/antlr4-intellij-adaptor
  - v0.2.0 (Dec 2024), NOT on Maven Central (only v0.1/2019 there)
  - JitPack build fails. Must vendor source or build locally.
  - ~17 Java source files, ~50KB total. BSD 2-Clause license.
  - Provides: ANTLRParserAdaptor (PsiParser bridge), ANTLRLexerAdaptor (Lexer bridge), PSIElementTypeFactory, ANTLRParseTreeToPSIConverter

- Quint CLI: https://github.com/informalsystems/quint/releases
  - Standalone binaries via `deno compile` (~100MB per platform)
  - Subcommands: parse, typecheck, compile, repl, run, test, verify, docs
  - `quint parse --out <json> --source-map <json>` and `quint typecheck --out <json>` produce structured JSON with source locations
