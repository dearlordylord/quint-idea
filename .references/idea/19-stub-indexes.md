# Stub Indexes

Source: https://plugins.jetbrains.com/docs/intellij/stub-indexes.html

## What Are Stubs?

A stub tree is a subset of the PSI tree stored in a compact serialized binary format. It preserves only essential declaration information (method/field names, modifiers) while omitting statements and local variables.

Each stub is a bean class with no behavior, storing state and parent/child relationships.

## Why Use Stubs?

- Avoid parsing entire files to find declarations
- File-based indexes access stubs without building full PSI
- Dramatically faster cross-file resolution and navigation
- Essential for large codebases

## Creating Stub Element Types

### Step 1: File Element Type

Change file element type to extend `IStubFileElementType`:

```kotlin
class MyFileElementType : IStubFileElementType<PsiFileStub<MyFile>>(MyLanguage.INSTANCE) {
    override fun getExternalId(): String = "myLanguage.FILE"

    override fun getStubVersion(): Int = 2  // Increment when format changes
}
```

### Step 2: Plugin Configuration

```xml
<extensions defaultExtensionNs="com.intellij">
    <stubElementTypeHolder
        class="com.example.psi.MyTypes"
        externalIdPrefix="myLanguage."/>
</extensions>
```

### Step 3: Stub Interface

```kotlin
interface MyDefinitionStub : StubElement<MyDefinition> {
    val name: String?
    val isPublic: Boolean
}
```

### Step 4: Stub Implementation

```kotlin
class MyDefinitionStubImpl(
    parent: StubElement<*>?,
    override val name: String?,
    override val isPublic: Boolean
) : StubBase<MyDefinition>(parent, MyTypes.DEFINITION as IStubElementType<*, *>),
    MyDefinitionStub
```

### Step 5: PSI Element Interface

```kotlin
interface MyDefinition : StubBasedPsiElement<MyDefinitionStub>, MyNamedElement {
    // ...
}
```

### Step 6: PSI Implementation

Must have both AST node and stub constructors:

```kotlin
class MyDefinitionImpl : StubBasedPsiElementBase<MyDefinitionStub>, MyDefinition {

    // AST constructor (when parsing from text)
    constructor(node: ASTNode) : super(node)

    // Stub constructor (when loading from index)
    constructor(stub: MyDefinitionStub, type: IStubElementType<*, *>) : super(stub, type)

    override fun getName(): String? {
        // Try stub first, fall back to AST
        val stub = greenStub
        if (stub != null) return stub.name
        return findChildByType(MyTypes.IDENTIFIER)?.text
    }

    override fun setName(name: String): PsiElement {
        val newId = MyElementFactory.createIdentifier(project, name)
        nameIdentifier?.replace(newId)
        return this
    }

    override fun getNameIdentifier(): PsiElement? =
        findChildByType(MyTypes.IDENTIFIER)
}
```

### Step 7: IStubElementType

```kotlin
class MyDefinitionElementType(debugName: String) :
    IStubElementType<MyDefinitionStub, MyDefinition>(debugName, MyLanguage.INSTANCE) {

    override fun getExternalId(): String = "myLanguage.DEFINITION"

    override fun createPsi(stub: MyDefinitionStub): MyDefinition {
        return MyDefinitionImpl(stub, this)
    }

    override fun createStub(psi: MyDefinition, parentStub: StubElement<out PsiElement>?): MyDefinitionStub {
        return MyDefinitionStubImpl(parentStub, psi.name, psi.isPublic)
    }

    override fun serialize(stub: MyDefinitionStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)      // Deduplicated string storage
        dataStream.writeBoolean(stub.isPublic)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): MyDefinitionStub {
        val name = dataStream.readNameString()
        val isPublic = dataStream.readBoolean()
        return MyDefinitionStubImpl(parentStub, name, isPublic)
    }

    override fun indexStub(stub: MyDefinitionStub, sink: IndexSink) {
        val name = stub.name
        if (name != null) {
            sink.occurrence(MyDefinitionIndex.KEY, name)
        }
    }

    override fun shouldCreateStub(node: ASTNode): Boolean {
        // Only create stubs for top-level definitions
        return node.psi is MyDefinition
    }
}
```

## Serialization

- Use `StubOutputStream.writeName()` / `StubInputStream.readNameString()` for strings — deduplicates
- Increment `IStubFileElementType.getStubVersion()` when changing binary format
- **All stub data must depend only on file contents** — no external file dependencies

## Stub Indexes

### Defining an Index

```kotlin
class MyDefinitionIndex : StringStubIndexExtension<MyDefinition>() {
    override fun getKey(): StubIndexKey<String, MyDefinition> = KEY

    companion object {
        val KEY: StubIndexKey<String, MyDefinition> =
            StubIndexKey.createIndexKey("my.definition.index")

        fun findDefinitions(
            name: String,
            project: Project,
            scope: GlobalSearchScope
        ): Collection<MyDefinition> {
            return StubIndex.getElements(KEY, name, project, scope, MyDefinition::class.java)
        }
    }
}
```

Register:

```xml
<extensions defaultExtensionNs="com.intellij">
    <stubIndex implementation="com.example.MyDefinitionIndex"/>
</extensions>
```

### Populating Indexes

Done in `IStubElementType.indexStub()`:

```kotlin
override fun indexStub(stub: MyDefinitionStub, sink: IndexSink) {
    stub.name?.let { sink.occurrence(MyDefinitionIndex.KEY, it) }
}
```

### Querying Indexes

```kotlin
// Get all keys
StubIndex.getInstance().getAllKeys(MyDefinitionIndex.KEY, project)

// Get elements by key
StubIndex.getElements(
    MyDefinitionIndex.KEY,
    "myFunction",
    project,
    GlobalSearchScope.allScope(project),
    MyDefinition::class.java
)
```

Returned data may be stale — verify actual elements for accuracy.

## File-Based Indexes

For broader indexing beyond stubs:

```kotlin
class MyFileIndex : FileBasedIndexExtension<String, String>() {
    override fun getName(): ID<String, String> = NAME

    override fun getIndexer(): DataIndexer<String, String, FileContent> {
        return DataIndexer { inputData ->
            // Return key-value pairs for this file
            mapOf("key" to "value")
        }
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> =
        EnumeratorStringDescriptor.INSTANCE

    override fun getValueExternalizer(): DataExternalizer<String> =
        EnumeratorStringDescriptor.INSTANCE

    override fun getVersion(): Int = 1

    override fun getInputFilter(): FileBasedIndex.InputFilter =
        DefaultFileTypeSpecificInputFilter(MyFileType.INSTANCE)

    companion object {
        val NAME: ID<String, String> = ID.create("my.file.index")
    }
}
```
