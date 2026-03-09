package com.dearlordylord.quint.idea.psi

import com.dearlordylord.quint.idea.QuintFileType
import com.dearlordylord.quint.idea.QuintLanguage
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider

class QuintFile(viewProvider: FileViewProvider) :
    PsiFileBase(viewProvider, QuintLanguage.INSTANCE) {
    override fun getFileType(): FileType = QuintFileType.INSTANCE
    override fun toString(): String = "Quint File"
}
