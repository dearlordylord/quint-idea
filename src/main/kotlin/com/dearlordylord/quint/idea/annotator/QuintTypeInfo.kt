package com.dearlordylord.quint.idea.annotator

import com.dearlordylord.quint.idea.psi.QuintNamedElement
import com.dearlordylord.quint.idea.psi.QuintPsiUtils
import com.intellij.psi.PsiElement

data class QuintResolvedTypeInfo(
    val name: String,
    val qualifier: String?,
    val typeString: String
)

object QuintTypeInfo {

    fun formattedTypeFor(declaration: QuintNamedElement, context: PsiElement): QuintResolvedTypeInfo? {
        val declName = declaration.name ?: return null
        val qualifier = declarationQualifier(declaration)

        annotatedParameterType(declaration)?.let { annotationType ->
            return QuintResolvedTypeInfo(declName, qualifier, annotationType)
        }

        val scheme = typeSchemeFor(declaration, context) ?: return null
        return QuintResolvedTypeInfo(declName, qualifier, QuintTypeFormatter.formatScheme(scheme))
    }

    fun typeForDeclaration(declaration: PsiElement, context: PsiElement): QuintTypeNode? {
        annotatedParameterTypeNode(declaration, context)?.let { return it }
        return typeSchemeFor(declaration, context)?.type
    }

    fun typeSchemeFor(declaration: PsiElement, context: PsiElement): QuintTypeScheme? {
        val declName = QuintPsiUtils.getDeclarationName(declaration)
            ?: QuintPsiUtils.getDeclarationName(declaration.parent)
            ?: return null
        val module = QuintPsiUtils.getContainingModule(declaration) ?: return null
        val moduleName = QuintPsiUtils.getDeclarationName(module) ?: return null
        return typeSchemeByName(declaration, context, moduleName, declName)
    }

    fun namedTypeInDeclarationModule(
        typeName: String,
        declaration: PsiElement,
        context: PsiElement
    ): QuintTypeNode? {
        val module = QuintPsiUtils.getContainingModule(declaration) ?: return null
        val moduleName = QuintPsiUtils.getDeclarationName(module) ?: return null
        return typeSchemeByName(declaration, context, moduleName, typeName)?.type
    }

    private fun annotatedParameterTypeNode(declaration: PsiElement, context: PsiElement): QuintTypeNode? {
        val typeName = QuintPsiUtils.getAnnotatedParameterTypeNode(declaration)?.text ?: return null
        return namedTypeInDeclarationModule(typeName, declaration, context)
    }

    private fun annotatedParameterType(declaration: PsiElement): String? {
        return QuintPsiUtils.getAnnotatedParameterTypeNode(declaration)?.text
    }

    private fun declarationQualifier(declaration: QuintNamedElement): String? {
        val parent = declaration.parent
        return parent?.let { QuintPsiUtils.getDeclarationQualifier(it) }
            ?: QuintPsiUtils.getDeclarationQualifier(declaration)
    }

    private fun typeSchemeByName(
        declaration: PsiElement,
        context: PsiElement,
        moduleName: String,
        declName: String
    ): QuintTypeScheme? {
        val declFile = declaration.containingFile?.virtualFile
        val contextFile = context.containingFile?.virtualFile

        return declFile?.let { QuintTypeCache.getTypeScheme(it, moduleName, declName) }
            ?: contextFile?.takeIf { it != declFile }?.let { QuintTypeCache.getTypeScheme(it, moduleName, declName) }
    }
}
