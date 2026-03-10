package com.dearlordylord.quint.idea.psi

import com.dearlordylord.quint.idea.parser.QuintParser
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.util.IncorrectOperationException
import org.antlr.intellij.adaptor.lexer.RuleIElementType
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class QuintNamedElement(node: ASTNode) : ANTLRPsiNode(node), PsiNameIdentifierOwner {

    override fun getName(): String? = QuintPsiUtils.getDeclarationName(this)

    override fun getNameIdentifier(): PsiElement? {
        val type = node.elementType as? RuleIElementType ?: return null

        return when (type.ruleIndex) {
            QuintParser.RULE_operDef -> {
                val callName = QuintPsiUtils.findFirstChildOfRule(this, QuintParser.RULE_normalCallName)
                callName?.let { QuintPsiUtils.findFirstChildOfRule(it, QuintParser.RULE_qualId) }
            }
            QuintParser.RULE_typeDef -> {
                QuintPsiUtils.findFirstChildOfRule(this, QuintParser.RULE_qualId)
                    ?: QuintPsiUtils.findFirstChildOfRule(this, QuintParser.RULE_typeDefHead)?.let {
                        QuintPsiUtils.findFirstChildOfRule(it, QuintParser.RULE_qualId)
                    }
            }
            QuintParser.RULE_module -> {
                QuintPsiUtils.findFirstChildOfRule(this, QuintParser.RULE_qualId)
            }
            QuintParser.RULE_parameter, QuintParser.RULE_annotatedParameter -> {
                QuintPsiUtils.findFirstChildOfRule(this, QuintParser.RULE_identOrHole)?.let {
                    QuintPsiUtils.findFirstChildOfRule(it, QuintParser.RULE_qualId)
                }
            }
            QuintParser.RULE_declaration -> {
                // const/var: qualId is direct child; assume: identOrHole → qualId
                QuintPsiUtils.findFirstChildOfRule(this, QuintParser.RULE_qualId)
                    ?: QuintPsiUtils.findFirstChildOfRule(this, QuintParser.RULE_identOrHole)?.let {
                        QuintPsiUtils.findFirstChildOfRule(it, QuintParser.RULE_qualId)
                    }
            }
            else -> null
        }
    }

    override fun setName(name: String): PsiElement {
        val nameId = nameIdentifier ?: throw IncorrectOperationException("No name identifier")
        val newId = QuintPsiUtils.createQualIdFromText(project, name)
            ?: throw IncorrectOperationException("Cannot create identifier")
        nameId.node.treeParent.replaceChild(nameId.node, newId.node)
        return this
    }
}
