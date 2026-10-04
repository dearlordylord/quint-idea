package com.dearlordylord.quint.idea.annotator

import com.dearlordylord.quint.idea.parser.QuintParser
import com.dearlordylord.quint.idea.psi.QuintPsiShape
import com.dearlordylord.quint.idea.psi.QuintPsiUtils
import com.intellij.psi.PsiElement

/** Source annotations retain exact field declarations even without compiler analysis. */
object QuintSourceTypes {
    fun annotation(declaration: PsiElement): PsiElement? = QuintPsiUtils.findFirstChildOfRule(declaration, QuintParser.RULE_type)

    fun typeForDeclaration(declaration: PsiElement): QuintTypeNode? {
        val result = annotation(declaration)?.let { type(it, mutableSetOf()) } ?: return null
        if (!QuintPsiShape.isRule(declaration, QuintParser.RULE_operDef)) return result
        val header = generateSequence(declaration.firstChild) { it.nextSibling }.takeWhile { it.text != "=" }
        if (header.none { it.text == "(" }) return result
        val parameters = QuintPsiShape.directChildrenOfRule(declaration, QuintParser.RULE_annotatedParameter) +
            QuintPsiShape.directChildrenOfRule(declaration, QuintParser.RULE_parameter)
        val arguments = parameters.map { parameter ->
            annotation(parameter)?.let { type(it, mutableSetOf()) } ?: QuintTypeNode.UNKNOWN
        }
        return QuintTypeNode("oper", null, null, arguments, result)
    }

    fun recordRow(annotation: PsiElement, visiting: MutableSet<PsiElement> = mutableSetOf()): PsiElement? {
        if (!visiting.add(annotation)) return null
        QuintPsiUtils.findFirstChildOfRule(annotation, QuintParser.RULE_row)?.let { return it }
        val name = QuintPsiUtils.findFirstChildOfRule(annotation, QuintParser.RULE_qualId)
        if (name != null) {
            val declaration = name.reference?.resolve() ?: return null
            return this.annotation(declaration)?.let { recordRow(it, visiting) }
        }
        val inner = QuintPsiShape.directChildrenOfRule(annotation, QuintParser.RULE_type).singleOrNull()
        return if (annotation.firstChild?.text == "(") inner?.let { recordRow(it, visiting) } else null
    }

    private fun type(node: PsiElement, visiting: MutableSet<PsiElement>): QuintTypeNode? {
        if (!visiting.add(node)) return null
        try {
            if (node.text in setOf("int", "str", "bool")) return QuintTypeNode(node.text, null, null, null, null)
            val row = QuintPsiUtils.findFirstChildOfRule(node, QuintParser.RULE_row)
            if (row != null) {
                val labels = QuintPsiShape.directChildrenOfRule(row, QuintParser.RULE_rowLabel)
                val types = QuintPsiShape.directChildrenOfRule(row, QuintParser.RULE_type)
                val fields = labels.zip(types).map { (label, fieldType) -> QuintFieldNode(label.text, type(fieldType, visiting) ?: QuintTypeNode.UNKNOWN) }
                return QuintTypeNode("rec", null, QuintRowNode("row", fields, null), null, null)
            }
            val name = QuintPsiUtils.findFirstChildOfRule(node, QuintParser.RULE_qualId)
            if (name != null) {
                val declaration = name.reference?.resolve()
                val definition = declaration?.let { annotation(it) }
                return definition?.let { type(it, visiting) }
            }
            val children = QuintPsiShape.directChildrenOfRule(node, QuintParser.RULE_type)
            if (children.size == 1 && node.firstChild.text == "(") return type(children.single(), visiting)
            return null
        } finally { visiting.remove(node) }
    }
}
