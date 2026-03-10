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

### Refactoring
- **Rename** (`Shift+F6`) — renames declarations and all their usages, including qualified references

### Code Formatting
- **Reformat Code** (`Cmd+Alt+L` / `Ctrl+Alt+L`) — enforces Quint formatting conventions (spacing around operators, after colons, etc.)

## Planned

- **Dot-context completion** — after `.`, show only applicable methods instead of all completions
- **Run configurations** — run `quint run`, `quint test`, `quint verify` from the IDE with gutter icons
- **Cross-file navigation** — go-to-definition and find usages across imports
- **Type-aware completion** — use type information to rank and filter suggestions
- **Match case bindings** — resolve variant parameters in match expressions
- **Destructuring patterns** — resolve names in `val (a, b) = ...`

## Build

### Prerequisites

- **JDK 21** — required by Kotlin 2.1.0 (JDK 25 causes version-string parse failures; JDK <17 is unsupported by IntelliJ Platform)
- **Gradle 9.4** — included via the wrapper (`./gradlew`), no separate install needed

The build uses a [Java toolchain](https://docs.gradle.org/current/userguide/toolchains.html) targeting JDK 21. If Gradle can't find a local JDK 21, it will attempt to auto-download one. To use a specific local JDK, either:

- Set `JAVA_HOME` to your JDK 21 installation, or
- Configure Gradle's [toolchain detection](https://docs.gradle.org/current/userguide/toolchains.html#sec:auto_detection) to find it

On macOS with Homebrew: `export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`

### Commands

```
./gradlew test          # run tests
./gradlew runIde        # launch IDE sandbox with plugin
./gradlew build         # full build including buildSearchableOptions
```

### Troubleshooting

If you see Kotlin version-string parse errors, your Gradle daemon likely cached a wrong JDK:

```
./gradlew --stop
JAVA_HOME=/path/to/jdk21 ./gradlew test --no-daemon
```

## License

Apache-2.0
