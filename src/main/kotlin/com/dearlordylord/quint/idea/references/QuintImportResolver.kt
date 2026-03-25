package com.dearlordylord.quint.idea.references

import com.dearlordylord.quint.idea.parser.QuintParser
import com.dearlordylord.quint.idea.psi.QuintPsiUtils
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.antlr.intellij.adaptor.lexer.RuleIElementType

enum class ImportKind { QUALIFIED, WILDCARD, SPECIFIC, ALIASED }

data class ImportInfo(
    val moduleName: String,
    val kind: ImportKind,
    val alias: String? = null,
    val specificName: String? = null,
    val fromSource: String? = null
)

object QuintImportResolver {

    fun extractImportInfo(importMod: PsiElement): ImportInfo? {
        val type = importMod.node?.elementType as? RuleIElementType ?: return null
        if (type.ruleIndex != QuintParser.RULE_importMod) return null

        val identOrStar = QuintPsiUtils.findFirstChildOfRule(importMod, QuintParser.RULE_identOrStar)
        val fromSourceNode = QuintPsiUtils.findFirstChildOfRule(importMod, QuintParser.RULE_fromSource)
        val fromSource = fromSourceNode?.text?.removeSurrounding("\"")

        if (identOrStar != null) {
            val nameNode = QuintPsiUtils.findFirstChildOfRule(importMod, QuintParser.RULE_name)
                ?: return null
            val moduleName = nameNode.text
            return if (identOrStar.text == "*") {
                ImportInfo(moduleName, ImportKind.WILDCARD, fromSource = fromSource)
            } else {
                ImportInfo(moduleName, ImportKind.SPECIFIC, specificName = identOrStar.text, fromSource = fromSource)
            }
        } else {
            val nameNodes = collectChildrenOfRule(importMod, QuintParser.RULE_name)
            if (nameNodes.isEmpty()) return null
            val moduleName = nameNodes[0].text

            return if (nameNodes.size >= 2) {
                ImportInfo(moduleName, ImportKind.ALIASED, alias = nameNodes[1].text, fromSource = fromSource)
            } else {
                ImportInfo(moduleName, ImportKind.QUALIFIED, fromSource = fromSource)
            }
        }
    }

    fun resolveFromSource(fromSource: String, contextFile: PsiFile): PsiFile? {
        val contextVf = contextFile.virtualFile ?: return null
        val parentDir = contextVf.parent ?: return null
        // Quint compiler unconditionally appends .qnt to fromSource paths
        val pathWithExt = if (fromSource.endsWith(".qnt")) fromSource else "$fromSource.qnt"
        val vf = parentDir.findFileByRelativePath(pathWithExt) ?: return null
        return PsiManager.getInstance(contextFile.project).findFile(vf)
    }

    fun findModule(moduleName: String, fromSource: String?, contextFile: PsiFile): PsiElement? {
        val targetFile = if (fromSource != null) {
            resolveFromSource(fromSource, contextFile) ?: return null
        } else {
            contextFile
        }
        return QuintPsiUtils.findModules(targetFile).firstOrNull {
            QuintPsiUtils.getDeclarationName(it) == moduleName
        }
    }

    fun findImportsInModule(module: PsiElement): List<ImportInfo> {
        return QuintPsiUtils.findChildrenOfRule(module, QuintParser.RULE_importMod)
            .mapNotNull { extractImportInfo(it) }
    }

    // Non-recursive: collects only direct children, unlike QuintPsiUtils.findChildrenOfRule
    private fun collectChildrenOfRule(parent: PsiElement, ruleIndex: Int): List<PsiElement> {
        val result = mutableListOf<PsiElement>()
        var child = parent.firstChild
        while (child != null) {
            val childType = child.node?.elementType as? RuleIElementType
            if (childType != null && childType.ruleIndex == ruleIndex) {
                result.add(child)
            }
            child = child.nextSibling
        }
        return result
    }
}
