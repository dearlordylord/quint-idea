package com.dearlordylord.quint.idea.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.psi.PsiFile

class QuintExternalAnnotator : ExternalAnnotator<Unit, Unit>() {
    override fun collectInformation(file: PsiFile) {}
    override fun doAnnotate(collectedInfo: Unit?) {}
    override fun apply(file: PsiFile, annotationResult: Unit?, holder: AnnotationHolder) {}
}
