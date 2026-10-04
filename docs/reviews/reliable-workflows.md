# Delivery audit: reliable Quint workflows

Scope: GitHub specification #1 and tasks #2–#8, baseline `8cc27d6`. TASKS.md remains unchanged at the user's request. Implementation is delivered in [PR #10](https://github.com/dearlordylord/quint-idea/pull/10); issues close on merge. No release is published.

## Acceptance evidence

| Task | Delivered behavior | Direct evidence |
| --- | --- | --- |
| #2 immutable analysis | Root plus reachable unsaved imports; nested, parent and transitive layout; missing/deleted inputs and cycles; independent copied workspaces; dependency/tool/target invalidation; obsolete publication rejection; diagnostic source remapping and cleanup | `QuintTypecheckSchedulingTest` (ordering, unrelated edits, binary replacement, symlink retargeting, overlapping roots and cleanup), `QuintRealCliTest` (real unsaved imports, deletion/cycles, concurrent distinct roots with retargeted imports, real-result publication after an edit) |
| #3 checking workflow | One shared manual/background workflow; seven distinct status labels; immediate manual checks when background work is disabled; unavailable, malformed, empty and timed-out CLI failures; cancellation and project-owned subprocess cleanup; stale inferred hover suppression | `QuintTypecheckSchedulingTest`, `QuintRealCliTest`; registered Check Current File/Status actions and the background-checking setting; `QuintDocumentationProvider` freshness fixture |
| #4 binding identity | Instantiated aliases/wildcards, explicit export surfaces and deep qualification; no implicit re-export or unimported qualification; branch-local match binders; shared completion/navigation/usage/rename identity with undo | `QuintImportScopeTest`, `QuintCrossFileCompletionTest`, `QuintReferenceTest`, `QuintRenameTest`, `QuintCrossFileRefTest`; pinned compiler declaration-surface fixtures and loading-cycle fixture |
| #5 record provenance | Inline source annotations, imported typedef fields, dot/with-string facts, exact defining-field navigation and safe absence for unknown provenance; function values are distinct from returned records | `QuintRecordIntelligenceTest`, `QuintRecordFieldNavigationTest`; source annotation/reference traversal and freshness-guarded compiler facts |
| #6 capability investigation | Conditional go for valid immutable snapshots: coherent parse/typecheck IR identity, application-ID disambiguation, call/nested types, Unicode/CRLF conversion, no mapping on incomplete source | `ci/probe-expression-types.py`, `docs/compiler/expression-type-probe.json`, `docs/compiler/expression-types.md`; separate implementation task [#9](https://github.com/dearlordylord/quint-idea/issues/9) |
| #7 explicit execution | Persistent test/run configuration and selected module/init/step/limits; target/executable validation; saved-file semantics; root working directory, native console/exit status, conservative gutter targets; Stop and project disposal | `QuintRunConfigurationTest`, `QuintRealCliTest` native launch/selection/space-path/Stop/project-disposal fixtures. Fixed TypeScript backend; selectable backends remain deferred |
| #8 reproducible verification | JDK21 CI; pinned external Quint0.32.0; optional source fixtures plus mandatory real-CLI gate; explicit IDEACommunity2025.1 verifier target | `.github/workflows/check.yml`, `ci/quint-test-toolchain.properties`, `validateQuintTestToolchain`, `realCliTest`, `verifyPlugin`; absent CLI gate verified to fail explicitly |

Parent #1's stories are covered by the corresponding rows. Chained-expression assistance remains the separately specified #9, as required by #6 rather than being claimed as shipped. Both lexers/token systems, native PSI, external CLI and lack of Node/npm build dependency remain intact.

## Verification

Local command: `JAVA_HOME=/home/node/.local/jdk21 QUINT_TEST_EXECUTABLE=/usr/local/share/npm-global/bin/quint ./gradlew check realCliTest verifyPlugin --no-daemon`.

Final implementation run: **210 tests, zero failures/errors/skips**. Plugin Verifier1.410: **Compatible with IC-251.23774.435 (2025.1)**. It reports two existing ANTLR-generated `getTokenNames()` deprecations. The expression capability probe passed separately. An additional inferred-hover freshness fixture was added after the local full run and verified separately. [Clean Linux CI](https://github.com/dearlordylord/quint-idea/actions/runs/37212558011) ran the complete final set: **211 tests, zero failures/errors/skips**, with a successful capability probe and IDEA Community 2025.1 compatibility verification. The first clean run exposed case mismatches in the existing parser input filenames; those were corrected before the successful run.

Limits: execution uses saved files; source checking uses captured unsaved documents. Compatibility evidence covers only IDEACommunity2025.1. Imported Unicode/CRLF expression spans require further evidence in #9. No Marketplace publishing or manual visual sandbox inspection is claimed.

## Standards review

No documented lexer, token, grammar, runtime API, JDK or external-CLI architecture breach was found. Review flagged independently published snapshot/type-map data, unused navigation field-list transport, and loss of polling after transient IO errors. These were fixed by one immutable cache entry, provenance-only navigation transport, and rescheduling in `finally`. Follow-up review confirmed the fixes; unused context properties were also removed.

## Specification review

Review flagged unimported same-file qualification, out-of-scope selectable backend, missing real subprocess retarget/concurrency/publication evidence, and missing rename undo evidence. All were fixed, covered by passing editor/real-CLI fixtures, and confirmed in follow-up review.

Findings: Standards—zero hard violations, two heuristic smells plus one reliability concern, all resolved. Spec—four findings, all resolved. No remaining finding in either axis.
