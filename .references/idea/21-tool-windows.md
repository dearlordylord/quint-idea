# Tool Windows

Source: https://plugins.jetbrains.com/docs/intellij/tool-windows.html

## Overview

Tool windows are IDE child windows displaying information, attached to the outer edges (left, right, bottom). Two registration approaches: declarative and programmatic.

## Declarative Registration

### plugin.xml

```xml
<extensions defaultExtensionNs="com.intellij">
    <toolWindow
        id="My Tool Window"
        factoryClass="com.example.MyToolWindowFactory"
        anchor="right"
        icon="AllIcons.General.Information"
        secondary="false"/>
</extensions>
```

Attributes:
- `id` (required): Identifier; use `toolwindow.stripe.[id]` for localization
- `factoryClass` (required): `ToolWindowFactory` implementation
- `anchor`: `"left"` (default), `"right"`, `"bottom"`
- `icon`: Button icon (13x13)
- `secondary`: Primary or secondary group placement

### ToolWindowFactory

```kotlin
class MyToolWindowFactory : ToolWindowFactory {

    // Lazy loading: called only when user clicks the button
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JPanel(BorderLayout())
        panel.add(JLabel("Hello from My Tool Window"), BorderLayout.CENTER)

        val content = ContentFactory.getInstance().createContent(panel, "Tab 1", false)
        toolWindow.contentManager.addContent(content)
    }

    // Conditional display (evaluated once on project load)
    override fun isApplicable(project: Project): Boolean {
        return true  // Show for all projects, or check conditions
    }
}
```

### With Toolbar

```kotlin
class MyToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = SimpleToolWindowPanel(true, true)  // vertical toolbar, border

        // Add toolbar
        val actionGroup = DefaultActionGroup()
        actionGroup.add(MyRefreshAction())
        val toolbar = ActionManager.getInstance()
            .createActionToolbar(ActionPlaces.TOOLWINDOW_TITLE, actionGroup, true)
        toolbar.targetComponent = panel
        panel.toolbar = toolbar.component

        // Add content
        panel.setContent(JLabel("Content"))

        val content = ContentFactory.getInstance().createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
```

## Multiple Tabs

```kotlin
override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val cm = toolWindow.contentManager

    val tab1 = ContentFactory.getInstance().createContent(Panel1(), "Overview", false)
    val tab2 = ContentFactory.getInstance().createContent(Panel2(), "Details", false)

    cm.addContent(tab1)
    cm.addContent(tab2)
}
```

Enable tab closing:

```xml
<toolWindow id="My Window" canCloseContents="true" .../>
```

Disable for specific tabs: `content.isCloseable = false`

## Programmatic Registration

```kotlin
ToolWindowManager.getInstance(project).registerToolWindow("My Window") {
    contentFactory = { toolWindow ->
        // Create content
    }
    anchor = ToolWindowAnchor.BOTTOM
    icon = MyIcons.TOOL_WINDOW
}
```

Always use `ToolWindowManager.invokeLater()` instead of `Application.invokeLater()` for tool window EDT tasks.

## Common Operations

```kotlin
// Access existing tool window
val tw = ToolWindowManager.getInstance(project).getToolWindow("My Window")

// Show notifications
ToolWindowManager.getInstance(project).notifyByBalloon(
    "My Window",
    MessageType.INFO,
    "Operation completed"
)

// Listen to events
project.messageBus.connect().subscribe(
    ToolWindowManagerListener.TOPIC,
    object : ToolWindowManagerListener {
        override fun toolWindowShown(toolWindow: ToolWindow) { /* ... */ }
    }
)
```
