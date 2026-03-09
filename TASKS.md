# Quint IntelliJ Plugin — Task Breakdown

Read PRD.md first. This document defines executable tasks for building the plugin.

Package: `com.dearlordylord.quint.idea`
Plugin ID: `com.dearlordylord.quint.idea`

---

## Execution Order

```
T0 (scaffold) ──sequential──▶ git commit
                                  │
                    ┌──────────────┼──────────────┐
                    ▼              ▼              ▼
                T1 (worktree)  T2 (worktree)  T3 (worktree)
                Lexer+HL+Edit  Parser+PSI     Annotator+Compl
                    │              │              │
                    └──────────────┼──────────────┘
                                  ▼
                          T4 (merge + verify)
```

T0 must complete and be committed before T1-T3 start.
T1, T2, T3 run in parallel in separate worktrees. No shared file modifications.
T4 merges all branches and runs integration tests.

---

## Isolation Contract

### Files ONLY T0 creates (never modified by T1-T3):
- `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`
- `src/main/resources/META-INF/plugin.xml`
- `src/main/resources/icons/quint.svg`
- `src/main/kotlin/.../QuintLanguage.kt`
- `src/main/kotlin/.../QuintFileType.kt`
- `src/main/kotlin/.../QuintIcons.kt`
- `src/main/kotlin/.../QuintTokenTypes.kt`
- `src/main/antlr/com/dearlordylord/quint/idea/parser/Quint.g4`

### File ownership per parallel task:
- **T1**: `lexer/Quint.flex`, `highlighting/*`, `editor/QuintBraceMatcher.kt`, `editor/QuintCommenter.kt`, `src/test/**/lexer/*`, `src/test/**/highlighting/*`, `src/test/testData/lexer/*`, `src/test/testData/highlighting/*`
- **T2**: `parser/*`, `psi/*`, `structure/*`, `editor/QuintFoldingBuilder.kt`, `vendor/**`, `src/test/**/parser/*`, `src/test/**/structure/*`, `src/test/testData/parser/*`
- **T3**: `annotator/*`, `settings/*`, `completion/*`, `src/test/**/annotator/*`, `src/test/**/completion/*`, `src/test/testData/completion/*`

### Merge conflict prevention:
- plugin.xml: T0 pre-registers ALL extension points. T1-T3 never touch it.
- build.gradle.kts: T0 includes ALL dependencies. T1-T3 never touch it.
- QuintTokenTypes.kt: T0 defines ALL token types. T1-T3 import but never modify.
- Each parallel task replaces stub files with real implementations. Stubs are placeholders that compile but throw `NotImplementedError` or return empty/minimal values.

---

## T0: Scaffold (Sequential, Blocking)

### Goal
Create a compilable project skeleton. Every class referenced in `plugin.xml` exists as a stub. `./gradlew build` succeeds (tests may be empty).

### Prerequisites
- `git init` the project
- Copy `quint.svg` icon from `.references` (VS Code extension has SVGs at `icons/logo-dark.svg`)

### Files to Create

#### `settings.gradle.kts`
```kotlin
rootProject.name = "quint-idea"
```

#### `gradle.properties`
```
pluginGroup = com.dearlordylord.quint.idea
pluginName = Quint
pluginVersion = 0.1.0
platformType = IC
platformVersion = 2025.1
```

#### `build.gradle.kts`
Key dependencies and plugins:
```kotlin
plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij.platform") version "2.12.0"
    id("antlr")
    id("org.xbib.gradle.plugin.jflex") version "3.0.2"
}

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2025.1")
        instrumentationTools()
        testFramework(TestFrameworkType.Platform)
    }
    antlr("org.antlr:antlr4:4.13.2")
    implementation("org.antlr:antlr4-runtime:4.13.2")
}
```
Notes:
- Use `intellijIdeaCommunity` (not `intellijIdea`) — we target Community.
- ANTLR4 Gradle plugin generates parser from `src/main/antlr/**/*.g4`.
- JFlex plugin generates lexer from `src/main/jflex/**/*.flex` (or configure source dir).
- The vendored `antlr4-intellij-adaptor` Java source lives under `src/main/java/` and compiles alongside Kotlin.

#### `src/main/resources/META-INF/plugin.xml`
Pre-register ALL extension points:
```xml
<idea-plugin>
    <id>com.dearlordylord.quint.idea</id>
    <name>Quint</name>
    <vendor>dearlordylord</vendor>
    <description>Language support for Quint specifications</description>

    <depends>com.intellij.modules.platform</depends>

    <extensions defaultExtensionNs="com.intellij">
        <!-- File type -->
        <fileType
            name="Quint"
            implementationClass="com.dearlordylord.quint.idea.QuintFileType"
            fieldName="INSTANCE"
            language="Quint"
            extensions="qnt"/>

        <!-- Parser -->
        <lang.parserDefinition
            language="Quint"
            implementationClass="com.dearlordylord.quint.idea.parser.QuintParserDefinition"/>

        <!-- Syntax highlighting -->
        <lang.syntaxHighlighterFactory
            language="Quint"
            implementationClass="com.dearlordylord.quint.idea.highlighting.QuintSyntaxHighlighterFactory"/>
        <colorSettingsPage
            implementation="com.dearlordylord.quint.idea.highlighting.QuintColorSettingsPage"/>

        <!-- Editor -->
        <lang.braceMatcher
            language="Quint"
            implementationClass="com.dearlordylord.quint.idea.editor.QuintBraceMatcher"/>
        <lang.commenter
            language="Quint"
            implementationClass="com.dearlordylord.quint.idea.editor.QuintCommenter"/>
        <lang.foldingBuilder
            language="Quint"
            implementationClass="com.dearlordylord.quint.idea.editor.QuintFoldingBuilder"/>

        <!-- Structure -->
        <lang.psiStructureViewFactory
            language="Quint"
            implementationClass="com.dearlordylord.quint.idea.structure.QuintStructureViewFactory"/>

        <!-- Completion -->
        <completion.contributor
            language="Quint"
            implementationClass="com.dearlordylord.quint.idea.completion.QuintCompletionContributor"/>

        <!-- External annotator (diagnostics) -->
        <externalAnnotator
            language="Quint"
            implementationClass="com.dearlordylord.quint.idea.annotator.QuintExternalAnnotator"/>

        <!-- Settings -->
        <applicationConfigurable
            parentId="tools"
            instance="com.dearlordylord.quint.idea.settings.QuintSettingsConfigurable"
            id="com.dearlordylord.quint.idea.settings.QuintSettingsConfigurable"
            displayName="Quint"/>
        <applicationService
            serviceImplementation="com.dearlordylord.quint.idea.settings.QuintSettingsState"/>
    </extensions>
</idea-plugin>
```

#### `src/main/antlr/com/dearlordylord/quint/idea/parser/Quint.g4`
Copy from `.references/quint/Quint.g4` with these modifications:
1. Remove `@header { ... }` block (TS import)
2. Replace 4 embedded TS actions with Java equivalents:
   - `wrongTypeApplication` action → `notifyErrorListeners("QNT009: Use square brackets...")`
   - `errorEq` action → `notifyErrorListeners("QNT006: unexpected '=', did you mean '=='?")`
   - `simpleId` qualified error → `notifyErrorListeners("QNT008: Identifiers in a " + $context + " cannot be qualified...")`
   - `identifier` reserved keyword error → `notifyErrorListeners("QNT008: Reserved keyword '" + $reserved.text + "' cannot be used...")`

#### Shared Kotlin Files

All under `src/main/kotlin/com/dearlordylord/quint/idea/`:

**`QuintLanguage.kt`**
```kotlin
class QuintLanguage private constructor() : Language("Quint") {
    companion object {
        @JvmField val INSTANCE = QuintLanguage()
    }
}
```

**`QuintFileType.kt`**
```kotlin
class QuintFileType private constructor() : LanguageFileType(QuintLanguage.INSTANCE) {
    override fun getName() = "Quint"
    override fun getDescription() = "Quint specification file"
    override fun getDefaultExtension() = "qnt"
    override fun getIcon() = QuintIcons.FILE
    companion object {
        @JvmField val INSTANCE = QuintFileType()
    }
}
```

**`QuintIcons.kt`**
```kotlin
object QuintIcons {
    @JvmField val FILE = IconLoader.getIcon("/icons/quint.svg", QuintIcons::class.java)
}
```

**`QuintTokenTypes.kt`**
Define ALL token IElementTypes matching Quint.g4 token rules:
```kotlin
object QuintTokenTypes {
    @JvmField val COMMENT = IElementType("COMMENT", QuintLanguage.INSTANCE)
    @JvmField val LINE_COMMENT = IElementType("LINE_COMMENT", QuintLanguage.INSTANCE)
    @JvmField val DOCCOMMENT = IElementType("DOCCOMMENT", QuintLanguage.INSTANCE)
    @JvmField val STRING = IElementType("STRING", QuintLanguage.INSTANCE)
    @JvmField val INT = IElementType("INT", QuintLanguage.INSTANCE)
    @JvmField val BOOL = IElementType("BOOL", QuintLanguage.INSTANCE)

    // Keywords
    @JvmField val MODULE = IElementType("module", QuintLanguage.INSTANCE)
    @JvmField val IMPORT = IElementType("import", QuintLanguage.INSTANCE)
    @JvmField val EXPORT = IElementType("export", QuintLanguage.INSTANCE)
    @JvmField val FROM = IElementType("from", QuintLanguage.INSTANCE)
    @JvmField val AS = IElementType("as", QuintLanguage.INSTANCE)
    @JvmField val CONST = IElementType("const", QuintLanguage.INSTANCE)
    @JvmField val VAR = IElementType("var", QuintLanguage.INSTANCE)
    @JvmField val ASSUME = IElementType("assume", QuintLanguage.INSTANCE)
    @JvmField val TYPE = IElementType("type", QuintLanguage.INSTANCE)
    @JvmField val VAL = IElementType("val", QuintLanguage.INSTANCE)
    @JvmField val DEF = IElementType("def", QuintLanguage.INSTANCE)
    @JvmField val PURE = IElementType("pure", QuintLanguage.INSTANCE)
    @JvmField val ACTION = IElementType("action", QuintLanguage.INSTANCE)
    @JvmField val RUN = IElementType("run", QuintLanguage.INSTANCE)
    @JvmField val TEMPORAL = IElementType("temporal", QuintLanguage.INSTANCE)
    @JvmField val NONDET = IElementType("nondet", QuintLanguage.INSTANCE)
    @JvmField val IF = IElementType("if", QuintLanguage.INSTANCE)
    @JvmField val ELSE = IElementType("else", QuintLanguage.INSTANCE)
    @JvmField val MATCH = IElementType("match", QuintLanguage.INSTANCE)
    @JvmField val AND = IElementType("and", QuintLanguage.INSTANCE)
    @JvmField val OR = IElementType("or", QuintLanguage.INSTANCE)
    @JvmField val ALL = IElementType("all", QuintLanguage.INSTANCE)
    @JvmField val ANY = IElementType("any", QuintLanguage.INSTANCE)
    @JvmField val IFF = IElementType("iff", QuintLanguage.INSTANCE)
    @JvmField val IMPLIES = IElementType("implies", QuintLanguage.INSTANCE)
    @JvmField val SET = IElementType("Set", QuintLanguage.INSTANCE)
    @JvmField val LIST = IElementType("List", QuintLanguage.INSTANCE)

    // Operators
    @JvmField val PLUS = IElementType("+", QuintLanguage.INSTANCE)
    @JvmField val MINUS = IElementType("-", QuintLanguage.INSTANCE)
    @JvmField val MUL = IElementType("*", QuintLanguage.INSTANCE)
    @JvmField val DIV = IElementType("/", QuintLanguage.INSTANCE)
    @JvmField val MOD = IElementType("%", QuintLanguage.INSTANCE)
    @JvmField val POW = IElementType("^", QuintLanguage.INSTANCE)
    @JvmField val GT = IElementType(">", QuintLanguage.INSTANCE)
    @JvmField val LT = IElementType("<", QuintLanguage.INSTANCE)
    @JvmField val GE = IElementType(">=", QuintLanguage.INSTANCE)
    @JvmField val LE = IElementType("<=", QuintLanguage.INSTANCE)
    @JvmField val NE = IElementType("!=", QuintLanguage.INSTANCE)
    @JvmField val EQ = IElementType("==", QuintLanguage.INSTANCE)
    @JvmField val ASGN = IElementType("=", QuintLanguage.INSTANCE)
    @JvmField val ARROW = IElementType("->", QuintLanguage.INSTANCE)
    @JvmField val FAT_ARROW = IElementType("=>", QuintLanguage.INSTANCE)
    @JvmField val LPAREN = IElementType("(", QuintLanguage.INSTANCE)
    @JvmField val RPAREN = IElementType(")", QuintLanguage.INSTANCE)
    @JvmField val LBRACE = IElementType("{", QuintLanguage.INSTANCE)
    @JvmField val RBRACE = IElementType("}", QuintLanguage.INSTANCE)
    @JvmField val LBRACKET = IElementType("[", QuintLanguage.INSTANCE)
    @JvmField val RBRACKET = IElementType("]", QuintLanguage.INSTANCE)
    @JvmField val COMMA = IElementType(",", QuintLanguage.INSTANCE)
    @JvmField val COLON = IElementType(":", QuintLanguage.INSTANCE)
    @JvmField val DOT = IElementType(".", QuintLanguage.INSTANCE)
    @JvmField val SEMICOLON = IElementType(";", QuintLanguage.INSTANCE)
    @JvmField val PIPE = IElementType("|", QuintLanguage.INSTANCE)
    @JvmField val PRIME = IElementType("'", QuintLanguage.INSTANCE)
    @JvmField val COLONCOLON = IElementType("::", QuintLanguage.INSTANCE)
    @JvmField val SPREAD = IElementType("...", QuintLanguage.INSTANCE)
    @JvmField val UNDERSCORE = IElementType("_", QuintLanguage.INSTANCE)
    @JvmField val HASHBANG = IElementType("HASHBANG_LINE", QuintLanguage.INSTANCE)

    // Identifiers
    @JvmField val LOW_ID = IElementType("LOW_ID", QuintLanguage.INSTANCE)
    @JvmField val CAP_ID = IElementType("CAP_ID", QuintLanguage.INSTANCE)

    // Special
    @JvmField val BAD_CHARACTER = IElementType("BAD_CHARACTER", QuintLanguage.INSTANCE)

    // Token sets
    @JvmField val KEYWORDS = TokenSet.create(
        MODULE, IMPORT, EXPORT, FROM, AS, CONST, VAR, ASSUME, TYPE,
        VAL, DEF, PURE, ACTION, RUN, TEMPORAL, NONDET,
        IF, ELSE, MATCH, AND, OR, ALL, ANY, IFF, IMPLIES, SET, LIST
    )
    @JvmField val COMMENTS = TokenSet.create(COMMENT, LINE_COMMENT, DOCCOMMENT)
    @JvmField val STRINGS = TokenSet.create(STRING)
    @JvmField val BRACES = TokenSet.create(LBRACE, RBRACE)
    @JvmField val BRACKETS = TokenSet.create(LBRACKET, RBRACKET)
    @JvmField val PARENS = TokenSet.create(LPAREN, RPAREN)
    @JvmField val OPERATORS = TokenSet.create(
        PLUS, MINUS, MUL, DIV, MOD, POW, GT, LT, GE, LE, NE, EQ,
        ASGN, ARROW, FAT_ARROW
    )
    @JvmField val TYPE_KEYWORDS = TokenSet.create(SET, LIST)
    @JvmField val QUALIFIERS = TokenSet.create(VAL, DEF, PURE, ACTION, RUN, TEMPORAL, NONDET)
}
```

#### Stub Files

All stubs must compile. Minimal pattern:

**`lexer/Quint.flex`** — empty/minimal flex spec that returns BAD_CHARACTER for everything. T1 replaces.

**`highlighting/QuintSyntaxHighlighter.kt`**
```kotlin
class QuintSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = TODO("stub")
    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> = emptyArray()
}
```

**`highlighting/QuintSyntaxHighlighterFactory.kt`**
```kotlin
class QuintSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?) = QuintSyntaxHighlighter()
}
```

**`highlighting/QuintColorSettingsPage.kt`** — stub implementing `ColorSettingsPage` with empty returns.

**`editor/QuintBraceMatcher.kt`** — stub implementing `PairedBraceMatcher` returning empty pairs.

**`editor/QuintCommenter.kt`** — stub implementing `Commenter` returning null for all.

**`editor/QuintFoldingBuilder.kt`** — stub extending `FoldingBuilderEx` with empty results.

**`parser/QuintParserDefinition.kt`** — stub implementing `ParserDefinition`. The `createLexer()` can return a dummy lexer, `createParser()` returns null/throws. Key: it must return `QuintFile` from `createFile()` and define `getCommentTokens()`/`getStringLiteralElements()` using `QuintTokenTypes`.

**`psi/QuintFile.kt`**
```kotlin
class QuintFile(viewProvider: FileViewProvider) :
    PsiFileBase(viewProvider, QuintLanguage.INSTANCE) {
    override fun getFileType() = QuintFileType.INSTANCE
    override fun toString() = "Quint File"
}
```

**`structure/QuintStructureViewFactory.kt`** — stub implementing `PsiStructureViewFactory` returning null.

**`completion/QuintCompletionContributor.kt`** — stub extending `CompletionContributor` with no providers registered.

**`annotator/QuintExternalAnnotator.kt`** — stub extending `ExternalAnnotator<Unit, Unit>` with no-op methods.

**`settings/QuintSettingsState.kt`** — stub extending `PersistentStateComponent<QuintSettingsState>` with `quintBinaryPath: String = ""`.

**`settings/QuintSettingsConfigurable.kt`** — stub implementing `Configurable` with no-op methods.

### Acceptance Criteria for T0
- `./gradlew build` succeeds (compilation passes)
- All stubs compile without errors
- ANTLR4 generates parser code from Quint.g4 at build time
- Plugin can be loaded in `./gradlew runIde` without crashes (features won't work — stubs)
- Git repo initialized, all files committed on `main` branch

---

## T1: Lexer + Syntax Highlighting + Editor Basics (Parallel, Worktree)

### Goal
Replace stubs in `lexer/`, `highlighting/`, and `editor/QuintBraceMatcher.kt`, `editor/QuintCommenter.kt` with working implementations. `.qnt` files display with correct syntax coloring, brace matching works, comment toggling works.

### Branch Name
`feature/lexer-highlighting`

### Files to Modify (replace stubs)
- `src/main/jflex/com/dearlordylord/quint/idea/lexer/Quint.flex` (or wherever JFlex source is configured)
- `src/main/kotlin/.../highlighting/QuintSyntaxHighlighter.kt`
- `src/main/kotlin/.../highlighting/QuintSyntaxHighlighterFactory.kt`
- `src/main/kotlin/.../highlighting/QuintColorSettingsPage.kt`
- `src/main/kotlin/.../editor/QuintBraceMatcher.kt`
- `src/main/kotlin/.../editor/QuintCommenter.kt`

### Files to Create (new)
- `src/test/kotlin/.../lexer/QuintLexerTest.kt`
- `src/test/kotlin/.../highlighting/QuintHighlightingTest.kt`
- `src/test/testData/lexer/` — sample `.qnt` files
- `src/test/testData/highlighting/` — sample `.qnt` files

### DO NOT MODIFY
- `plugin.xml`, `build.gradle.kts`, `QuintLanguage.kt`, `QuintFileType.kt`, `QuintIcons.kt`, `QuintTokenTypes.kt`
- Any file in `parser/`, `psi/`, `structure/`, `annotator/`, `settings/`, `completion/`, `vendor/`
- `editor/QuintFoldingBuilder.kt` (belongs to T2)

### Implementation Details

**`Quint.flex`** — JFlex grammar, ~120 lines. Derive mechanically from Quint.g4 token rules:
- States: `YYINITIAL` only (Quint has no lexer modes)
- Return `QuintTokenTypes.*` for each token
- Use `FlexAdapter` to wrap as IntelliJ `Lexer` (or the generated class directly if JFlex plugin generates a compatible class)
- All Quint tokens from the grammar: keywords (module, import, export, from, as, const, var, assume, type, val, def, pure, action, run, temporal, nondet, if, else, match, and, or, all, any, iff, implies, Set, List), operators (+, -, *, /, %, ^, >, <, >=, <=, !=, ==, =, ->, =>, etc.), literals (STRING, INT, BOOL), identifiers (LOW_ID, CAP_ID), comments (//, /* */, ///), whitespace, braces/brackets/parens
- `BAD_CHARACTER` for anything unrecognized
- Reference: `.references/idea/10-lexer.md`

**`QuintSyntaxHighlighter.kt`**:
- `getHighlightingLexer()` → return JFlex-generated lexer
- `getTokenHighlights(tokenType)` → map `QuintTokenTypes.*` to `TextAttributesKey`:
  - Keywords → `DefaultLanguageHighlighterColors.KEYWORD`
  - `SET`, `LIST` → `DefaultLanguageHighlighterColors.CLASS_NAME` (type keywords)
  - Qualifiers (val, def, pure, action, etc.) → `DefaultLanguageHighlighterColors.KEYWORD`
  - Operators → `DefaultLanguageHighlighterColors.OPERATION_SIGN`
  - STRING → `DefaultLanguageHighlighterColors.STRING`
  - INT → `DefaultLanguageHighlighterColors.NUMBER`
  - BOOL → `DefaultLanguageHighlighterColors.KEYWORD` (or NUMBER)
  - LINE_COMMENT, COMMENT → `DefaultLanguageHighlighterColors.LINE_COMMENT` / `BLOCK_COMMENT`
  - DOCCOMMENT → `DefaultLanguageHighlighterColors.DOC_COMMENT`
  - LOW_ID → `DefaultLanguageHighlighterColors.IDENTIFIER`
  - CAP_ID → `DefaultLanguageHighlighterColors.CLASS_NAME` (types start uppercase)
  - Braces/brackets/parens → `DefaultLanguageHighlighterColors.BRACES` / `BRACKETS` / `PARENTHESES`
  - BAD_CHARACTER → `HighlighterColors.BAD_CHARACTER`
- Reference: `.references/idea/12-syntax-highlighting.md`

**`QuintColorSettingsPage.kt`**:
- Implement `ColorSettingsPage`
- Provide demo text (a short Quint module showing all token types)
- Map attribute descriptors to the TextAttributesKeys defined above
- Reference: `.references/idea/12-syntax-highlighting.md`

**`QuintBraceMatcher.kt`**:
- Implement `PairedBraceMatcher`
- Pairs: `LBRACE`/`RBRACE`, `LBRACKET`/`RBRACKET`, `LPAREN`/`RPAREN`
- `isPairedBracesAllowedBeforeType` → true for whitespace, comments, closing braces
- Reference: `.references/idea/08-custom-language-overview.md`

**`QuintCommenter.kt`**:
- Implement `Commenter`
- `getLineCommentPrefix()` → `"// "`
- `getBlockCommentPrefix()` → `"/* "`
- `getBlockCommentSuffix()` → `" */"`
- `getCommentedBlockCommentPrefix()` → null
- `getCommentedBlockCommentSuffix()` → null

### Tests

**`QuintLexerTest.kt`**:
- Tokenize sample Quint code strings directly (no IDE fixture needed)
- Assert token types and text for: keywords, operators, identifiers, strings, integers, comments, mixed code
- Test edge cases: hex integers (`0x1F_A0`), underscore separators (`1_000_000`), doc comments (`///`), hashbang (`#!/usr/bin/env quint`)
- Use a helper that creates the JFlex lexer, feeds it text, and collects token type/text pairs

**`QuintHighlightingTest.kt`**:
- Extend `BasePlatformTestCase`
- `myFixture.configureByText("test.qnt", "...")` then `myFixture.checkHighlighting()`
- Verify that keywords, strings, comments are highlighted correctly
- Note: this test loads the full plugin. The parser stub may produce errors — that's OK, highlighting is lexer-based

### Acceptance Criteria
- `./gradlew test` passes for lexer and highlighting tests
- All Quint tokens from the grammar are lexed correctly
- Opening a `.qnt` file in `./gradlew runIde` shows syntax coloring
- Ctrl+/ toggles line comments
- Brace matching highlights work for `{}`, `[]`, `()`

---

## T2: Parser + PSI + Structure + Folding (Parallel, Worktree)

### Goal
Replace stubs in `parser/`, `psi/`, `structure/`, and `editor/QuintFoldingBuilder.kt`. Vendor the `antlr4-intellij-adaptor`. `.qnt` files parsed into a PSI tree, structure view works, code folding works.

### Branch Name
`feature/parser-psi`

### Files to Modify (replace stubs)
- `src/main/kotlin/.../parser/QuintParserDefinition.kt`
- `src/main/kotlin/.../psi/QuintFile.kt` (may need minor updates)
- `src/main/kotlin/.../structure/QuintStructureViewFactory.kt`
- `src/main/kotlin/.../editor/QuintFoldingBuilder.kt`

### Files to Create (new)
- `src/main/java/org/antlr/intellij/adaptor/` — vendored adaptor source (~12 Java files)
- `src/main/kotlin/.../parser/QuintParser.kt` — ANTLR parser wrapper using adaptor
- `src/main/kotlin/.../psi/QuintPsiUtils.kt` — utility functions for PSI tree navigation
- `src/test/kotlin/.../parser/QuintParserTest.kt`
- `src/test/kotlin/.../structure/QuintStructureViewTest.kt`
- `src/test/testData/parser/` — sample `.qnt` files + expected PSI dumps

### DO NOT MODIFY
- `plugin.xml`, `build.gradle.kts`, `QuintLanguage.kt`, `QuintFileType.kt`, `QuintIcons.kt`, `QuintTokenTypes.kt`
- Any file in `lexer/`, `highlighting/`, `annotator/`, `settings/`, `completion/`
- `editor/QuintBraceMatcher.kt`, `editor/QuintCommenter.kt` (belong to T1)

### Implementation Details

**Vendor `antlr4-intellij-adaptor`**:
- Download v0.2.0 source from https://github.com/antlr/antlr4-intellij-adaptor/tree/0.2.0/src/main/java/org/antlr/intellij/adaptor
- Place under `src/main/java/org/antlr/intellij/adaptor/`
- Include these packages: `lexer/` (7 files), `parser/` (5 files), `psi/Trees.java`
- Exclude: `xpath/`, `SymtabUtils.java` (unused)
- Add BSD 2-Clause license notice in `vendor/LICENSE-antlr4-intellij-adaptor.txt` (or at top of vendored dir)
- Fix any deprecation warnings for IntelliJ 2025.1 APIs
- Reference: https://github.com/antlr/antlr4-intellij-adaptor

**`QuintParser.kt`** — ANTLR parser wrapper:
```kotlin
class QuintParser : ANTLRParserAdaptor(QuintLanguage.INSTANCE, QuintParser(null)) {
    override fun parse(parser: Parser, root: IElementType): ParseTree {
        return (parser as generatedQuintParser).modules()
    }
}
```
Note: the generated ANTLR4 parser class and our wrapper will have name conflicts. Use a different name for the wrapper (e.g., `QuintParserAdaptor`) or use the fully qualified name for the generated parser.

**`QuintParserDefinition.kt`**:
- `createLexer()` → return `ANTLRLexerAdaptor(QuintLanguage.INSTANCE, QuintLexer(null))` (using the ANTLR4-generated lexer, NOT the JFlex lexer — the parser needs ANTLR4 tokens)
- `createParser()` → return `QuintParserAdaptor()`
- `getFileNodeType()` → `IFileElementType(QuintLanguage.INSTANCE)`
- `createFile()` → `QuintFile(viewProvider)`
- `getCommentTokens()` → `QuintTokenTypes.COMMENTS` (but mapped to ANTLR4 token types via `PSIElementTypeFactory`)
- `getStringLiteralElements()` → `QuintTokenTypes.STRINGS`
- **Critical**: Use `PSIElementTypeFactory.defineLanguageIElementTypes(QuintLanguage.INSTANCE, QuintLexer.tokenNames, QuintParser.ruleNames)` to create the ANTLR4-to-IntelliJ token type mapping. This is called once, typically in a companion object or init block.
- Reference: `.references/idea/11-parser-and-psi.md`

**Token type mapping issue**: The JFlex lexer (T1) uses `QuintTokenTypes.*` IElementTypes. The ANTLR4 parser uses `PSIElementTypeFactory`-generated IElementTypes. These are DIFFERENT objects. This is expected — the JFlex lexer is used for syntax highlighting only, the ANTLR4 lexer+parser is used for PSI tree construction. The `ParserDefinition` provides the ANTLR4-based lexer for the parser pipeline.

**`QuintStructureViewFactory.kt`**:
- Implement `PsiStructureViewFactory`
- Walk PSI tree to find: modules (`module` keyword), declarations (const, var, def, val, action, type, assume)
- Map to `StructureViewTreeElement` with:
  - Modules → `PlatformIcons.PACKAGE_ICON` or custom
  - `def`/`pure def` → `PlatformIcons.FUNCTION_ICON`
  - `val`/`pure val` → `PlatformIcons.VARIABLE_ICON`
  - `var` → `PlatformIcons.FIELD_ICON`
  - `const` → `PlatformIcons.PROPERTY_ICON`
  - `action` → `PlatformIcons.METHOD_ICON`
  - `type` → `PlatformIcons.CLASS_ICON`
  - `temporal` → `PlatformIcons.ABSTRACT_METHOD_ICON`
- Extract names from PSI tree using `QuintPsiUtils`
- Reference: `.references/idea/13-references-and-navigation.md` (Structure View section)

**`QuintFoldingBuilder.kt`**:
- Extend `FoldingBuilderEx`
- Fold: module bodies (`module Name { ... }`), block expressions (`and { ... }`, `or { ... }`, `all { ... }`, `any { ... }`), block comments (`/* ... */`)
- Placeholder text: `module Name { ... }` → `module Name { ... }`, blocks → `{ ... }`
- Walk PSI tree, find nodes with LBRACE...RBRACE spanning multiple lines
- Reference: `.references/idea/08-custom-language-overview.md`

**`QuintPsiUtils.kt`**:
- Helper functions for navigating the ANTLR4-generated PSI tree
- `findModules(file: QuintFile): List<PsiElement>` — find all module nodes
- `findDeclarations(module: PsiElement): List<PsiElement>` — find declarations within a module
- `getDeclarationName(decl: PsiElement): String?` — extract the identifier name
- `getDeclarationQualifier(decl: PsiElement): String?` — extract qualifier (val, def, action, etc.)
- Use ANTLR4 rule indices (from generated `QuintParser.RULE_*` constants) to identify node types

### Tests

**`QuintParserTest.kt`**:
- Extend `ParsingTestCase` (from `com.intellij.testFramework`)
- Parse various `.qnt` files, compare PSI tree dump to expected output
- Test cases: empty module, module with declarations (const, var, val, def, action, type), expressions (arithmetic, boolean, set/list operations, lambda, if-else, match), imports, nested modules
- Ensure no parser errors for valid Quint code
- Test error recovery for invalid code

**`QuintStructureViewTest.kt`**:
- Extend `BasePlatformTestCase`
- Configure a `.qnt` file, invoke structure view, assert tree elements match expected declarations

### Acceptance Criteria
- `./gradlew test` passes for parser and structure tests
- Valid `.qnt` files parse without errors
- PSI tree contains nodes for all grammar rules
- Structure view shows modules and their declarations with correct icons
- Code folding works for modules and block expressions
- `./gradlew runIde` → open `.qnt` file → View > Tool Windows > Structure shows outline

---

## T3: External Annotator + Settings + Completion (Parallel, Worktree)

### Goal
Replace stubs in `annotator/`, `settings/`, `completion/`. Real-time diagnostics from `quint typecheck` appear in editor. Settings page for binary path. Keyword/builtin completion works.

### Branch Name
`feature/annotator-completion`

### Files to Modify (replace stubs)
- `src/main/kotlin/.../annotator/QuintExternalAnnotator.kt`
- `src/main/kotlin/.../settings/QuintSettingsState.kt`
- `src/main/kotlin/.../settings/QuintSettingsConfigurable.kt`
- `src/main/kotlin/.../completion/QuintCompletionContributor.kt`

### Files to Create (new)
- `src/main/kotlin/.../annotator/QuintToolRunner.kt` — interface for CLI invocation (DI boundary)
- `src/main/kotlin/.../annotator/QuintCliToolRunner.kt` — real implementation calling `quint` binary
- `src/main/kotlin/.../annotator/QuintTypecheckResult.kt` — data classes for JSON parsing
- `src/test/kotlin/.../annotator/QuintExternalAnnotatorTest.kt`
- `src/test/kotlin/.../annotator/QuintTypecheckResultParserTest.kt`
- `src/test/kotlin/.../completion/QuintCompletionTest.kt`
- `src/test/testData/completion/` — sample `.qnt` files
- `src/test/testData/annotator/` — sample JSON output fixtures

### DO NOT MODIFY
- `plugin.xml`, `build.gradle.kts`, `QuintLanguage.kt`, `QuintFileType.kt`, `QuintIcons.kt`, `QuintTokenTypes.kt`
- Any file in `lexer/`, `highlighting/`, `parser/`, `psi/`, `structure/`, `vendor/`
- `editor/` (all files belong to T1 or T2)

### Implementation Details

**`QuintToolRunner.kt`** — DI interface (critical for test isolation):
```kotlin
interface QuintToolRunner {
    fun typecheck(filePath: String): QuintTypecheckResult
}
```

**`QuintCliToolRunner.kt`** — real implementation:
- Read binary path from `QuintSettingsState`
- Execute: `<quintPath> typecheck <filePath> --out <tempFile>`
- Parse JSON from temp file
- Handle: binary not found (show notification), process timeout, invalid JSON
- Use `GeneralCommandLine` and `ProcessHandlerFactory` for process execution
- Do NOT run on EDT — external annotator runs on background thread by design

**`QuintTypecheckResult.kt`** — data classes:
```kotlin
data class QuintTypecheckResult(
    val stage: String,
    val errors: List<QuintError>,
    val warnings: List<QuintError>
)
data class QuintError(
    val explanation: String,
    val locs: List<QuintErrorLocation>
)
data class QuintErrorLocation(
    val source: String,
    val start: QuintPosition,
    val end: QuintPosition
)
data class QuintPosition(val line: Int, val col: Int, val index: Int)
```
- Parse using `kotlinx.serialization` or `Gson` (check which is available in IntelliJ platform runtime — `Gson` is bundled)

**`QuintExternalAnnotator.kt`**:
- Extend `ExternalAnnotator<QuintAnnotatorInput, QuintTypecheckResult>`
- `collectInformation(PsiFile)` → return file path + settings state (runs on EDT, keep fast)
- `doAnnotate(input)` → call `QuintToolRunner.typecheck()` (runs on background thread)
- `apply(file, result, holder)` → for each error, create `Annotation` with:
  - Convert `QuintPosition(line, col)` to `TextRange` using `document.getLineStartOffset(line) + col`
  - `HighlightSeverity.ERROR` or `WARNING`
  - Message from `explanation`
- If `quintBinaryPath` is empty, skip silently (plugin works without binary — just no diagnostics)
- Reference: `.references/idea/12-syntax-highlighting.md` (External Annotator section), `.references/idea/18-inspections.md`

**`QuintSettingsState.kt`**:
```kotlin
@State(
    name = "QuintSettings",
    storages = [Storage("QuintSettings.xml")]
)
class QuintSettingsState : PersistentStateComponent<QuintSettingsState> {
    var quintBinaryPath: String = ""

    override fun getState() = this
    override fun loadState(state: QuintSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(): QuintSettingsState =
            ApplicationManager.getApplication().getService(QuintSettingsState::class.java)
    }
}
```

**`QuintSettingsConfigurable.kt`**:
- Implement `Configurable`
- UI: single `TextFieldWithBrowseButton` for quint binary path
- File chooser dialog for selecting the binary
- Validate: check if file exists and is executable
- Reference: `.references/idea/03-plugin-structure.md` (Services section)

**`QuintCompletionContributor.kt`**:
- Extend `CompletionContributor`
- Register `CompletionProvider` for `CompletionType.BASIC`
- Provide:
  1. **Keywords**: module, import, export, from, as, const, var, assume, type, val, def, pure, action, run, temporal, nondet, if, else, match, and, or, all, any, iff, implies
  2. **Type keywords**: int, str, bool, Set, List, Tup, Rec, Bool, Int, Nat
  3. **Builtin operators** (after dot): union, intersect, filter, map, fold, foldl, size, contains, forall, exists, flatMap, powerset, flatten, head, tail, nth, slice, indices, append, concat, range, select, mapBy, keys, values, get, set, put, setBy, fieldNames, with, tuples, oneOf, isFinite, in, subseteq, exclude, to, cross
- Use `LookupElementBuilder.create()` with appropriate icons and type text
- Mark as `DumbAware` — keyword completion doesn't need indexes
- Reference: `.references/idea/14-code-completion.md`

### Tests

**`QuintTypecheckResultParserTest.kt`** (no IDE fixture needed):
- Parse sample JSON strings matching `quint typecheck` output format
- Test: successful typecheck (empty errors), single error, multiple errors, errors in different files
- Test: malformed JSON handling
- Fixture JSON files in `src/test/testData/annotator/`

**`QuintExternalAnnotatorTest.kt`**:
- Unit test the annotator logic with a mock `QuintToolRunner`
- Inject mock that returns predefined `QuintTypecheckResult`
- Verify that errors are mapped to correct text ranges and severities
- Test: no binary configured → no annotations (graceful degradation)

**`QuintCompletionTest.kt`**:
- Extend `BasePlatformTestCase`
- `myFixture.configureByText("test.qnt", "module M { <caret> }")` then `myFixture.completeBasic()`
- Assert completion list contains keywords (val, def, action, etc.)
- Test dot-completion context: after `mySet.<caret>`, assert builtin set operators appear
- Note: dot-completion context detection may be basic at this stage (token-based, not type-aware)

### Acceptance Criteria
- `./gradlew test` passes for annotator and completion tests
- JSON parsing handles all error formats from `quint typecheck`
- Settings page appears under Tools > Quint in IDE preferences
- With valid `quint` binary path configured, type errors appear as red squiggles
- Without binary, plugin works silently (no errors, no diagnostics)
- Basic keyword completion works in `.qnt` files
- Mock-based tests verify annotator logic without calling real `quint` binary

---

## T4: Merge + Integration Verification (Sequential)

### Goal
Merge T1-T3 branches into `main`. Verify all features work together.

### Steps
1. Merge `feature/lexer-highlighting` into `main`
2. Merge `feature/parser-psi` into `main` (expect no conflicts)
3. Merge `feature/annotator-completion` into `main` (expect no conflicts)
4. Run `./gradlew build` — must pass
5. Run `./gradlew test` — all tests from T1, T2, T3 must pass
6. Run `./gradlew runIde` and manually verify:
   - Open a `.qnt` file → syntax highlighting works
   - Keywords, strings, comments, identifiers have distinct colors
   - Brace matching highlights `{}`, `[]`, `()`
   - Ctrl+/ toggles line comments
   - View > Tool Windows > Structure shows module/declaration outline
   - Code folding works on modules
   - Typing triggers keyword completion
   - (If quint binary configured) type errors appear as annotations

### Conflict Resolution
If conflicts arise (they shouldn't given the isolation contract):
- Prefer the parallel task's version for files in their ownership
- For any shared file conflicts (shouldn't happen), inspect and merge manually

### Post-Merge Cleanup
- Remove any remaining `TODO("stub")` calls
- Ensure no dead stub code remains
- Run `./gradlew verifyPlugin` to check plugin descriptor validity

---

## Deferred to Future Sessions

### Run Configurations (PRD Phase 5)
- `QuintRunConfigurationType`, `QuintRunConfiguration`
- `quint run`, `quint test`, `quint verify` commands
- Settings editor: main module, init/step actions, invariants, backend selection
- Gutter run icons

### LSP Optional Tier (PRD A2)
- Blocked on LSP server binary distribution
- Would activate only in commercial IDEs via `<depends optional="true">com.intellij.modules.lsp</depends>`

### Advanced Intelligence
- Go-to-definition across files
- Find usages
- Rename refactoring
- Type-aware completion (using `quint typecheck` JSON output for type info)
- Stub indexes for cross-file resolution

---

## Reference Docs Index

For agent prompts, include the relevant reference docs:

| Task | Key References |
|------|---------------|
| T0 | `.references/idea/01-getting-started.md`, `02-gradle-plugin.md`, `03-plugin-structure.md`, `09-file-type.md` |
| T1 | `.references/idea/10-lexer.md`, `12-syntax-highlighting.md`, `08-custom-language-overview.md` |
| T2 | `.references/idea/11-parser-and-psi.md`, `07-psi.md`, `13-references-and-navigation.md` (Structure View) |
| T3 | `.references/idea/18-inspections.md`, `14-code-completion.md`, `12-syntax-highlighting.md` (External Annotator) |
| All | `.references/quint/Quint.g4` (canonical grammar), `PRD.md` (architecture decisions) |
