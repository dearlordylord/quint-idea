# Changelog

## [Unreleased]
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
