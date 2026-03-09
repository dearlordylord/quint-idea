# Language Server Protocol (LSP) Support

Source: https://plugins.jetbrains.com/docs/intellij/language-server-protocol.html

## Overview

LSP is an open-standard protocol for communication between development tools and language servers. Provides an alternative to native PSI-based custom language support.

## Supported IDEs

Available in **commercial** IntelliJ-based IDEs: IntelliJ IDEA, WebStorm, PhpStorm, PyCharm, DataSpell, RubyMine, CLion, DataGrip, GoLand, Rider, RustRover. Unified PyCharm supported since 2025.1.

**Not available** in Community editions unless using `com.intellij.modules.lsp` dependency.

## Requirements

- IntelliJ Platform 2023.2+
- Gradle IntelliJ Platform Plugin 2.x

## Setup

### build.gradle.kts

```kotlin
plugins {
    id("org.jetbrains.intellij.platform") version "2.12.0"
}

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        intellijIdea("2025.3.3")  // Must be commercial edition
    }
}
```

### plugin.xml

```xml
<idea-plugin>
    <depends>com.intellij.modules.lsp</depends>
</idea-plugin>
```

## Minimal Implementation

### LspServerSupportProvider

```kotlin
internal class MyLspServerSupportProvider : LspServerSupportProvider {
    override fun fileOpened(
        project: Project,
        file: VirtualFile,
        serverStarter: LspServerStarter
    ) {
        if (file.extension == "my") {
            serverStarter.ensureServerStarted(MyLspServerDescriptor(project))
        }
    }
}
```

### LspServerDescriptor

```kotlin
private class MyLspServerDescriptor(project: Project) :
    ProjectWideLspServerDescriptor(project, "My Language") {

    override fun isSupportedFile(file: VirtualFile): Boolean {
        return file.extension == "my"
    }

    override fun createCommandLine(): GeneralCommandLine {
        return GeneralCommandLine("my-language-server", "--stdio")
    }
}
```

### Registration

```xml
<extensions defaultExtensionNs="com.intellij.platform">
    <lsp.serverSupportProvider
        implementation="com.example.MyLspServerSupportProvider"/>
</extensions>
```

## Supported Features by Version

### 2025.3
- Server Initiated Progress
- Highlight Usages In File
- Go To Symbol
- File Structure / Breadcrumbs / Sticky Lines
- Parameter Info

### 2025.2
- Inlay Hints (2025.2.2+)
- Folding Range (2025.2.2+)

### 2025.1
- Document Link
- Pull Diagnostics (2025.1.2+)

### 2024.3
- Color Preview
- Document Save Notification (2024.3.1+)
- Go To Type Declaration (2024.3.1+)

### 2024.2
- Find Usages
- Completion Item Resolve
- Code Action Resolve
- Semantic Highlighting (2024.2.2+)

### 2024.1
- Socket communication
- Execute Command
- Apply WorkspaceEdit
- Show Document Request

### 2023.3
- Intention Actions
- Code Formatting
- Request Cancellation
- Quick Documentation (2023.3.2+)
- File Watcher (2023.3.2+)

### 2023.2 (Initial)
- StdIO Communication
- Diagnostics Publishing
- Code Completion
- Go to Declaration

## Status Bar Widget

```kotlin
override fun createLspServerWidgetItem(
    lspServer: LspServer,
    currentFile: VirtualFile?
): LspServerWidgetItem {
    return LspServerWidgetItem(
        lspServer,
        currentFile,
        MyIcons.PLUGIN_ICON,
        MyConfigurable::class.java
    )
}
```

## Communication Methods

### StdIO (Default)

```kotlin
override fun createCommandLine(): GeneralCommandLine {
    return GeneralCommandLine("my-ls", "--stdio")
}
```

### Socket (2024.1+)

Override `startServerProcess()` instead of `createCommandLine()` and connect via socket.

## Customization

### LspCustomization

Access via `LspServerDescriptor.lspCustomization` property:

```kotlin
override val lspCustomization: LspCustomization
    get() = LspCustomization().apply {
        // Fine-tune or disable specific LSP features
    }
```

### Custom Requests/Notifications

```kotlin
// Handle custom server requests
override fun createLsp4jClient(): Lsp4jClient {
    return object : Lsp4jClient() {
        // Handle custom notifications from server
    }
}

// Send custom requests to server
override val lsp4jServerClass: Class<*>
    get() = MyLsp4jServer::class.java
```

## Language Server Distribution

Two approaches:
1. **Bundle** the language server binary as a plugin resource
2. **User-configured** server path via settings

### Bundled Server Example

```kotlin
override fun createCommandLine(): GeneralCommandLine {
    val serverPath = PathManager.getPluginsPath() + "/my-plugin/server/my-ls"
    return GeneralCommandLine(serverPath, "--stdio")
}
```

## Debugging

Enable LSP logging:
- Help > Diagnostic Tools > Debug Log Settings
- Add: `#com.intellij.platform.lsp`

## Reference Implementation

The **Prisma ORM plugin** demonstrates practical LSP implementation patterns.

## LSP vs Native

| Aspect | LSP | Native (PSI) |
|--------|-----|-------------|
| Implementation effort | Lower | Higher |
| IDE integration depth | Limited | Full |
| Performance | Network overhead | Direct |
| Feature coverage | Protocol-limited | Unlimited |
| Reuse existing server | Yes | No |
| Community edition | No | Yes |
| Offline | Depends | Yes |
