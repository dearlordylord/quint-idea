package com.dearlordylord.quint.idea.references

import com.dearlordylord.quint.idea.parser.QuintParser
import com.dearlordylord.quint.idea.psi.QuintNamedElement
import com.dearlordylord.quint.idea.psi.QuintPsiShape
import com.dearlordylord.quint.idea.psi.QuintPsiUtils
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement

/** Spelling in the current scope is separate from the physical declaration identity. */
data class QuintVisibleSymbol(val name: String, val declaration: PsiNamedElement)

object QuintScopeResolver {
    fun findVisibleDeclarations(position: PsiElement): List<PsiNamedElement> =
        findVisibleSymbols(position).filter { it.name == it.declaration.name }.map { it.declaration }

    fun findVisibleSymbols(position: PsiElement, sourceContext: PsiElement = position): List<QuintVisibleSymbol> {
        val result = mutableListOf<QuintVisibleSymbol>()
        fun add(declaration: PsiNamedElement) {
            declaration.name?.takeIf { it != "_" }?.let { result.add(QuintVisibleSymbol(it, declaration)) }
        }
        var current = position.parent
        while (current != null) {
            when (QuintPsiShape.ruleIndex(current)) {
                QuintParser.RULE_operDef -> {
                    (QuintPsiShape.parameterNodes(current) + QuintPsiShape.annotatedParameterNodes(current)).filterIsInstance<QuintNamedElement>().forEach(::add)
                }
                QuintParser.RULE_lambdaUnsugared, QuintParser.RULE_lambdaTupleSugar ->
                    QuintPsiShape.parameterNodes(current).filterIsInstance<QuintNamedElement>().forEach(::add)
                QuintParser.RULE_matchSumCase -> {
                    val variant = QuintPsiUtils.findFirstChildOfRule(current, QuintParser.RULE_matchSumVariant)
                    if (variant != null) QuintPsiShape.directChildrenOfRule(variant, QuintParser.RULE_simpleId).filterIsInstance<QuintNamedElement>().forEach(::add)
                }
                QuintParser.RULE_expr -> {
                    val first = current.firstChild
                    if (first is QuintNamedElement && QuintPsiShape.isOperationDefinition(first) && !QuintPsiShape.isAncestor(first, position)) add(first)
                }
                QuintParser.RULE_module -> {
                    val physical = QuintPsiUtils.getContainingModule(sourceContext) ?: current
                    result.addAll(moduleSymbols(physical, false, mutableSetOf()))
                }
            }
            current = current.parent
        }
        // Walk-up order preserves the nearest binder for completion and resolution alike.
        return result.distinctBy { it.name }
    }

    fun findModuleLevelDeclarations(module: PsiElement): List<PsiNamedElement> =
        exportedSymbols(module).filter { it.name == it.declaration.name }.map { it.declaration }

    fun exportedSymbols(module: PsiElement): List<QuintVisibleSymbol> = moduleSymbols(module, true, mutableSetOf())

    private fun moduleSymbols(module: PsiElement, exportedOnly: Boolean, visiting: MutableSet<Pair<PsiElement, Boolean>>): List<QuintVisibleSymbol> {
        val key = module to exportedOnly
        if (!visiting.add(key)) return emptyList()
        try {
            val result = mutableListOf<QuintVisibleSymbol>()
            for (declaration in QuintPsiUtils.findDeclarations(module)) {
                val own = if (declaration is QuintNamedElement) declaration else declaration.children.filterIsInstance<QuintNamedElement>().firstOrNull()
                if (own != null) own.name?.let { result.add(QuintVisibleSymbol(it, own)) }
                for (node in declaration.children) {
                    val rule = QuintPsiShape.ruleIndex(node)
                    val export = rule == QuintParser.RULE_exportMod
                    if (!export && (exportedOnly || rule !in setOf(QuintParser.RULE_importMod, QuintParser.RULE_instanceMod))) continue
                    val info = if (export) exportInfo(node) else QuintImportResolver.extractImportInfo(node)
                    if (info == null) continue
                    val file = module.containingFile ?: continue
                    val aliasImport = if (export) QuintImportResolver.findImportsInModule(module).firstOrNull { (it.alias ?: it.moduleName) == info.moduleName } else null
                    val target = QuintImportResolver.findModule(aliasImport?.moduleName ?: info.moduleName, aliasImport?.fromSource ?: info.fromSource, file) ?: continue
                    val surface = moduleSymbols(target, true, visiting)
                    result.addAll(qualify(surface, info))
                }
            }
            return result.distinctBy { it.name }
        } finally { visiting.remove(key) }
    }

    private fun qualify(symbols: List<QuintVisibleSymbol>, info: ImportInfo): List<QuintVisibleSymbol> = when (info.kind) {
        ImportKind.WILDCARD -> symbols
        ImportKind.SPECIFIC -> symbols.filter { it.name == info.specificName }
        ImportKind.QUALIFIED, ImportKind.ALIASED -> symbols.map { it.copy(name = "${info.alias ?: info.moduleName}::${it.name}") }
    }

    private fun exportInfo(node: PsiElement): ImportInfo? {
        val names = QuintPsiShape.nameNodes(node)
        val module = names.firstOrNull()?.text ?: return null
        val selected = QuintPsiUtils.findFirstChildOfRule(node, QuintParser.RULE_identOrStar)?.text
        return when {
            selected == "*" -> ImportInfo(module, ImportKind.WILDCARD)
            selected != null -> ImportInfo(module, ImportKind.SPECIFIC, specificName = selected)
            names.size > 1 -> ImportInfo(module, ImportKind.ALIASED, alias = names[1].text)
            else -> ImportInfo(module, ImportKind.QUALIFIED)
        }
    }
}
