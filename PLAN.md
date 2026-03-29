# Auto-append `.qnt` extension in `from` path resolution

## Context

The Quint compiler **unconditionally appends `.qnt`** to `fromSource` paths. Users write `from "./imports"` and the compiler resolves `./imports.qnt`. There is no fallback — it does NOT try the path as-is.

Source: `quintParserFrontend.ts`:
```typescript
const importeePath = sourceResolver.lookupPath(stemPath, decl.fromSource + '.qnt')
```

Official example (`importFrom.qnt`):
```quint
import Math.* from "./imports"   // resolves to ./imports.qnt
```

Our plugin currently uses the path as-is (`findFileByRelativePath(fromSource)`), so `from "./imports"` fails to resolve.

## Fix

Two places resolve `fromSource` paths:
1. `QuintImportResolver.resolveFromSource()` — used by scope resolver and qualified refs
2. `QuintFileReference.resolve()` — delegates to #1

Only #1 needs to change.

### Change in `QuintImportResolver.resolveFromSource()`

```kotlin
fun resolveFromSource(fromSource: String, contextFile: PsiFile): PsiFile? {
    val contextVf = contextFile.virtualFile ?: return null
    val parentDir = contextVf.parent ?: return null
    // Quint compiler unconditionally appends .qnt
    val pathWithExt = if (fromSource.endsWith(".qnt")) fromSource else "$fromSource.qnt"
    val vf = parentDir.findFileByRelativePath(pathWithExt) ?: return null
    return PsiManager.getInstance(contextFile.project).findFile(vf)
}
```

Note: we also accept paths that already have `.qnt` (defensive, in case users write `from "./foo.qnt"` explicitly — the grammar allows any STRING).

### Tests

Add to `QuintImportResolverTest`:
```kotlin
fun testResolveFromSourceWithoutExtension() {
    myFixture.addFileToProject("imports.qnt", "module Math { def sqr(x) = x * x }")
    val bFile = myFixture.configureByText("main.qnt", "module M { val x = 1 }")
    val resolved = QuintImportResolver.resolveFromSource("./imports", bFile)
    assertNotNull(resolved)
    assertEquals("imports.qnt", resolved!!.name)
}

fun testResolveFromSourceWithExtensionStillWorks() {
    myFixture.addFileToProject("a.qnt", "module A { val x = 1 }")
    val bFile = myFixture.configureByText("b.qnt", "module B { val y = 2 }")
    val resolved = QuintImportResolver.resolveFromSource("./a.qnt", bFile)
    assertNotNull(resolved)
    assertEquals("a.qnt", resolved!!.name)
}
```

Add cross-file reference test without extension:
```kotlin
fun testCrossFileRefWithoutExtension() {
    myFixture.addFileToProject("a.qnt", "module A { val x = 1 }")
    myFixture.configureByText("b.qnt",
        "module B { import A.* from \"./a\" val y = <caret>x }")
    // should resolve via auto-appended .qnt
}
```

Add file reference test without extension:
```kotlin
fun testFromSourceWithoutExtension() {
    myFixture.addFileToProject("a.qnt", "module A { val x = 1 }")
    myFixture.configureByText("b.qnt",
        """module B { import A.* from "<caret>./a" }""")
    // Cmd+Click should navigate to a.qnt
}
```

### Symlinks

Symlinks work already — `VirtualFile.findFileByRelativePath()` follows them via OS-level resolution. No changes needed. Same as Quint compiler (inherits from Node.js fs, no explicit `realpath`).

## Files modified

| File | Change |
|------|--------|
| `QuintImportResolver.kt` | Append `.qnt` if missing in `resolveFromSource()` |
| `QuintImportResolverTest.kt` | 2 new tests for extension handling |
| `QuintImportScopeTest.kt` | 1 new cross-file test without extension |
| `QuintFileReferenceTest.kt` | 1 new Cmd+Click test without extension |

## Verification

```
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ./gradlew test --no-daemon
```
