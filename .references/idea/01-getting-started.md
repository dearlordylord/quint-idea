# Getting Started with IntelliJ Plugin Development

Source: https://plugins.jetbrains.com/docs/intellij/developing-plugins.html

## Prerequisites

- **IDE**: IntelliJ IDEA (Community or Ultimate), keep version current
- **JDK**: Java 17 or higher
- **Gradle**: 8.13 or later
- **Required IDE Plugins**:
  - Gradle plugin (typically pre-installed)
  - Plugin DevKit (install from JetBrains Marketplace; not bundled since 2023.3)

Review Plugin User Experience (UX) guidelines before starting.

## Build System

Use Gradle with the IntelliJ Platform Gradle Plugin:

- **IntelliJ Platform Gradle Plugin (2.x)** — Required for platform versions 2024.2+
  - Plugin ID: `org.jetbrains.intellij.platform`
- **Gradle IntelliJ Plugin (1.x)** — Obsolete; supports versions through 2022.3
  - Plugin ID: `org.jetbrains.intellij`

## Project Creation

### Option 1: New Project Wizard

1. File > New > Project
2. Select "IDE Plugin" generator
3. Configure project name, location, language (Java/Kotlin), build system (Gradle)
4. Generates minimal boilerplate

### Option 2: IntelliJ Platform Plugin Template (GitHub)

Repository: https://github.com/JetBrains/intellij-platform-plugin-template

Includes:
- Pre-configured Gradle build
- CI/CD workflows (GitHub Actions)
- Plugin signing setup
- Changelog management
- Code quality checks

### Minimal `build.gradle.kts`

```kotlin
plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij.platform") version "2.12.0"
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea("2024.3")
        instrumentationTools()
    }
}
```

### Minimal `plugin.xml`

Located at `src/main/resources/META-INF/plugin.xml`:

```xml
<idea-plugin>
    <id>com.example.myplugin</id>
    <name>My Plugin</name>
    <vendor>My Company</vendor>
    <description><![CDATA[
        Plugin description here.
    ]]></description>

    <depends>com.intellij.modules.platform</depends>

    <extensions defaultExtensionNs="com.intellij">
        <!-- Extensions go here -->
    </extensions>
</idea-plugin>
```

## Running and Debugging

The Gradle plugin provides tasks for running a development IDE instance:

```bash
# Run plugin in development IDE
./gradlew runIde

# Run with debugging
./gradlew runIde --debug-jvm
```

The development instance uses a separate sandbox directory with its own config, system, and plugins directories, isolated from your main IDE installation.

### IDE Development Instance

- Runs alongside your main IDE
- Has separate configuration and plugin directories
- Automatically includes your plugin
- Uses the IDE version specified in `build.gradle.kts`

## Kotlin Support

Kotlin is fully supported for plugin development. Add Kotlin to an existing plugin project:

```kotlin
plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
}
```

Guidelines:
- Use Kotlin version bundled with the target IDE platform
- Avoid using Kotlin standard library features newer than the bundled version
- Kotlin classes are compatible with Java-based extension points
- Use `class` (not `object`) for extensions and services

## Project Structure

```
my-plugin/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── src/
│   ├── main/
│   │   ├── java/          # or kotlin/
│   │   │   └── com/example/
│   │   └── resources/
│   │       └── META-INF/
│   │           └── plugin.xml
│   └── test/
│       ├── java/          # or kotlin/
│       │   └── com/example/
│       └── testData/
└── gradle/
    └── wrapper/
```

## Alternative Approaches

- **Theme plugins**: Can use the legacy DevKit model
- **Scala plugins**: Can use the SBT plugin from JetBrains
