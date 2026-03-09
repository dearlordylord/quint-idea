# Plugin Structure

Source: https://plugins.jetbrains.com/docs/intellij/plugin-structure.html

## Plugin Content / Directory Structure

```
my-plugin/
├── lib/
│   ├── my-plugin.jar          # Plugin classes
│   └── third-party-lib.jar    # Dependencies
├── META-INF/
│   └── plugin.xml             # Plugin descriptor
└── classes/                   # (alternative to JAR)
```

## plugin.xml Configuration File

Located at `src/main/resources/META-INF/plugin.xml`. This is the core configuration file.

### Root Element

```xml
<idea-plugin url="https://example.com/my-plugin" require-restart="false">
    <!-- ... -->
</idea-plugin>
```

Attributes:
- `url` (optional): Plugin homepage
- `require-restart` (optional, default: false): Whether installation requires IDE restart

### Essential Metadata

```xml
<idea-plugin>
    <!-- Unique plugin identifier (like Java package). Cannot change after release. -->
    <id>com.example.myplugin</id>

    <!-- User-visible name in title case -->
    <name>My Plugin</name>

    <!-- Semantic version -->
    <version>1.0.0</version>

    <!-- Vendor info -->
    <vendor url="https://example.com" email="support@example.com">
        My Company
    </vendor>

    <!-- Plugin description (HTML in CDATA) -->
    <description><![CDATA[
        <p>Description of the plugin.</p>
        <ul>
            <li>Feature 1</li>
            <li>Feature 2</li>
        </ul>
    ]]></description>

    <!-- Change notes (HTML in CDATA) -->
    <change-notes><![CDATA[
        <p>Version 1.0.0: Initial release</p>
    ]]></change-notes>

    <!-- IDE version compatibility -->
    <idea-version since-build="243" until-build="243.*"/>
</idea-plugin>
```

### Dependencies

```xml
<!-- Required dependency -->
<depends>com.intellij.modules.platform</depends>

<!-- Optional dependency with separate config -->
<depends optional="true" config-file="optional-features.xml">
    com.intellij.modules.java
</depends>

<!-- Incompatible plugin -->
<incompatible-with>com.example.conflicting.plugin</incompatible-with>
```

Common module dependencies:
- `com.intellij.modules.platform` — all IDEs
- `com.intellij.modules.lang` — language support
- `com.intellij.modules.java` — Java support (IntelliJ only)
- `com.intellij.modules.lsp` — LSP support

### Extensions

```xml
<extensions defaultExtensionNs="com.intellij">
    <!-- Register extensions to IntelliJ platform extension points -->
    <appStarter implementation="com.example.MyAppStarter"/>
    <projectConfigurable implementation="com.example.MySettings"/>
</extensions>

<!-- Extending another plugin -->
<extensions defaultExtensionNs="another.plugin.id">
    <myExtensionPoint implementation="com.example.MyImpl"/>
</extensions>
```

Extension attributes:
- `id` (optional): Unique extension identifier
- `order` (optional): `first`, `last`, `before <id>`, `after <id>`
- `os` (optional): `freebsd`, `linux`, `mac`, `unix`, `windows`

### Extension Points

```xml
<extensionPoints>
    <extensionPoint
        name="myExtensionPoint"
        interface="com.example.MyInterface"
        dynamic="true"
        area="IDEA_PROJECT">
        <with attribute="implementationClass" implements="com.example.BaseClass"/>
    </extensionPoint>
</extensionPoints>
```

Attributes:
- `name` or `qualifiedName` (required)
- `interface` or `beanClass` (required)
- `dynamic` (optional): Supports dynamic plugin loading
- `area` (optional): `IDEA_APPLICATION` (default), `IDEA_PROJECT`, `IDEA_MODULE`

### Actions

```xml
<actions resource-bundle="messages.MyBundle">
    <action id="MyAction"
            class="com.example.MyAction"
            text="My Action"
            description="Does something"
            icon="AllIcons.Actions.Execute">
        <keyboard-shortcut keymap="$default" first-keystroke="ctrl alt G"/>
        <add-to-group group-id="ToolsMenu" anchor="last"/>
    </action>

    <group id="MyGroup" text="My Group" popup="true" compact="false">
        <action id="MyGroupAction" class="com.example.GroupAction"/>
        <separator/>
        <reference ref="MyAction"/>
        <add-to-group group-id="MainMenu" anchor="after" relative-to-action="ToolsMenu"/>
    </group>
</actions>
```

### Listeners

```xml
<!-- Application-level listeners -->
<applicationListeners>
    <listener topic="com.intellij.openapi.vfs.VirtualFileListener"
              class="com.example.MyVfsListener"
              activeInTestMode="true"
              activeInHeadlessMode="true"/>
</applicationListeners>

<!-- Project-level listeners -->
<projectListeners>
    <listener topic="com.intellij.openapi.project.ProjectManagerListener"
              class="com.example.MyProjectListener"/>
</projectListeners>
```

### Resource Bundles

```xml
<resource-bundle>messages.MyBundle</resource-bundle>
```

### XInclude for Splitting Config

```xml
<xi:include href="/META-INF/optional-features.xml"
            xpointer="xpointer(/idea-plugin/*)">
    <xi:fallback/>
</xi:include>
```

## Services

Three scopes:
- **Application-level**: Global singleton (`@Service`)
- **Project-level**: Per-project instance (`@Service(Service.Level.PROJECT)`)
- **Module-level**: Per-module instance (discouraged)

### Light Services (Recommended)

```kotlin
@Service
class MyAppService {
    fun doSomething(param: String) {}
}

@Service(Service.Level.PROJECT)
class MyProjectService(private val project: Project) {
    fun doSomething(param: String) {}
}
```

Retrieval:

```kotlin
val appService = service<MyAppService>()
val projectService = project.service<MyProjectService>()
```

Restrictions for light services:
- Class must be `final`
- No constructor injection for dependencies
- Cannot be overridden

### Traditional Services (plugin.xml)

```xml
<extensions defaultExtensionNs="com.intellij">
    <applicationService
        serviceInterface="com.example.MyService"
        serviceImplementation="com.example.MyServiceImpl"/>
    <projectService
        serviceInterface="com.example.MyProjectService"
        serviceImplementation="com.example.MyProjectServiceImpl"/>
</extensions>
```

### Service Lifecycle

- Loaded on-demand via `getService()`
- Never cache service instances in fields
- Avoid heavy initialization in constructors
- Services can implement `Disposable` for cleanup
- Kotlin services can receive `CoroutineScope` via constructor

## Plugin Class Loaders

- Each plugin gets its own class loader
- Class loader delegates to parent (platform) first, then searches plugin classes
- Dependencies declared in `<depends>` allow accessing other plugins' classes

## Deprecated: Components

`<application-components>`, `<project-components>`, `<module-components>` are deprecated. Replace with services and extensions.
