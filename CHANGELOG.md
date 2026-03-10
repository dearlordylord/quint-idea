# Changelog

## [Unreleased]

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
