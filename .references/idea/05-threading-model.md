# Threading Model

Source: https://plugins.jetbrains.com/docs/intellij/threading-model.html

## Core Architecture

Two main thread categories:

- **EDT (Event Dispatch Thread)**: Handles UI events and data modifications. Must complete quickly.
- **BGT (Background Threads)**: Handle long-running operations. Multiple BGTs run concurrently.

## Read-Write Lock System

Application-wide synchronization for PSI, VFS, and project model:

| Lock Type | Access | Thread | Restrictions |
|-----------|--------|--------|-------------|
| Read Lock | Read only | Any thread | Blocked if write lock held |
| Write Intent Lock | Read + upgrade | Any thread | Blocked by other write intent/write |
| Write Lock | Read + Write | EDT only | Exclusive access |

Multiple readers can run concurrently. Write lock blocks all other access.

## Read Actions

```kotlin
// Modern Kotlin (2024.1+)
val psiFile = readAction {
    // read and return PsiFile
}

// Traditional
val psiFile = ReadAction.compute<PsiFile, Throwable> {
    // read and return PsiFile
}

// Java
ReadAction.run(() -> {
    // read data
});
```

Rules:
- Allowed from any thread
- On EDT invoked with `Application.invokeLater()`, write intent lock is implicit — no explicit read action needed
- Objects accessed in read actions aren't guaranteed valid between consecutive actions — verify validity

## Write Actions

```kotlin
// Modern Kotlin (2024.1+)
writeAction {
    // modify data
}

// Traditional
WriteAction.run<Throwable> {
    // modify data
}
```

Rules:
- **Only on EDT**
- Must always wrap in write action API
- Forbidden inside UI renderers
- Only in write-safe contexts (user actions, `SwingUtilities.invokeLater()` from them)

## Non-Blocking Read Actions (NBRA)

Standard read actions holding locks too long freeze the UI. Use cancellable read actions:

```kotlin
ReadAction.nonBlocking {
    // expensive operation
}
    .expireWith(project)      // auto-cancel on project close
    .inSmartMode(project)     // wait for indexing
    .coalesceBy(this)         // deduplicate concurrent calls
    .submit(AppExecutorUtil.getAppExecutorService())
```

Key practices:
- Check object validity at start
- `inSmartMode()` when accessing file-based indexes
- Automatically cancelled when write action arrives, then restarted

## EDT and Modality

Use `Application.invokeLater()` instead of `SwingUtilities.invokeLater()`:

```kotlin
ApplicationManager.getApplication().invokeLater({
    // EDT code
}, ModalityState.defaultModalityState())
```

ModalityState options:

| State | Behavior |
|-------|----------|
| `defaultModalityState()` | Best for most cases |
| `current()` | Execute when modality stack unchanged |
| `stateForComponent()` | Execute when component's dialog is topmost |
| `nonModal()` | Run after all modal dialogs close |
| `any()` | Execute immediately (UI only, no PSI/VFS mods) |

## Performance Rules

**Avoid on EDT:**
- Long VFS traversals
- PSI parsing
- Reference resolution
- Index queries
- Heavy event listener operations

**Optimization:**
- Minimize write action scope — prepare data in BGT first
- Use `MergingUpdateQueue` for scheduling background event processing
- Use `AsyncFileListener` for VFS events
- Move slow operations to BGT

## Checking Current Thread

```kotlin
ApplicationManager.getApplication().isDispatchThread  // true if EDT

UIUtil.invokeLaterIfNeeded {
    // runs immediately on EDT, schedules if on BGT
}
```
