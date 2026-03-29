package com.dearlordylord.quint.idea.documentation

import com.dearlordylord.quint.idea.annotator.QuintTypeCache
import com.dearlordylord.quint.idea.completion.QuintCompletionContributor
import com.dearlordylord.quint.idea.psi.QuintNamedElement
import com.dearlordylord.quint.idea.psi.QuintPsiUtils
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement

class QuintDocumentationProvider : AbstractDocumentationProvider() {

    override fun generateDoc(element: PsiElement, originalElement: PsiElement?): String? {
        val info = resolveTypeInfo(element) ?: return null
        return buildHtml(info)
    }

    override fun getQuickNavigateInfo(element: PsiElement, originalElement: PsiElement?): String? {
        val info = resolveTypeInfo(element) ?: return null
        val prefix = if (info.qualifier != null) "${info.qualifier} " else ""
        return "$prefix${info.name}: ${info.typeString}"
    }

    private fun resolveTypeInfo(element: PsiElement): TypeInfo? {
        val declaration = when (element) {
            is QuintNamedElement -> element
            else -> {
                val ref = element.reference
                ref?.resolve() as? QuintNamedElement
            }
        } ?: return resolveBuiltinInfo(element)

        val declName = declaration.name ?: return null
        val qualifier = getQualifier(declaration)
        val moduleName = findModuleName(declaration) ?: return null

        // Look up the type in the cache — try the declaration's file first,
        // then the hover site's file (cross-file imports store types on the importing file)
        val declFile = declaration.containingFile?.virtualFile
        val hoverFile = element.containingFile?.virtualFile

        val typeStr = (declFile?.let { QuintTypeCache.getFormattedType(it, moduleName, declName) })
            ?: (hoverFile?.takeIf { it != declFile }?.let { QuintTypeCache.getFormattedType(it, moduleName, declName) })
            ?: return null

        return TypeInfo(declName, qualifier, typeStr)
    }

    private fun findModuleName(element: PsiElement): String? {
        val module = QuintPsiUtils.getContainingModule(element) ?: return null
        return QuintPsiUtils.getDeclarationName(module)
    }

    private fun getQualifier(declaration: QuintNamedElement): String? {
        val parent = declaration.parent ?: return null
        return QuintPsiUtils.getDeclarationQualifier(parent)
            ?: QuintPsiUtils.getDeclarationQualifier(declaration)
    }

    private fun buildHtml(info: TypeInfo): String {
        val name = StringUtil.escapeXmlEntities(info.name)
        val typeStr = StringUtil.escapeXmlEntities(info.typeString)
        val qualifierHtml = info.qualifier?.let { "<b>${StringUtil.escapeXmlEntities(it)}</b> " } ?: ""
        return "<html><body><pre>$qualifierHtml$name: $typeStr</pre></body></html>"
    }

    private fun resolveBuiltinInfo(element: PsiElement): TypeInfo? {
        val name = element.text ?: return null
        val info = QuintCompletionContributor.BUILTIN_OPERATORS[name]
            ?: QuintCompletionContributor.BUILTIN_VALUES[name]
            ?: return null
        return TypeInfo(name, info.category, info.signature)
    }

    private data class TypeInfo(
        val name: String,
        val qualifier: String?,
        val typeString: String
    )
}
