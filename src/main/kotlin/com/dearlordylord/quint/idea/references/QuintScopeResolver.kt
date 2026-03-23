package com.dearlordylord.quint.idea.references

import com.dearlordylord.quint.idea.parser.QuintParser
import com.dearlordylord.quint.idea.psi.QuintNamedElement
import com.dearlordylord.quint.idea.psi.QuintPsiUtils
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil
import org.antlr.intellij.adaptor.lexer.RuleIElementType

object QuintScopeResolver {

    fun findVisibleDeclarations(position: PsiElement): List<PsiNamedElement> {
        val result = mutableListOf<PsiNamedElement>()
        var current = position.parent

        while (current != null) {
            val type = current.node?.elementType as? RuleIElementType
            if (type != null) {
                when (type.ruleIndex) {
                    QuintParser.RULE_operDef -> {
                        collectOperDefParams(current, result)
                    }
                    QuintParser.RULE_lambdaUnsugared, QuintParser.RULE_lambdaTupleSugar -> {
                        collectLambdaParams(current, result)
                    }
                    QuintParser.RULE_expr -> {
                        collectLetInBinding(current, position, result)
                    }
                    QuintParser.RULE_module -> {
                        collectModuleDeclarations(current, result, mutableSetOf())
                    }
                }
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
        var child = operDef.firstChild
        while (child != null) {
            if (child is QuintNamedElement) {
                val childType = child.node?.elementType as? RuleIElementType
                if (childType?.ruleIndex == QuintParser.RULE_parameter ||
                    childType?.ruleIndex == QuintParser.RULE_annotatedParameter
                ) {
                    result.add(child)
                }
            }
            child = child.nextSibling
        }
    }

    private fun collectLambdaParams(lambda: PsiElement, result: MutableList<PsiNamedElement>) {
        var child = lambda.firstChild
        while (child != null) {
            if (child is QuintNamedElement) {
                val childType = child.node?.elementType as? RuleIElementType
                if (childType != null && childType.ruleIndex == QuintParser.RULE_parameter) {
                    result.add(child)
                }
            }
            child = child.nextSibling
        }
    }

    private fun collectLetInBinding(expr: PsiElement, position: PsiElement, result: MutableList<PsiNamedElement>) {
        // letIn pattern: expr → operDef expr
        val firstChild = expr.firstChild
        if (firstChild is QuintNamedElement) {
            val firstType = firstChild.node?.elementType as? RuleIElementType
            if (firstType != null && firstType.ruleIndex == QuintParser.RULE_operDef) {
                // Only visible if position is in the body expr, not in the operDef itself
                if (!PsiTreeUtil.isAncestor(firstChild, position, false)) {
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
        var child = module.firstChild
        while (child != null) {
            val childType = child.node?.elementType as? RuleIElementType
            if (childType != null && childType.ruleIndex == QuintParser.RULE_documentedDeclaration) {
                var declChild = child.firstChild
                while (declChild != null) {
                    val declType = declChild.node?.elementType as? RuleIElementType
                    if (declType != null && declType.ruleIndex == QuintParser.RULE_declaration) {
                        if (declChild is QuintNamedElement) {
                            result.add(declChild)
                        } else {
                            var inner = declChild.firstChild
                            while (inner != null) {
                                if (inner is QuintNamedElement) {
                                    result.add(inner)
                                } else {
                                    val innerType = inner.node?.elementType as? RuleIElementType
                                    if (innerType?.ruleIndex == QuintParser.RULE_importMod) {
                                        collectImportedDeclarations(inner, module, result, visiting)
                                    }
                                }
                                inner = inner.nextSibling
                            }
                        }
                    }
                    declChild = declChild.nextSibling
                }
            }
            child = child.nextSibling
        }
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

        when (importInfo.kind) {
            ImportKind.WILDCARD -> result.addAll(imported)
            ImportKind.SPECIFIC -> imported.firstOrNull { it.name == importInfo.specificName }
                ?.let { result.add(it) }
            else -> {} // QUALIFIED/ALIASED handled by QuintReference
        }
    }
}
