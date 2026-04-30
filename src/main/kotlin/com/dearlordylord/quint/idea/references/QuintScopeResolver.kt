package com.dearlordylord.quint.idea.references

import com.dearlordylord.quint.idea.parser.QuintParser
import com.dearlordylord.quint.idea.psi.QuintNamedElement
import com.dearlordylord.quint.idea.psi.QuintPsiShape
import com.dearlordylord.quint.idea.psi.QuintPsiUtils
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement

object QuintScopeResolver {

    fun findVisibleDeclarations(position: PsiElement): List<PsiNamedElement> {
        val result = mutableListOf<PsiNamedElement>()
        var current = position.parent

        while (current != null) {
            when (QuintPsiShape.ruleIndex(current)) {
                QuintParser.RULE_operDef -> collectOperDefParams(current, result)
                QuintParser.RULE_lambdaUnsugared, QuintParser.RULE_lambdaTupleSugar -> collectLambdaParams(current, result)
                QuintParser.RULE_expr -> collectLetInBinding(current, position, result)
                QuintParser.RULE_module -> collectModuleDeclarations(current, result, mutableSetOf())
            }
            current = current.parent
        }

        return result
    }

    fun findModuleLevelDeclarations(module: PsiElement): List<PsiNamedElement> {
        val result = mutableListOf<PsiNamedElement>()
        collectModuleDeclarations(module, result, mutableSetOf())
        return result
    }

    private fun collectOperDefParams(operDef: PsiElement, result: MutableList<PsiNamedElement>) {
        result.addAll(namedDirectChildren(operDef, QuintParser.RULE_parameter, QuintParser.RULE_annotatedParameter))
    }

    private fun collectLambdaParams(lambda: PsiElement, result: MutableList<PsiNamedElement>) {
        result.addAll(namedDirectChildren(lambda, QuintParser.RULE_parameter))
    }

    private fun collectLetInBinding(expr: PsiElement, position: PsiElement, result: MutableList<PsiNamedElement>) {
        // letIn pattern: expr → operDef expr
        val firstChild = expr.firstChild
        if (firstChild is QuintNamedElement) {
            if (QuintPsiShape.isOperationDefinition(firstChild)) {
                // Only visible if position is in the body expr, not in the operDef itself
                if (!QuintPsiShape.isAncestor(firstChild, position)) {
                    result.add(firstChild)
                }
            }
        }
    }

    private fun collectModuleDeclarations(
        module: PsiElement,
        result: MutableList<PsiNamedElement>,
        visiting: MutableSet<PsiElement>
    ) {
        if (!visiting.add(module)) return // cycle guard
        val documentedDeclarations = QuintPsiShape.documentedDeclarations(module)
        for (documentedDeclaration in documentedDeclarations) {
            val declarationNodes = QuintPsiShape.declarationNodes(documentedDeclaration)
            for (declaration in declarationNodes) {
                collectDeclarationSurface(declaration, module, result, visiting)
            }
        }
    }

    private fun collectDeclarationSurface(
        declaration: PsiElement,
        module: PsiElement,
        result: MutableList<PsiNamedElement>,
        visiting: MutableSet<PsiElement>
    ) {
        if (declaration is QuintNamedElement) {
            result.add(declaration)
            return
        }

        var inner = declaration.firstChild
        while (inner != null) {
            when {
                inner is QuintNamedElement -> result.add(inner)
                QuintPsiShape.isImportModule(inner) ->
                    collectImportedDeclarations(inner, module, result, visiting)
            }
            inner = inner.nextSibling
        }
    }

    private fun namedDirectChildren(parent: PsiElement, vararg ruleIndices: Int): List<PsiNamedElement> {
        val rules = ruleIndices.toSet()
        return QuintPsiShape.parameterNodes(parent)
            .plus(if (QuintParser.RULE_annotatedParameter in rules) QuintPsiShape.annotatedParameterNodes(parent) else emptyList())
            .filterIsInstance<QuintNamedElement>()
            .filter { QuintPsiShape.ruleIndex(it) in rules }
    }

    private fun collectImportedDeclarations(
        importMod: PsiElement,
        module: PsiElement,
        result: MutableList<PsiNamedElement>,
        visiting: MutableSet<PsiElement>
    ) {
        val importInfo = QuintImportResolver.extractImportInfo(importMod) ?: return
        val containingFile = module.containingFile ?: return
        val targetModule = QuintImportResolver.findModule(
            importInfo.moduleName, importInfo.fromSource, containingFile
        ) ?: return

        if (importInfo.kind != ImportKind.WILDCARD && importInfo.kind != ImportKind.SPECIFIC) return

        val imported = mutableListOf<PsiNamedElement>()
        collectModuleDeclarations(targetModule, imported, visiting)
        if (importInfo.kind == ImportKind.WILDCARD) {
            result.addAll(imported)
        } else {
            imported.firstOrNull { it.name == importInfo.specificName }?.let { result.add(it) }
        }
    }
}
