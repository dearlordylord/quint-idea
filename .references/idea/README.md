# IntelliJ IDEA Plugin Development Reference

Complete reference documentation for creating IntelliJ IDEA plugins, sourced from the [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html).

## Table of Contents

### Getting Started
- [01-getting-started.md](01-getting-started.md) - Prerequisites, project creation, Gradle setup, running/debugging
- [02-gradle-plugin.md](02-gradle-plugin.md) - IntelliJ Platform Gradle Plugin 2.x configuration, dependencies, tasks, publishing

### Plugin Architecture
- [03-plugin-structure.md](03-plugin-structure.md) - Plugin components, plugin.xml configuration, extensions, services, listeners
- [04-action-system.md](04-action-system.md) - AnAction, registration, action groups, keyboard shortcuts, toolbar/menu integration
- [05-threading-model.md](05-threading-model.md) - EDT, read/write actions, background threads, non-blocking read actions, modality
- [06-disposables.md](06-disposables.md) - Disposable lifecycle, parent-child relationships, resource cleanup

### PSI (Program Structure Interface)
- [07-psi.md](07-psi.md) - PSI overview, PSI files, PSI elements, PSI references, virtual file system

### Custom Language Support
- [08-custom-language-overview.md](08-custom-language-overview.md) - Overview of all custom language support features
- [09-file-type.md](09-file-type.md) - Registering file types and language definitions
- [10-lexer.md](10-lexer.md) - Implementing lexers with JFlex, token types, lexer state
- [11-parser-and-psi.md](11-parser-and-psi.md) - Parser implementation, Grammar-Kit, PsiBuilder, PSI tree construction
- [12-syntax-highlighting.md](12-syntax-highlighting.md) - Lexer-based, parser-based, and annotator-based highlighting
- [13-references-and-navigation.md](13-references-and-navigation.md) - PsiReference, resolve, navigation, Go to Declaration
- [14-code-completion.md](14-code-completion.md) - CompletionContributor, CompletionProvider, LookupElement
- [15-find-usages.md](15-find-usages.md) - FindUsagesProvider, WordsScanner, UsageTypeProvider
- [16-refactoring.md](16-refactoring.md) - Rename refactoring, NamesValidator, RenamePsiElementProcessor
- [17-code-formatting.md](17-code-formatting.md) - FormattingModelBuilder, Block tree, Spacing, Indent, Wrap, Alignment
- [18-inspections.md](18-inspections.md) - Code inspections, LocalInspectionTool, QuickFix, Annotator
- [19-stub-indexes.md](19-stub-indexes.md) - Stub trees, stub element types, stub indexes, serialization

### LSP (Language Server Protocol)
- [20-lsp.md](20-lsp.md) - LSP support in IntelliJ, LspServerSupportProvider, LspServerDescriptor, supported features

### UI Components
- [21-tool-windows.md](21-tool-windows.md) - ToolWindowFactory, declarative/programmatic registration, content management
- [22-run-configurations.md](22-run-configurations.md) - ConfigurationType, RunConfiguration, SettingsEditor, context-based creation

### Testing
- [23-testing.md](23-testing.md) - Test fixtures, BasePlatformTestCase, test data, light/heavy tests

### Publishing
- [24-publishing.md](24-publishing.md) - JetBrains Marketplace, plugin signing, release channels, versioning

---

**Source:** [IntelliJ Platform Plugin SDK Documentation](https://plugins.jetbrains.com/docs/intellij/welcome.html)
**Reference VS Code extension:** [Quint VS Code Extension](https://github.com/informalsystems/quint/tree/main/vscode/quint-vscode)
