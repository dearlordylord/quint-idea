# Registering File Types

Source: https://plugins.jetbrains.com/docs/intellij/registering-file-type.html

## Language Definition

First, define the language:

```kotlin
class MyLanguage : Language("MyLanguage") {
    companion object {
        val INSTANCE = MyLanguage()
    }
}
```

## File Type Implementation

Extend `LanguageFileType`:

```kotlin
class MyFileType : LanguageFileType(MyLanguage.INSTANCE) {

    override fun getName(): String = "My File"

    override fun getDescription(): String = "My language file"

    override fun getDefaultExtension(): String = "my"

    override fun getIcon(): Icon = MyIcons.FILE

    companion object {
        val INSTANCE = MyFileType()
    }
}
```

## Registration in plugin.xml

```xml
<extensions defaultExtensionNs="com.intellij">
    <fileType
        implementationClass="com.example.MyFileType"
        fieldName="INSTANCE"
        name="My File"
        language="MyLanguage"
        extensions="my"/>
</extensions>
```

## File Association Attributes

| Attribute | Format | Example |
|-----------|--------|---------|
| `extensions` | Semicolon-separated, no `.` prefix | `"my;myl"` |
| `fileNames` | Case-sensitive exact names | `"Makefile;Dockerfile"` |
| `fileNamesCaseInsensitive` | Case-insensitive names | `"makefile"` |
| `patterns` | Wildcard patterns (`*`, `?`) | `"*.my;my-*"` |
| `hashBangs` | Hashbang patterns | `"myinterpreter"` |

## Icons

Create an icon holder class:

```kotlin
object MyIcons {
    val FILE = IconLoader.getIcon("/icons/myfile.svg", MyIcons::class.java)
}
```

Place icons at `src/main/resources/icons/myfile.svg`. Use 16x16 SVG icons.

## OS File Association

Implement `OSFileIdeAssociation` interface to control OS-level file associations.

## Plugin Recommendations

Register `com.intellij.fileTypeStatisticProvider` to enable "Plugin Recommendations" — notifications suggesting your plugin when users open files of your type.
