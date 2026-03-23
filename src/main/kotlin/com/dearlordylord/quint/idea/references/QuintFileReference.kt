package com.dearlordylord.quint.idea.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReferenceBase

class QuintFileReference(element: PsiElement, range: TextRange) :
    PsiReferenceBase<PsiElement>(element, range, true) {

    override fun resolve(): PsiFile? {
        val path = rangeInElement.substring(element.text)
        if (path.isEmpty()) return null
        val containingFile = element.containingFile ?: return null
        return QuintImportResolver.resolveFromSource(path, containingFile)
    }
}
