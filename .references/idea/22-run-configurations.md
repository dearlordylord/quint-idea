# Run Configurations

Source: https://plugins.jetbrains.com/docs/intellij/run-configurations.html

## Architecture

### ConfigurationType

Entry point. Register at `com.intellij.configurationType`.

```kotlin
class MyConfigurationType : SimpleConfigurationType(
    "MyRunConfiguration",
    "My Language",
    "Run My Language programs",
    NotNullLazyValue.createValue { MyIcons.RUN }
) {
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return MyRunConfiguration(project, this, "My Language")
    }
}
```

```xml
<extensions defaultExtensionNs="com.intellij">
    <configurationType implementation="com.example.MyConfigurationType"/>
</extensions>
```

For multiple factories, extend `ConfigurationTypeBase` and use `addFactory()`.

### RunConfiguration

```kotlin
class MyRunConfiguration(
    project: Project,
    factory: ConfigurationType,
    name: String
) : RunConfigurationBase<MyRunProfileState>(project, factory.configurationFactories[0], name) {

    var scriptPath: String = ""
    var arguments: String = ""

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return MySettingsEditor()
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return MyRunProfileState(this, environment)
    }

    override fun checkConfiguration() {
        if (scriptPath.isBlank()) {
            throw RuntimeConfigurationError("Script path is required")
        }
        if (!File(scriptPath).exists()) {
            throw RuntimeConfigurationWarning("Script file not found: $scriptPath")
        }
    }

    // Persistence
    override fun readExternal(element: Element) {
        super.readExternal(element)
        scriptPath = JDOMExternalizerUtil.readField(element, "scriptPath", "")
        arguments = JDOMExternalizerUtil.readField(element, "arguments", "")
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        JDOMExternalizerUtil.writeField(element, "scriptPath", scriptPath)
        JDOMExternalizerUtil.writeField(element, "arguments", arguments)
    }
}
```

### RunProfileState

```kotlin
class MyRunProfileState(
    private val config: MyRunConfiguration,
    private val environment: ExecutionEnvironment
) : CommandLineState(environment) {

    override fun startProcess(): ProcessHandler {
        val commandLine = GeneralCommandLine("my-interpreter", config.scriptPath)
        if (config.arguments.isNotBlank()) {
            commandLine.addParameters(config.arguments.split(" "))
        }
        commandLine.workDirectory = File(config.project.basePath!!)
        return ProcessHandlerFactory.getInstance().createColoredProcessHandler(commandLine)
    }
}
```

### SettingsEditor

```kotlin
class MySettingsEditor : SettingsEditor<MyRunConfiguration>() {
    private val scriptPathField = TextFieldWithBrowseButton()
    private val argumentsField = JTextField()

    override fun createEditor(): JComponent {
        val panel = JPanel(GridBagLayout())
        // Layout fields
        scriptPathField.addBrowseFolderListener(
            "Select Script", null, null,
            FileChooserDescriptorFactory.createSingleFileDescriptor("my")
        )
        // Add components to panel...
        return panel
    }

    override fun applyEditorTo(config: MyRunConfiguration) {
        config.scriptPath = scriptPathField.text
        config.arguments = argumentsField.text
    }

    override fun resetEditorFrom(config: MyRunConfiguration) {
        scriptPathField.text = config.scriptPath
        argumentsField.text = config.arguments
    }
}
```

## Context-Based Creation

### LazyRunConfigurationProducer

```kotlin
class MyRunConfigurationProducer :
    LazyRunConfigurationProducer<MyRunConfiguration>() {

    override fun getConfigurationFactory(): ConfigurationFactory {
        return MyConfigurationType().configurationFactories[0]
    }

    override fun setupConfigurationFromContext(
        configuration: MyRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val file = context.location?.virtualFile ?: return false
        if (file.extension != "my") return false

        configuration.scriptPath = file.path
        configuration.name = file.nameWithoutExtension
        return true
    }

    override fun isConfigurationFromContext(
        configuration: MyRunConfiguration,
        context: ConfigurationContext
    ): Boolean {
        val file = context.location?.virtualFile ?: return false
        return configuration.scriptPath == file.path
    }
}
```

```xml
<extensions defaultExtensionNs="com.intellij">
    <runConfigurationProducer
        implementation="com.example.MyRunConfigurationProducer"/>
</extensions>
```

## Validation

```kotlin
override fun checkConfiguration() {
    // Non-fatal
    throw RuntimeConfigurationWarning("Check your settings")

    // Non-blocking error
    throw RuntimeConfigurationException("Something seems wrong")

    // Fatal - blocks execution
    throw RuntimeConfigurationError("Cannot run: missing file")
}
```

## Gutter Run Icons

```kotlin
class MyRunLineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element is MyModuleDeclaration) {
            return Info(
                AllIcons.RunConfigurations.TestState.Run,
                { "Run ${element.name}" },
                ExecutorAction.getActions(0)[0]
            )
        }
        return null
    }
}
```

```xml
<extensions defaultExtensionNs="com.intellij">
    <runLineMarkerContributor
        language="MyLanguage"
        implementationClass="com.example.MyRunLineMarkerContributor"/>
</extensions>
```

## Programmatic Execution

```kotlin
val runManager = RunManager.getInstance(project)
val settings = runManager.getConfigurationSettings(MyConfigurationType())
if (settings.isNotEmpty()) {
    ProgramRunnerUtil.executeConfiguration(settings[0], DefaultRunExecutor.getRunExecutorInstance())
}
```
