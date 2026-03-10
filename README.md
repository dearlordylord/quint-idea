# Quint IntelliJ IDEA Plugin

Language support for [Quint](https://github.com/informalsystems/quint), a specification language for distributed systems and protocols.

Works on **IntelliJ IDEA Community Edition** 2025.1+

## Features

### Syntax Highlighting
- Full lexer covering all Quint tokens: keywords, operators, identifiers, strings, integers, booleans, comments (line `//`, block `/* */`, doc `///`)
- Distinct colors for keywords, types (`Set`, `List`), uppercase identifiers, operators, literals

### Editor
- Brace matching 
- Line comment toggling (`Ctrl+/` / `Cmd+/`)
- Code folding for modules and block expressions

### Parsing & Structure
- Full ANTLR4-based parser matching the official Quint grammar
- Structure view (View > Tool Windows > Structure) showing modules, declarations, and their qualifiers

### Navigation
- **Go-to-definition** (`Cmd+Click` / `Ctrl+Click`) — resolves vals, defs, actions, consts, vars, types, parameters, lambda parameters, let-in bindings
- **Find Usages** - lists all references to a declaration
- Forward references within a module
- Qualified references (`ModuleName::member`)
- Scope-aware: inner bindings shadow outer ones

### Completion
- Keywords, type keywords, builtin operators (with signatures), builtin values (`Nat`, `Int`, `Bool`)
- Scope-aware: locally visible declarations appear in completion

### Diagnostics
- External annotator shells out to `quint typecheck` CLI for type errors
- Errors appear as inline squiggles with messages
- Configure the `quint` binary path in Settings > Tools > Quint
- Works silently when no binary is configured

## Planned

- **Dot-context completion** — after `.`, show only applicable methods instead of all completions
- **Run configurations** — run `quint run`, `quint test`, `quint verify` from the IDE with gutter icons
- **Cross-file navigation** — go-to-definition and find usages across imports
- **Rename refactoring**
- **Type-aware completion** — use type information to rank and filter suggestions
- **Match case bindings** — resolve variant parameters in match expressions
- **Destructuring patterns** — resolve names in `val (a, b) = ...`

## Build

```
./gradlew test          # run tests
./gradlew runIde        # launch IDE sandbox with plugin
./gradlew build         # full build
```

Requires JDK 17+.

## License

Apache-2.0
