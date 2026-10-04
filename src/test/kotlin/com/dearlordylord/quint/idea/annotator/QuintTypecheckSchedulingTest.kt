package com.dearlordylord.quint.idea.annotator

import com.dearlordylord.quint.idea.settings.QuintSettingsState
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File
import java.nio.file.Files

/**
 * The annotator decides when to invoke the quint CLI (debounce, cache) and where to
 * write the snapshot quint reads. These tests assert those decisions directly against
 * a mocked [QuintToolRunner] so we can count and inspect CLI invocations.
 */
class QuintTypecheckSchedulingTest : BasePlatformTestCase() {
    private lateinit var originalBinaryPath: String
    private val invocations = mutableListOf<String>()
    private var stubResult = emptyResult()

    override fun setUp() {
        super.setUp()
        originalBinaryPath = QuintSettingsState.getInstance().quintBinaryPath
        QuintSettingsState.getInstance().quintBinaryPath = "/bin/true"
        QuintExternalAnnotator.clearCacheForTests()
        invocations.clear()
        stubResult = emptyResult()
        QuintExternalAnnotator.toolRunnerFactory = {
            object : QuintToolRunner {
                override fun typecheck(filePath: String): QuintTypecheckResult {
                    invocations.add(filePath)
                    return stubResult
                }
            }
        }
        QuintTypecheckSchedulingService.nowProvider = { 0L }
    }

    override fun tearDown() {
        try {
            QuintSettingsState.getInstance().quintBinaryPath = originalBinaryPath
            QuintExternalAnnotator.toolRunnerFactory = null
            QuintExternalAnnotator.clearCacheForTests()
            QuintTypecheckSchedulingService.nowProvider = System::currentTimeMillis
        } finally {
            super.tearDown()
        }
    }

    private fun emptyResult() =
        QuintTypecheckResult(stage = "typechecking", errors = emptyList(), warnings = emptyList())

    private fun resultWithError(message: String) = QuintTypecheckResult(
        stage = "typechecking",
        errors = listOf(QuintError(explanation = message, locs = emptyList())),
        warnings = emptyList()
    )

    private fun runPass(file: PsiFile = myFixture.file): QuintAnnotationResult? {
        val annotator = QuintExternalAnnotator()
        val input = annotator.collectInformation(file) ?: return null
        return annotator.doAnnotate(input)
    }

    private fun edit(text: String) {
        WriteCommandAction.runWriteCommandAction(project) {
            myFixture.editor.document.setText(text)
        }
    }

    private fun markEditedAt(t: Long) {
        QuintTypecheckSchedulingService.nowProvider = { t }
        QuintTypecheckSchedulingService.getInstance().markEdited(myFixture.editor.document)
    }

    fun testDefersWhileTyping() {
        myFixture.configureByText("a.qnt", "module a { val x = 1 }")
        markEditedAt(100L)

        val annotator = QuintExternalAnnotator()
        val input = annotator.collectInformation(myFixture.file)!!
        assertTrue(input.skipTypecheck)
        assertNull(annotator.doAnnotate(input))
        assertEquals(0, invocations.size)
    }

    fun testDoesNotDeferAfterQuietPeriod() {
        myFixture.configureByText("a.qnt", "module a { val x = 1 }")
        markEditedAt(100L)
        QuintTypecheckSchedulingService.nowProvider = { 100L + QuintTypecheckSchedulingService.QUIET_PERIOD_MS + 50 }

        val input = QuintExternalAnnotator().collectInformation(myFixture.file)!!
        assertFalse(input.skipTypecheck)
        QuintExternalAnnotator().doAnnotate(input)
        assertEquals(1, invocations.size)
    }

    fun testCacheHitForSameContent() {
        myFixture.configureByText("a.qnt", "module a { val x = 1 }")
        stubResult = resultWithError("oops")
        runPass()
        assertEquals(1, invocations.size)

        val second = QuintExternalAnnotator().collectInformation(myFixture.file)!!
        assertTrue(second.skipTypecheck)
        val result = QuintExternalAnnotator().doAnnotate(second)
        assertEquals(1, result?.typecheckResult?.errors?.size)
        assertEquals(1, invocations.size)
    }

    fun testCacheInvalidatesWhenContentChanges() {
        myFixture.configureByText("a.qnt", "module a { val x = 1 }")
        stubResult = resultWithError("first")
        runPass()

        edit("module a { val x = 2 }")
        QuintTypecheckSchedulingService.nowProvider = { 10_000L }
        stubResult = emptyResult()

        val input = QuintExternalAnnotator().collectInformation(myFixture.file)!!
        assertFalse(input.skipTypecheck)
        val result = QuintExternalAnnotator().doAnnotate(input)
        assertEquals(2, invocations.size)
        assertEquals(0, result?.typecheckResult?.errors?.size)
    }

    fun testCacheSurvivesSpuriousModStampReset() {
        // saveDocument + VFS refresh resets Document.modificationStamp; content-hash
        // cache must still validate because nothing the typecheck depends on changed.
        myFixture.configureByText("a.qnt", "module a { val x = 1 }")
        stubResult = resultWithError("oops")
        runPass()

        WriteAction.run<RuntimeException> {
            FileDocumentManager.getInstance().saveDocument(myFixture.editor.document)
        }

        val input = QuintExternalAnnotator().collectInformation(myFixture.file)!!
        assertTrue(input.skipTypecheck)
        QuintExternalAnnotator().doAnnotate(input)
        assertEquals(1, invocations.size)
    }

    fun testCacheKeyedByOriginalFileNotPsiCopy() {
        // The daemon hands us non-physical PsiFile copies for some passes; they must
        // share the original's cache entry, not bypass it.
        myFixture.configureByText("a.qnt", "module a { val x = 1 }")
        stubResult = resultWithError("oops")
        runPass()

        val copy = myFixture.file.copy() as PsiFile
        val input = QuintExternalAnnotator().collectInformation(copy)!!
        assertTrue(input.skipTypecheck)
        QuintExternalAnnotator().doAnnotate(input)
        assertEquals(1, invocations.size)
    }

    fun testInputSnapshotsDocumentTextAndHash() {
        // collectInformation must capture the text NOW, so doAnnotate feeds that exact
        // snapshot to quint — not whatever the live document holds afterwards.
        myFixture.configureByText("a.qnt", "module a { val x = 1 }")
        val input = QuintExternalAnnotator().collectInformation(myFixture.file)!!
        edit("module a { val x = 999 }")
        assertEquals("module a { val x = 1 }", input.documentText)
        assertEquals("module a { val x = 1 }".hashCode(), input.contentHash)
    }

    fun testNewContentTriggersFreshTypecheckEvenAfterPriorOne() {
        myFixture.configureByText("a.qnt", "module a { val x = 1 }")
        runPass()
        edit("module a { val x = 1 } // typo")
        QuintTypecheckSchedulingService.nowProvider = { 10_000L }
        stubResult = resultWithError("typo!")

        val input = QuintExternalAnnotator().collectInformation(myFixture.file)!!
        assertFalse(input.skipTypecheck)
        val result = QuintExternalAnnotator().doAnnotate(input)
        assertEquals(2, invocations.size)
        assertEquals("typo!", result?.typecheckResult?.errors?.firstOrNull()?.explanation)
    }

    fun testNestedImportUsesUnsavedDependency() {
        myFixture.addFileToProject("lib/helper.qnt", "module helper { val y = 1 }")
        val root = myFixture.addFileToProject("main.qnt", "module main { import helper.* from \"lib/helper\" val x = y }")
        val dep = FileDocumentManager.getInstance().getDocument(root.virtualFile.parent.findFileByRelativePath("lib/helper.qnt")!!)!!
        WriteCommandAction.runWriteCommandAction(project) { dep.setText("module helper { val y = 2 }") }
        QuintExternalAnnotator.toolRunnerFactory = {
            object : QuintToolRunner {
                override fun typecheck(filePath: String): QuintTypecheckResult {
                    val imported = File(File(filePath).parentFile, "lib/helper.qnt")
                    assertTrue("nested import must exist in check input", imported.exists())
                    assertEquals("module helper { val y = 2 }", imported.readText())
                    return emptyResult()
                }
            }
        }
        assertNotNull(runPass(root))
    }

    fun testDependencyChangesInvalidateRootButUnrelatedEditsDoNot() {
        val dependency = myFixture.addFileToProject("dep.qnt", "module dep { val y = 1 }")
        val root = myFixture.addFileToProject("root.qnt", "module root { import dep.* from \"dep\" val x = y }")
        assertNotNull(runPass(root))
        val unrelated = myFixture.addFileToProject("other.qnt", "module other { val z = 3 }")
        val otherDoc = FileDocumentManager.getInstance().getDocument(unrelated.virtualFile)!!
        WriteCommandAction.runWriteCommandAction(project) { otherDoc.setText("module other { val z = 4 }") }
        assertTrue(QuintExternalAnnotator().collectInformation(root)!!.skipTypecheck)
        val depDoc = FileDocumentManager.getInstance().getDocument(dependency.virtualFile)!!
        WriteCommandAction.runWriteCommandAction(project) { depDoc.setText("module dep { val y = 2 }") }
        assertFalse(QuintExternalAnnotator().collectInformation(root)!!.skipTypecheck)
    }

    fun testOldRequestCannotReplaceNewerCheck() {
        myFixture.configureByText("a.qnt", "module a { val x = 1 }")
        val annotator = QuintExternalAnnotator()
        val older = annotator.collectForManualCheck(myFixture.file)!!
        val newer = annotator.collectForManualCheck(myFixture.file)!!
        stubResult = resultWithError("newer")
        assertNotNull(annotator.doAnnotate(newer))
        stubResult = resultWithError("older")
        assertNull(annotator.doAnnotate(older))
        val state = QuintCheckingService.getInstance(project).state(myFixture.file.virtualFile)!!
        assertEquals("newer", state.result!!.errors.single().explanation)
    }

    fun testDependencyEditWhileCheckingRejectsResult() {
        val dependency = myFixture.addFileToProject("dep.qnt", "module dep { val y = 1 }")
        val root = myFixture.addFileToProject("root.qnt", "module root { import dep.* from \"dep\" val x = y }")
        val annotator = QuintExternalAnnotator()
        val input = annotator.collectInformation(root)!!
        val doc = FileDocumentManager.getInstance().getDocument(dependency.virtualFile)!!
        WriteCommandAction.runWriteCommandAction(project) { doc.setText("module dep { val y = 2 }") }
        assertNull(annotator.doAnnotate(input))
        assertEquals(QuintCheckingStatus.STALE, QuintCheckingService.getInstance(project).state(root.virtualFile)!!.status)
    }

    fun testManualCheckingWorksWithBackgroundDisabledAndReportsTimeout() {
        myFixture.configureByText("a.qnt", "module a { val x = 1 }")
        val settings = QuintSettingsState.getInstance()
        settings.backgroundChecking = false
        try {
            val annotator = QuintExternalAnnotator()
            assertNull(annotator.doAnnotate(annotator.collectInformation(myFixture.file)))
            QuintExternalAnnotator.toolRunnerFactory = {
                object : QuintToolRunner {
                    override fun typecheck(filePath: String): QuintTypecheckResult =
                        throw QuintToolFailure(QuintCheckingStatus.TIMEOUT, "deadline exceeded")
                }
            }
            assertNull(annotator.doAnnotate(annotator.collectForManualCheck(myFixture.file)))
            val state = QuintCheckingService.getInstance(project).state(myFixture.file.virtualFile)!!
            assertEquals(QuintCheckingStatus.TIMEOUT, state.status)
            assertEquals("deadline exceeded", state.message)
            assertNull(state.result)
        } finally { settings.backgroundChecking = true }
    }

    fun testCancelledManualCheckDoesNotRemainChecking() {
        myFixture.configureByText("a.qnt", "module a { val x = 1 }")
        QuintExternalAnnotator.toolRunnerFactory = {
            object : QuintToolRunner {
                override fun typecheck(filePath: String): QuintTypecheckResult = throw com.intellij.openapi.progress.ProcessCanceledException()
            }
        }
        val annotator = QuintExternalAnnotator()
        try {
            annotator.doAnnotate(annotator.collectForManualCheck(myFixture.file))
            fail("cancellation must propagate")
        } catch (_: com.intellij.openapi.progress.ProcessCanceledException) {
            assertEquals(QuintCheckingStatus.STALE, QuintCheckingService.getInstance(project).state(myFixture.file.virtualFile)!!.status)
        }
    }

    fun testUnavailableExecutableNeverBecomesCurrentSuccess() {
        myFixture.configureByText("a.qnt", "module a { val x = 1 }")
        QuintSettingsState.getInstance().quintBinaryPath = "/missing/quint-executable"
        QuintExternalAnnotator.toolRunnerFactory = null
        val annotator = QuintExternalAnnotator()
        assertNull(annotator.doAnnotate(annotator.collectForManualCheck(myFixture.file)))
        assertEquals(QuintCheckingStatus.UNAVAILABLE, QuintCheckingService.getInstance(project).state(myFixture.file.virtualFile)!!.status)
    }

    fun testOverlappingRootsKeepIndependentCapturedInputs() {
        val dependency = myFixture.addFileToProject("b.qnt", "module B { pure val value = 2 }")
        val root = myFixture.addFileToProject("a.qnt", "module A { import B.* from \"b\" pure val result = value }")
        val annotator = QuintExternalAnnotator()
        lateinit var second: QuintAnnotatorInput
        var firstWorkspace: File? = null
        QuintExternalAnnotator.toolRunnerFactory = {
            object : QuintToolRunner {
                override fun typecheck(filePath: String): QuintTypecheckResult {
                    firstWorkspace = File(filePath).parentFile
                    val imported = File(firstWorkspace, "b.qnt")
                    assertEquals("module B { pure val value = 2 }", imported.readText())
                    assertNotNull(annotator.doAnnotate(second))
                    assertEquals("module B { pure val value = 2 }", imported.readText())
                    return emptyResult()
                }
            }
        }
        val first = annotator.collectForManualCheck(root)!!
        val document = FileDocumentManager.getInstance().getDocument(dependency.virtualFile)!!
        WriteCommandAction.runWriteCommandAction(project) { document.setText("module B { pure val value = 3 }") }
        QuintExternalAnnotator.toolRunnerFactory = {
            object : QuintToolRunner {
                override fun typecheck(filePath: String): QuintTypecheckResult {
                    assertFalse(firstWorkspace == File(filePath).parentFile)
                    assertEquals("module B { pure val value = 3 }", File(filePath).readText())
                    return emptyResult()
                }
            }
        }
        second = annotator.collectForManualCheck(dependency)!!
        assertNull("first root became stale, but its input remained isolated", annotator.doAnnotate(first))
        assertFalse(firstWorkspace!!.exists())
    }

    fun testTypecheckWorkspaceLivesOutsideUserRepo() {
        val sourceDir = Files.createTempDirectory("quint-test-src-").toFile()
        try {
            File(sourceDir, "battle.qnt").writeText("module battle { import helpers.* from \"helpers\" val x = y }")
            File(sourceDir, "helpers.qnt").writeText("module helpers { val y = 2 }")
            stubResult = resultWithError("oops")

            QuintExternalAnnotator.toolRunnerFactory = {
                object : QuintToolRunner {
                    override fun typecheck(filePath: String): QuintTypecheckResult {
                        invocations.add(filePath)
                        assertTrue(File(File(filePath).parentFile, "helpers.qnt").exists())
                        return stubResult
                    }
                }
            }
            val target = File(sourceDir, "battle.qnt")
            val vfile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(target)!!
            val psi = PsiManager.getInstance(project).findFile(vfile)!!
            runPass(psi)

            assertEquals(1, invocations.size)
            val invoked = File(invocations.single())
            assertFalse(
                "quint path must NOT be inside the user's source dir: $invoked",
                invoked.canonicalPath.startsWith(sourceDir.canonicalPath)
            )
            assertEquals("battle.qnt", invoked.name)
            assertFalse("owned checking workspace must be removed", invoked.parentFile.exists())
            assertFalse(
                "no temp file may leak into the user's source dir",
                sourceDir.listFiles { f -> f.name.startsWith(".quint-idea-") }?.any() ?: false
            )
        } finally {
            sourceDir.deleteRecursively()
        }
    }
    fun testExecutableReplacementRejectsInFlightResultAndInvalidatesCache() {
        val executable = Files.createTempFile("quint-toolchain-", ".sh").toFile()
        try {
            executable.writeText("#!/bin/sh\nexit 0\n")
            assertTrue(executable.setExecutable(true))
            QuintSettingsState.getInstance().quintBinaryPath = executable.path
            myFixture.configureByText("a.qnt", "module a { val x = 1 }")
            assertNotNull(runPass())
            val annotator = QuintExternalAnnotator()
            val input = annotator.collectForManualCheck(myFixture.file)!!
            executable.writeText("#!/bin/sh\nexit 1\n")
            assertNull("a result belongs to the captured executable", annotator.doAnnotate(input))
            assertEquals(QuintCheckingStatus.STALE, QuintCheckingService.getInstance(project).state(myFixture.file.virtualFile)!!.status)
            assertFalse(annotator.collectInformation(myFixture.file)!!.skipTypecheck)
        } finally { executable.delete() }
    }

    fun testImportSymlinkRetargetingRejectsInFlightResult() {
        val directory = Files.createTempDirectory("quint-import-target-").toFile()
        try {
            val first = File(directory, "first.qnt").apply { writeText("module dep { val y = 1 }") }
            val second = File(directory, "second.qnt").apply { writeText(first.readText()) }
            val imported = File(directory, "dep.qnt").toPath()
            Files.createSymbolicLink(imported, first.toPath())
            val root = File(directory, "root.qnt").apply { writeText("module root { import dep.* from \"dep\" val x = y }") }
            val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(root)!!
            val psi = PsiManager.getInstance(project).findFile(virtualFile)!!
            val annotator = QuintExternalAnnotator()
            val input = annotator.collectForManualCheck(psi)!!
            Files.delete(imported)
            Files.createSymbolicLink(imported, second.toPath())
            assertNull("equal contents cannot hide a changed import identity", annotator.doAnnotate(input))
            assertEquals(QuintCheckingStatus.STALE, QuintCheckingService.getInstance(project).state(virtualFile)!!.status)
        } finally { directory.deleteRecursively() }
    }
    fun testInferredHoverDoesNotPresentDependencyStaleTypes() {
        val dependency = myFixture.addFileToProject("dep.qnt", "module dep { pure val y = 1 }")
        myFixture.configureByText("root.qnt", "module root { import dep.* from \"dep\" pure val x = y pure val z = <caret>x }")
        stubResult = QuintTypecheckResult(
            "typechecking", emptyList(), emptyList(),
            listOf(QuintModule(10, "root", listOf(QuintDeclaration(1, "def", "x", "pureval", null, null)))),
            mapOf("1" to QuintTypeScheme(emptyList(), emptyList(), QuintTypeNode("int", null, null, null, null)))
        )
        assertNotNull(runPass())
        val declaration = myFixture.getReferenceAtCaretPosition()!!.resolve()!!
        val documentation = com.dearlordylord.quint.idea.documentation.QuintDocumentationProvider()
        assertTrue(documentation.generateDoc(declaration, null)!!.contains("x: int"))
        val document = FileDocumentManager.getInstance().getDocument(dependency.virtualFile)!!
        WriteCommandAction.runWriteCommandAction(project) { document.setText("module dep { pure val y = true }") }
        assertNull("retained inferred types must not appear current in hover", documentation.generateDoc(declaration, null))
    }
}

