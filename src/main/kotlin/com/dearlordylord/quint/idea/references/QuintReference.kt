package com.dearlordylord.quint.idea.references

import com.dearlordylord.quint.idea.parser.QuintParser
import com.dearlordylord.quint.idea.psi.QuintPsiUtils
import com.intellij.openapi.util.TextRange
import org.antlr.intellij.adaptor.lexer.RuleIElementType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.util.IncorrectOperationException

class QuintReference(element: PsiElement, textRange: TextRange) :
    PsiReferenceBase<PsiElement>(element, textRange) {

    override fun resolve(): PsiElement? {
        val name = element.text

        // Handle qualified names (Foo::bar)
        if ("::" in name) {
            return resolveQualified(name)
        }

        // Handle instance parameter bindings: import M(PARAM = expr).*
        val instanceParam = resolveInstanceParam(name)
        if (instanceParam != null) return instanceParam

        val declarations = QuintScopeResolver.findVisibleDeclarations(element)
        val scopeResult = declarations.firstOrNull { it.name == name }
        if (scopeResult != null) return scopeResult

        // Fallback: if inside nameAfterDot, try resolving as a record field
        return resolveAsRecordField(name)
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val currentText = element.text
        val newText = if ("::" in currentText) {
            val prefix = currentText.substringBeforeLast("::")
            "$prefix::$newElementName"
        } else {
            newElementName
        }
        val newId = QuintPsiUtils.createQualIdFromText(element.project, newText)
            ?: throw IncorrectOperationException("Cannot create identifier")
        element.node.treeParent.replaceChild(element.node, newId.node)
        return newId
    }

    override fun getVariants(): Array<Any> = emptyArray()

    private fun resolveAsRecordField(name: String): PsiElement? {
        val parent = element.parent ?: return null
        val parentType = parent.node?.elementType as? RuleIElementType ?: return null

        val nameAfterDot = if (parentType.ruleIndex == QuintParser.RULE_nameAfterDot) {
            parent
        } else {
            val gp = parent.parent ?: return null
            val gpType = gp.node?.elementType as? RuleIElementType ?: return null
            if (gpType.ruleIndex == QuintParser.RULE_nameAfterDot) gp else return null
        }

        // nameAfterDot's parent should be the dotCall expr
        val dotCall = nameAfterDot.parent ?: return null
        val fields = QuintRecordTypeResolver.resolveRecordFieldsFromDotCall(dotCall) ?: return null

        if (fields.none { it.fieldName == name }) return null
        return QuintRecordTypeResolver.findFieldDefinition(name, fields, element)
    }

    private fun resolveInstanceParam(name: String): PsiElement? {
        // qualId → name → instanceMod
        val nameNode = element.parent ?: return null
        val nameType = nameNode.node?.elementType as? RuleIElementType ?: return null
        if (nameType.ruleIndex != QuintParser.RULE_name) return null

        val instanceMod = nameNode.parent ?: return null
        val instanceType = instanceMod.node?.elementType as? RuleIElementType ?: return null
        if (instanceType.ruleIndex != QuintParser.RULE_instanceMod) return null

        val moduleNameNode = QuintPsiUtils.findFirstChildOfRule(instanceMod, QuintParser.RULE_moduleName)
            ?: return null
        return resolveMemberInModule(moduleNameNode.text, name)
    }

    private fun resolveQualified(qualName: String): PsiElement? {
        val parts = qualName.split("::")
        if (parts.size != 2) return null
        return resolveMemberInModule(parts[0], parts[1])
    }

    private fun resolveMemberInModule(moduleName: String, memberName: String): PsiElement? {
        val file = element.containingFile ?: return null

        // 1. Same-file lookup (existing behavior)
        val modules = QuintPsiUtils.findModules(file)
        val sameFileModule = modules.firstOrNull {
            QuintPsiUtils.getDeclarationName(it) == moduleName
        }
        if (sameFileModule != null) {
            val decl = QuintScopeResolver.findModuleLevelDeclarations(sameFileModule)
                .firstOrNull { it.name == memberName }
            if (decl != null) return decl
        }

        // 2. Cross-file: check imports in containing module
        val containingModule = QuintPsiUtils.getContainingModule(element) ?: return null
        val imports = QuintImportResolver.findImportsInModule(containingModule)
        for (imp in imports) {
            val matches = when (imp.kind) {
                ImportKind.QUALIFIED -> imp.moduleName == moduleName
                ImportKind.ALIASED -> imp.alias == moduleName
                else -> false
            }
            if (!matches) continue
            val targetModule = QuintImportResolver.findModule(imp.moduleName, imp.fromSource, file)
                ?: continue
            val decl = QuintScopeResolver.findModuleLevelDeclarations(targetModule)
                .firstOrNull { it.name == memberName }
            if (decl != null) return decl
        }
        return null
    }
}
