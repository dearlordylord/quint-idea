package com.dearlordylord.quint.idea.structure

import com.dearlordylord.quint.idea.QuintIcons
import com.dearlordylord.quint.idea.psi.QuintFile
import com.dearlordylord.quint.idea.psi.QuintPsiUtils
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.structureView.*
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.NavigatablePsiElement
import org.antlr.intellij.adaptor.lexer.RuleIElementType
import com.dearlordylord.quint.idea.parser.QuintParser
import javax.swing.Icon

class QuintStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder? {
        if (psiFile !is QuintFile) return null
        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel {
                return QuintStructureViewModel(psiFile)
            }
        }
    }
}

class QuintStructureViewModel(file: PsiFile) :
    StructureViewModelBase(file, QuintStructureViewElement(file)),
    StructureViewModel.ElementInfoProvider {

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean = false
    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean = false
}

class QuintStructureViewElement(private val element: PsiElement) : StructureViewTreeElement {

    override fun getValue(): Any = element

    override fun navigate(requestFocus: Boolean) {
        if (element is NavigatablePsiElement) {
            element.navigate(requestFocus)
        }
    }

    override fun canNavigate(): Boolean = element is NavigatablePsiElement && element.canNavigate()
    override fun canNavigateToSource(): Boolean = element is NavigatablePsiElement && element.canNavigateToSource()

    override fun getPresentation(): ItemPresentation {
        if (element is PsiFile) {
            return element.presentation ?: PresentationData(element.name, null, QuintIcons.FILE, null)
        }

        val type = element.node?.elementType
        val name = QuintPsiUtils.getDeclarationName(element)
        val qualifier = QuintPsiUtils.getDeclarationQualifier(element)

        val icon = getIconForElement(type, qualifier)
        val displayText = if (qualifier != null && name != null) "$qualifier $name" else name ?: element.text?.take(30) ?: "?"

        return PresentationData(displayText, null, icon, null)
    }

    override fun getChildren(): Array<TreeElement> {
        if (element is PsiFile) {
            // Find modules at the top level
            val modules = QuintPsiUtils.findModules(element)
            return modules.map { QuintStructureViewElement(it) }.toTypedArray()
        }

        val type = element.node?.elementType
        if (type is RuleIElementType && type.ruleIndex == QuintParser.RULE_module) {
            // Find declarations inside the module
            val declarations = QuintPsiUtils.findDeclarations(element)
            return declarations.map { QuintStructureViewElement(it) }.toTypedArray()
        }

        return emptyArray()
    }

    private fun getIconForElement(type: Any?, qualifier: String?): Icon {
        return QuintIcons.FILE // Use the Quint file icon as a general-purpose icon
    }
}
