# Expression types in Quint 0.32.0

Decision for #6: **go for a separately scoped implementation on valid immutable snapshots**. Incomplete source has no usable mapping; do not enable chained-expression completion by guessing a receiver's type from a nested identifier.

Reproduce the evidence with:

```
QUINT_TEST_EXECUTABLE=/absolute/path/to/quint python3 ci/probe-expression-types.py
```

The probe checks the pinned CLI version, then runs `parse --source-map` and `typecheck --out` against the same temporary input. Its plain and Unicode/CRLF cases prove identical module IR identities between the two commands. Matching application IDs carry record types for `make(2)` and `make(2).child`, and an integer type for `make(2).child.count`. The incomplete `make(2).` case fails parsing and typechecking. Recorded output is in expression-type-probe.json; the executable probe is the authority.

The source map has `sourceIndex` and `map`; entries are ID to source index/start/end, with inclusive end positions. Typecheck output has `modules` and an ID-keyed `types` map. Typecheck does not accept `--source-map`; a separate parse is required. Two commands must run on the exact same isolated snapshot and toolchain. Compare the complete module IR before joining their output; reject mismatched or incomplete results.

A source span alone is ambiguous: synthetic string arguments of field applications can share the entire receiver/member span with an application and have a different type. The probe filters using the actual IR application's identity. A future implementation must retain node kind and provenance, not pick an arbitrary type with matching offsets.

The Unicode/CRLF root fixture establishes code-point indices/columns in the original root text, including CRLF characters. IntelliJ uses UTF-16 offsets, so convert columns using the current captured line and code points. Do not assume byte offsets or blindly normalize root text. Imported-source loading may normalize line endings; add imported CRLF/Unicode fixtures before supporting expression spans across files. Line/column mapping is the preferred editor bridge.

The separate implementation task is [#9](https://github.com/dearlordylord/quint-idea/issues/9). It must cover bounded two-command execution, cancellation, source ownership, stale result rejection, exact receiver selection and safe behavior while typing. Its implementation is separate from #6 and from the baseline record-annotation work in #5.

Primary references: [pinned CLI commands](https://github.com/quint-co/quint/blob/v0.32.0/quint/src/cli.ts), [pinned source loading](https://github.com/quint-co/quint/blob/v0.32.0/quint/src/parsing/sourceResolver.ts), [Quint 0.32.0 release](https://github.com/quint-co/quint/releases/tag/v0.32.0). The probe verifies the installed release directly rather than assuming the repository's current main matches it.
