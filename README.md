# Quint IntelliJ IDEA Plugin

Language support for [Quint](https://github.com/informalsystems/quint), a specification language for distributed systems and protocols.

Targets **IntelliJ IDEA Community Edition 2025.1**, verified with Plugin Verifier against that release.

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
- Qualified references (`ModuleName::member`), instantiated module aliases and explicit exports
- Match branch parameters with branch-local shadowing
- Imports use the Quint loader spelling: `from "path/name"` appends `.qnt` automatically
- Scope-aware: inner bindings shadow outer ones

### Completion
- Keywords, type keywords, builtin operators (with signatures), builtin values (`Nat`, `Int`, `Bool`)
- Scope-aware: locally visible declarations, qualified instance members and match parameters appear in completion
- Record fields from source annotations, including inline and imported record types
- Record field navigation follows the receiver annotation to its actual defining source

### Diagnostics
- External annotator shells out to `quint typecheck` CLI for type errors
- Errors appear as inline squiggles with messages
- Configure the `quint` binary path in Settings > Tools > Quint
- Checks immutable snapshots of the root and reachable imports, including unsaved documents and nested/parent paths
- Dependency and executable changes invalidate results; obsolete checks cannot replace newer results
- Tools → **Check Current Quint File** requests an immediate check
- Tools → **Quint Check Status** distinguishes checking, current, stale, failed, unavailable, timeout and unchecked
- Background checking can be disabled in Settings; manual checking remains available
- Stale compiler type facts are withheld from completion and hover; source annotations remain available

### Refactoring
- **Rename** (`Shift+F6`) — renames declarations and all their usages, including qualified references

### Code Formatting
- **Reformat Code** (`Cmd+Alt+L` / `Ctrl+Alt+L`) — enforces Quint formatting conventions (spacing around operators, after colons, etc.)

### Execution

- Native **Quint** run configurations for `quint test` and `quint run`, with module, initializer/step and limits (TypeScript backend)
- Gutter entry points for supported test declarations and modules with `init`/`step`
- Standard Run console, exit status and Stop; project disposal terminates owned processes
- Execution uses **saved files**. Save changes before running. This differs from typechecking, which captures unsaved documents.

## Planned

- **Verification configurations** — `quint verify` and its backend workflow
- **Expression types** — chained receivers require coherent compiler source mapping; see [capability investigation](docs/compiler/expression-types.md)
- **Destructuring patterns** — resolve names in `val (a, b) = ...`

## Build

### Prerequisites

- **JDK 21** — required by Kotlin 2.1.0 (JDK 25 causes version-string parse failures; JDK <17 is unsupported by IntelliJ Platform)
- **Gradle 9.4** — included via the wrapper (`./gradlew`), no separate install needed

The build targets a locally installed JDK 21; no automatic toolchain download is configured. To select it, either:

- Set `JAVA_HOME` to your JDK 21 installation, or
- Configure Gradle's [toolchain detection](https://docs.gradle.org/current/userguide/toolchains.html#sec:auto_detection) to find it

On macOS with Homebrew: `export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`

### Commands

```
./gradlew test          # source/editor fixtures; no CLI required
./gradlew verifyPlugin  # binary compatibility against IDEA Community 2025.1
./gradlew runIde        # launch IDE sandbox with plugin
./gradlew build         # full build including buildSearchableOptions
```

### Required real CLI gate

The external test CLI is pinned to Quint **0.32.0** in `ci/quint-test-toolchain.properties`. The plugin build does not require Node.js/npm. Provision the CLI separately, then run:

```
QUINT_TEST_EXECUTABLE=/absolute/path/to/quint ./gradlew realCliTest --no-daemon
QUINT_TEST_EXECUTABLE=/absolute/path/to/quint python3 ci/probe-expression-types.py
```

`realCliTest` fails when the executable is missing, relative, non-executable or has a different version. Source/editor fixtures remain usable without it. The real gate includes import snapshots, unsaved dependencies, diagnostic remapping, native test/run launch and cancellation. CI provisions the pinned CLI and runs the same gate plus plugin verification; Node is used only to install that external CLI.

### Troubleshooting

If you see Kotlin version-string parse errors, your Gradle daemon likely cached a wrong JDK:

```
./gradlew --stop
JAVA_HOME=/path/to/jdk21 ./gradlew test --no-daemon
```

## License

Apache-2.0
