# Custom Language Support — Overview

Source: https://plugins.jetbrains.com/docs/intellij/custom-language-support.html

## Overview

IntelliJ provides a comprehensive framework for adding support for custom languages. Features are implemented incrementally, from basic (file type, syntax highlighting) to advanced (refactoring, debugging).

## Feature Implementation Order

Implement features in this recommended order:

### Foundation (Required)
1. **Language definition** — `Language` subclass
2. **File type registration** — `LanguageFileType` → [09-file-type.md](09-file-type.md)
3. **Lexer** — Tokenization → [10-lexer.md](10-lexer.md)
4. **Parser and PSI** — AST/PSI tree → [11-parser-and-psi.md](11-parser-and-psi.md)
5. **Syntax highlighting** — Colors → [12-syntax-highlighting.md](12-syntax-highlighting.md)

### Navigation & Intelligence
6. **References and resolve** — Go to Declaration → [13-references-and-navigation.md](13-references-and-navigation.md)
7. **Code completion** — Autocompletion → [14-code-completion.md](14-code-completion.md)
8. **Find Usages** → [15-find-usages.md](15-find-usages.md)
9. **Rename refactoring** → [16-refactoring.md](16-refactoring.md)

### Code Quality
10. **Code formatting** → [17-code-formatting.md](17-code-formatting.md)
11. **Code inspections** — Error/warning analysis → [18-inspections.md](18-inspections.md)

### Performance
12. **Stub indexes** — Fast indexing → [19-stub-indexes.md](19-stub-indexes.md)

### Additional Features
- Folding builder
- Commenter (line/block comments)
- Brace matching
- Structure view
- Go to symbol
- Documentation provider
- Parameter info
- Spell checking support

## Alternative: LSP

For languages with existing Language Server Protocol implementations, consider the LSP approach instead of native PSI-based support. See [20-lsp.md](20-lsp.md).

LSP provides many features with less code but with less IDE integration depth than native support.

## Custom Language Support Tutorial

The official tutorial uses a `.properties`-like language ("Simple Language") as an example. The complete tutorial has 21 steps with a reference implementation at:
https://github.com/JetBrains/intellij-sdk-code-samples/tree/main/simple_language_plugin

Tutorial steps:
1. Prerequisites
2. Language and File Type
3. Grammar and Lexer (JFlex)
4. Parser (Grammar-Kit BNF)
5. Lexer and Parser Testing
6. Syntax Highlighter
7. Color Settings Page
8. Annotator
9. Line Marker Provider
10. Completion Contributor
11. Reference Contributor
12. Find Usages Provider
13. Folding Builder
14. Go To Symbol Contributor
15. Structure View Factory
16. Formatter
17. Code Style Settings
18. Commenter
19. Quick Fix
20. Documentation Provider
21. Spell Checking

## Key Extension Points for Custom Languages

| Extension Point | Purpose |
|----------------|---------|
| `com.intellij.fileType` | File type registration |
| `com.intellij.lang.parserDefinition` | Parser and PSI |
| `com.intellij.lang.syntaxHighlighterFactory` | Syntax highlighting |
| `com.intellij.annotator` | Semantic highlighting/errors |
| `com.intellij.completion.contributor` | Code completion |
| `com.intellij.psi.referenceContributor` | Reference resolution |
| `com.intellij.lang.findUsagesProvider` | Find usages |
| `com.intellij.lang.refactoring.NamesValidator` | Name validation |
| `com.intellij.lang.formatter` | Code formatting |
| `com.intellij.localInspection` | Code inspections |
| `com.intellij.lang.foldingBuilder` | Code folding |
| `com.intellij.lang.commenter` | Comment support |
| `com.intellij.lang.braceMatcher` | Brace matching |
| `com.intellij.lang.psiStructureViewFactory` | Structure view |
| `com.intellij.gotoSymbolContributor` | Go to symbol |
| `com.intellij.lang.documentationProvider` | Quick docs |
| `com.intellij.codeInsight.parameterInfo` | Parameter hints |
| `com.intellij.spellchecker.support` | Spell checking |
| `com.intellij.colorSettingsPage` | Color settings |
