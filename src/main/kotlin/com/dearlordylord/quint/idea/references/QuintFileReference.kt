package com.dearlordylord.quint.idea.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReferenceBase

/**
 * Reference from a `fromSource` rule node to the target .qnt file.
 * Enables Cmd+Click navigation on `"./a.qnt"` in `from "./a.qnt"`.
 */
class QuintFileReference(element: PsiElement, range: TextRange) :
    PsiReferenceBase<PsiElement>(element, range, true) {

    override fun resolve(): PsiFile? {
        val path = rangeInElement.substring(element.text)
        if (path.isEmpty()) return null

        val containingVf = element.containingFile?.virtualFile ?: return null
        val baseDir = containingVf.parent ?: return null
        val targetVf = baseDir.findFileByRelativePath(path) ?: return null
        return PsiManager.getInstance(element.project).findFile(targetVf)
    }
}
