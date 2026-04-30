package com.dearlordylord.quint.idea.documentation

import com.dearlordylord.quint.idea.QuintVocabulary
import com.dearlordylord.quint.idea.annotator.QuintTypeInfo
import com.dearlordylord.quint.idea.annotator.QuintResolvedTypeInfo
import com.dearlordylord.quint.idea.psi.QuintNamedElement
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

        return QuintTypeInfo.formattedTypeFor(declaration, element)?.toLocalTypeInfo()
    }

    private fun buildHtml(info: TypeInfo): String {
        val name = StringUtil.escapeXmlEntities(info.name)
        val typeStr = StringUtil.escapeXmlEntities(info.typeString)
        val qualifierHtml = info.qualifier?.let { "<b>${StringUtil.escapeXmlEntities(it)}</b> " } ?: ""
        return "<html><body><pre>$qualifierHtml$name: $typeStr</pre></body></html>"
    }

    private fun resolveBuiltinInfo(element: PsiElement): TypeInfo? {
        val name = element.text ?: return null
        val info = QuintVocabulary.builtinInfo(name)
            ?: return null
        return TypeInfo(name, info.category, info.signature)
    }

    private fun QuintResolvedTypeInfo.toLocalTypeInfo(): TypeInfo =
        TypeInfo(name, qualifier, typeString)

    private data class TypeInfo(
        val name: String,
        val qualifier: String?,
        val typeString: String
    )
}
