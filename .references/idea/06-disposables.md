# Disposable Lifecycle Management

Source: https://plugins.jetbrains.com/docs/intellij/disposers.html

## Overview

`Disposer` is a singleton managing a tree of `Disposable` instances for resource cleanup. Children are disposed before parents.

## Resource Types Managed

- Listeners (most common)
- File handles, database connections
- Caches and data structures

## Automatic Disposal

- Application-level services: disposed on IDE closure or plugin unload
- Project-level services: disposed on project close or plugin unload
- Extensions registered in plugin.xml are **not** automatically disposed

## Registration

```kotlin
Disposer.register(parentDisposable, childDisposable)
```

## Choosing a Parent Disposable

| Use Case | Parent |
|----------|--------|
| Plugin lifetime | Application/project-level service |
| Dialog display | `DialogWrapper.getDisposable()` |
| Tool window tab | `Content.setDisposer()` |
| Short-lived | `Disposer.newDisposable()` + manual disposal |

**Never** use `Application` or `Project` directly as parents in plugin code — resources won't be disposed on plugin unload.

## Implementation Pattern

```kotlin
class MyResource(parentDisposable: Disposable) : Disposable {
    init {
        Disposer.register(parentDisposable, this)
    }

    override fun dispose() {
        // cleanup
    }
}
```

## Registering Listeners with Disposal

```kotlin
// Preferred: listener auto-unsubscribes on disposal
messageBus.connect(parentDisposable).subscribe(topic, listener)

// API with parent disposable parameter
model.addModelListener(listener, parentDisposable)
```

## Utility Methods

```kotlin
Disposer.isDisposed(disposable)  // Check disposal status
Disposer.dispose(disposable)      // Manual disposal (recursive)
```

## Memory Leak Detection

Enable with `idea.disposer.debug=on`. Platform checks on exit in test/internal/debug modes. Look at "Caused by" stack trace to find where undisposed objects were registered.
