package com.dearlordylord.quint.idea.structure

import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.psi.PsiFile

class QuintStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder? = null
}
