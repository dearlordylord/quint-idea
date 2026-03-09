# Action System

Source: https://plugins.jetbrains.com/docs/intellij/action-system.html

## Overview

The Action System enables plugins to add menu items and toolbar buttons to IntelliJ IDEs. Every action requires implementation and registration.

## AnAction Class

Actions extend `AnAction`. Instances persist for the application's lifetime.

**Critical**: `AnAction` subclasses must not have class fields of any kind (memory leak risk). Use services for state.

For dumb-mode support, extend `DumbAwareAction`.

### Two Essential Methods

#### `update(AnActionEvent)`

Determines action availability. Called frequently — must execute very quickly.

```kotlin
override fun update(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT)
    val editor = e.getData(CommonDataKeys.EDITOR)
    e.presentation.isEnabled = project != null && editor != null
    e.presentation.isVisible = project != null
}
```

Must override `getActionUpdateThread()` to specify threading:

```kotlin
override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT  // or EDT
}
```

- **BGT** (Background Thread): Safe PSI/VFS access, no Swing access
- **EDT** (Event Dispatch Thread): Swing access, avoid PSI operations

#### `actionPerformed(AnActionEvent)`

Executes when user triggers the action. Can perform substantial work.

```kotlin
override fun actionPerformed(e: AnActionEvent) {
    val project = e.getRequiredData(CommonDataKeys.PROJECT)
    val editor = e.getRequiredData(CommonDataKeys.EDITOR)
    // Perform action
}
```

## Registration

### plugin.xml Registration

```xml
<actions>
    <action id="com.example.MyAction"
            class="com.example.MyAction"
            text="My Action"
            description="Description for status bar"
            icon="AllIcons.Actions.Execute">
        <keyboard-shortcut keymap="$default"
                           first-keystroke="ctrl alt G"
                           second-keystroke="C"/>
        <mouse-shortcut keymap="$default"
                        keystroke="ctrl button3"/>
        <add-to-group group-id="ToolsMenu"
                      anchor="after"
                      relative-to-action="com.example.OtherAction"/>
        <override-text place="MainMenu" text="Alternate Text"/>
        <synonym text="SearchAlias"/>
    </action>
</actions>
```

### Programmatic Registration

```kotlin
val action = MyAction()
ActionManager.getInstance().registerAction("com.example.MyAction", action)

// Add to existing group
val group = ActionManager.getInstance().getAction("ToolsMenu") as DefaultActionGroup
group.add(action)
```

## Action Groups

Groups are containers for actions and subgroups.

```xml
<group id="com.example.MyGroup"
       text="My Group"
       description="Group description"
       popup="true"
       compact="false"
       searchable="true">
    <action id="com.example.Action1" class="com.example.Action1"/>
    <separator text="Section Name"/>
    <reference ref="com.example.ExistingAction"/>
    <add-to-group group-id="MainMenu" anchor="last"/>
</group>
```

- `popup="true"`: Creates a submenu
- `compact="true"`: Hides disabled actions in menus

## Specialized Action Types

- **ToggleAction / DumbAwareToggleAction**: Checkbox-style actions
- **DefaultActionGroup**: Standard group implementation
- **EmptyAction**: Reserves runtime-registered action IDs

## Localization via Resource Bundles

```properties
# messages/MyBundle.properties
action.com.example.MyAction.text=Localized Text
action.com.example.MyAction.description=Localized Description
group.com.example.MyGroup.text=Localized Group Name
```

Declare in plugin.xml:

```xml
<resource-bundle>messages.MyBundle</resource-bundle>
```

Or per-action:

```xml
<actions resource-bundle="messages.MyBundle">
    <!-- ... -->
</actions>
```

## Toolbar and Menu Construction (Programmatic)

```kotlin
val actionManager = ActionManager.getInstance()

// Create toolbar
val toolbar = actionManager.createActionToolbar(
    ActionPlaces.TOOLBAR,
    actionGroup,
    true  // horizontal
)
toolbar.targetComponent = myPanel

// Create popup menu
val popupMenu = actionManager.createActionPopupMenu(
    ActionPlaces.EDITOR_POPUP,
    actionGroup
)
```

## Common Data Keys

```kotlin
CommonDataKeys.PROJECT        // Current project
CommonDataKeys.EDITOR          // Active editor
CommonDataKeys.VIRTUAL_FILE    // Selected file
CommonDataKeys.PSI_FILE        // PSI file
CommonDataKeys.PSI_ELEMENT     // PSI element at caret
PlatformDataKeys.TOOL_WINDOW   // Active tool window
```

## Best Practices

- Never expose reusable logic via static methods on `AnAction` — use services/utilities instead
- Keep `update()` lightweight
- Use `ActivityTracker.getInstance().inc()` to refresh toolbar states when availability changes without user gestures
- Use UI Inspector to find existing action IDs
