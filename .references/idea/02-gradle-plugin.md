# IntelliJ Platform Gradle Plugin 2.x

Source: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html

## Requirements

- IntelliJ Platform: 2022.3+
- Gradle: 8.13+
- Java Runtime: 17+
- Plugin ID: `org.jetbrains.intellij.platform`

## Basic Setup

### `build.gradle.kts`

```kotlin
plugins {
    id("org.jetbrains.intellij.platform") version "2.12.0"
}

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

### Groovy (`build.gradle`)

```groovy
plugins {
    id 'org.jetbrains.intellij.platform' version '2.12.0'
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea '2024.3'
        instrumentationTools()
    }
}
```

## Repository Configuration

### Default (recommended)

```kotlin
repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}
```

### With Marketplace Plugin Support

```kotlin
repositories {
    mavenCentral()
    intellijPlatform {
        releases()
        marketplace()
    }
}
```

### Centralized in `settings.gradle.kts`

```kotlin
import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

plugins {
    id("org.jetbrains.intellij.platform.settings") version "2.12.0"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
        intellijPlatform {
            defaultRepositories()
        }
    }
}
```

## Platform Dependencies

### Target a specific IDE

```kotlin
dependencies {
    intellijPlatform {
        intellijIdea("2024.3")          // IntelliJ IDEA Community
        // or: intellijIdeaUltimate("2024.3")
        // or: clion("2024.3")
        // or: goLand("2024.3")
        // etc.
    }
}
```

### Parametrized via `gradle.properties`

```properties
# gradle.properties
platformType = IC
platformVersion = 2024.3
```

```kotlin
dependencies {
    intellijPlatform {
        val type = providers.gradleProperty("platformType")
        val version = providers.gradleProperty("platformVersion")
        create(type, version)
    }
}
```

### Using Platform Type Enum

```kotlin
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

dependencies {
    intellijPlatform {
        create(IntelliJPlatformType.IntellijIdeaCommunity, "2024.3")
    }
}
```

### Local IDE Instance

```kotlin
dependencies {
    intellijPlatform {
        local("/Users/user/Applications/IntelliJ IDEA Ultimate.app")
    }
}
```

## Plugin Dependencies

### Bundled Plugins

```kotlin
dependencies {
    intellijPlatform {
        intellijIdea("2024.3")
        bundledPlugin("com.intellij.java")
    }
}
```

### Marketplace Plugins

```kotlin
dependencies {
    intellijPlatform {
        plugin("org.intellij.scala", "2024.1.4")
    }
}
```

### Combined

```kotlin
dependencies {
    intellijPlatform {
        intellijIdea("2025.3.3")
        bundledPlugin("com.intellij.java")
        plugin("org.intellij.scala", "2024.1.4")
    }
}
```

## Multi-Module Projects

### Root Module

```kotlin
plugins {
    id("org.jetbrains.intellij.platform") version "2.12.0"
}
```

### Submodules

```kotlin
plugins {
    id("org.jetbrains.intellij.platform.module")
}
```

The `module` variant avoids polluting submodules with root-level tasks (signing, publishing, running).

## Key Gradle Tasks

| Task | Description |
|------|-------------|
| `runIde` | Run plugin in development IDE instance |
| `buildPlugin` | Build plugin distribution ZIP |
| `signPlugin` | Sign plugin distribution |
| `publishPlugin` | Publish to JetBrains Marketplace |
| `verifyPlugin` | Verify plugin structure |
| `verifyPluginSignature` | Verify plugin signature |
| `verifyPluginProjectConfiguration` | Validate project setup |

## Plugin Signing Configuration

```kotlin
signPlugin {
    certificateChain.set(providers.environmentVariable("CERTIFICATE_CHAIN"))
    privateKey.set(providers.environmentVariable("PRIVATE_KEY"))
    password.set(providers.environmentVariable("PRIVATE_KEY_PASSWORD"))
}

publishPlugin {
    token.set(providers.environmentVariable("PUBLISH_TOKEN"))
}
```

## Publishing Configuration

```kotlin
intellijPlatform {
    publishing {
        token.set(providers.environmentVariable("PUBLISH_TOKEN"))
        channels.set(listOf("beta"))  // default is empty (stable)
    }
}
```

## Plugin Variants

| Sub-plugin | ID | Usage |
|-----------|-----|-------|
| Platform | `org.jetbrains.intellij.platform` | Main plugin |
| Module | `org.jetbrains.intellij.platform.module` | Submodules |
| Settings | `org.jetbrains.intellij.platform.settings` | Repository config in settings.gradle |

## Snapshot Releases

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        maven("https://central.sonatype.com/repository/maven-snapshots/")
        gradlePluginPortal()
    }
}
```

Use `--refresh-dependencies` to update snapshots.

## IDE Source Attachment

Enabled by default via `downloadSources` property. Configure in IDE:
- Settings > Advanced Settings > Build Tools > Gradle > Download sources (2023.2+)
