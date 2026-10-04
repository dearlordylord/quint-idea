# Changelog

## [Unreleased]
### Added
- Check Current Quint File and checking status actions, with a background-checking toggle
- Native saved-file test/run configurations, conservative gutter entry points, console output and Stop
- Instantiated module members, explicit exports, deep qualified names and match branch binders in source intelligence
- Inline/imported record annotation assistance and field navigation through declaration provenance
- Pinned real Quint CLI fixtures, expression-type capability probe and automated IDEA Community 2025.1 verification

### Fixed
- Typecheck now captures reachable unsaved imports in isolated workspaces instead of mirroring only siblings
- Analysis freshness includes dependencies, import identities, executable changes and request ordering
- Missing executable, malformed/empty output and timeout no longer appear as clean checks
- Stale compiler type facts are withheld; unknown record-field provenance no longer produces guessed navigation
- Import navigation uses the same unconditional `.qnt` suffix rule as the pinned compiler

## [0.5.8]
### Changed
- Refactored language-support internals to centralize Quint vocabulary, PSI shape, name resolution, type lookup, typecheck execution, record-field workflows, and parser token naming without changing user-facing behavior

## [0.5.7]
### Fixed
- Typing lag on large files: ANTLR parser now attempts SLL prediction first and falls back to full LL only on ambiguity, cutting per-keystroke parse time on expression-heavy files so the EDT is no longer blocked waiting for the read lock to release
- Diagnostics becoming stuck (red never appearing after an edit, or stale red never clearing after a fix): the debounced typecheck now schedules a daemon restart after the quiet period, so a pass reliably resumes once you stop typing
- Typecheck cache invalidated by unrelated save / VFS churn: cache is keyed by document content hash instead of `Document.modificationStamp`, which resets on save and used to flicker annotations or pin stale errors
- Daemon "highlighting copy" passes bypassing the cache: resolved the editor's real document via `PsiFile.originalFile` so copy and original share one cache entry
- Hover type info going blank when the file has a typecheck error: the type cache is now replaced only when quint returns type data, preserving the last known-good types across transient errors

### Changed
- Snapshot temp file (`.quint-idea-*.qnt.tmp`) no longer written into the user's source directory. A mirror of the source dir is maintained under `PathManager.getTempPath()` with sibling `.qnt` files hard-linked in, keeping the repo free of transient files and `git status` clean

## [0.5.6]
### Fixed
- Debounced external typechecking while typing so editor diagnostics wait for a short idle period instead of re-running on every keystroke

## [0.5.5]
### Fixed
- Hover documentation now shows type for annotated parameters (e.g. `t: TurnState`)
- Cmd+Click on record fields (e.g. `t.actionsRemaining`) works when receiver is an annotated parameter
- Type hover and field resolution now works for `type` definitions (e.g. `TurnState`, `CreatureState`)

## [0.5.4]
### Added
- Plugin icon (Quint logo) for JetBrains Marketplace and IDE plugin list

## [0.5.3]
### Added
- Auto-close double quotes: typing `"` inserts a pair with caret between; backspace on opening `"` removes both
- Record field completion: typing `t.` on a record-typed value suggests field names with types
- String field completion: typing inside `t.with("...")` suggests record field names
- Cmd+Click on `t.fieldName` and `"fieldName"` in `.with()` navigates to field definition in the type
- Hover documentation for builtin operators (`with`, `fieldNames`, etc.)

## [0.5.2]
### Fixed
- Diagnostics now typecheck the editor buffer instead of the saved file (squiggles update instantly on edit, no save needed)
- Fixed potential temp file collision when multiple annotator runs overlap

## [0.5.1]
### Added
- Auto-detect quint binary from PATH and common install locations (no manual configuration needed)

## [0.5.0]
### Added
- Type information on hover (Quick Documentation): shows inferred types for declarations using `quint typecheck` output
- Supports all Quint types: records, tuples, sum types, operators, type variables, Set/List

## [0.4.2]
### Fixed
- Auto-append `.qnt` extension when resolving `from` paths (e.g., `from "./imports"` now correctly resolves to `imports.qnt`), matching Quint compiler behavior

## [0.4.1]
### Added
- Cross-file reference resolution: Cmd+Click navigates to definitions in imported files
- Wildcard imports (`import A.* from "./file.qnt"`) bring names into unqualified scope
- Specific imports (`import A.foo from "./file.qnt"`) bring single names into scope
- Qualified cross-file refs (`A::foo`) and aliased refs (`B::foo` via `import A as B`)
- Same-file wildcard/specific imports now resolve (`import A.*` within same file)
- Cmd+Click on `"./path.qnt"` in `from` clause navigates to the file
- Cross-file completion: imported names appear in autocomplete suggestions

## [0.3.1]
### Fixed
- Deprecated API warnings flagged by JetBrains Marketplace plugin verification

## [0.3.0]
### Added
- Rename refactoring (Shift+F6): rename declarations (val, def, const, type, parameter) and all usages are updated automatically
- Cross-module rename: renaming a `const` also updates instance parameter bindings (`import M(PARAM = expr).*`)

## [0.2.0]
### Added
- Dot-context completion: after `expr.`, only dot-callable items are shown (builtin operators, user-defined defs, `and`/`or`/`iff`/`implies`). Keywords, type keywords, and builtin values are suppressed in dot context.
- Auto-indent and Reformat Code support: Enter after `{` or `[` indents, Cmd+Alt+L reformats with correct indentation for modules, blocks, records, and lists. Spacing rules for operators, commas, and delimiters.

## [0.1.0]
### Added
- Syntax highlighting (keywords, operators, strings, numbers, comments, doc comments)
- ANTLR4-based parser with full Quint grammar support
- Structure view (modules, declarations with icons)
- Code folding for modules and block expressions
- Brace matching (`{}`, `[]`, `()`) and comment toggling
- Keyword and builtin operator completion with signatures
- Scope-aware completion (parameters, let-in bindings, module-level declarations)
- Go-to-definition (Cmd+Click) for single-file references
- Find Usages for declarations
- Qualified reference resolution (`Module::name`)
- External annotator: diagnostics from `quint typecheck` CLI
- Settings page for `quint` binary path (Tools > Quint)
