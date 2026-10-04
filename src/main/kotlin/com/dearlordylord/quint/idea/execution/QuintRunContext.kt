package com.dearlordylord.quint.idea.execution

import com.dearlordylord.quint.idea.QuintFileType
import com.dearlordylord.quint.idea.parser.QuintParser
import com.dearlordylord.quint.idea.psi.QuintNamedElement
import com.dearlordylord.quint.idea.psi.QuintPsiShape
import com.dearlordylord.quint.idea.psi.QuintPsiUtils
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

private data class QuintExecutionTarget(val module: QuintNamedElement, val mode: QuintExecutionMode, val test: String)

private fun targetAt(element: PsiElement?): QuintExecutionTarget? {
    val named = element?.let { PsiTreeUtil.getParentOfType(it, QuintNamedElement::class.java, false) } ?: return null
    if (named.containingFile.fileType != QuintFileType.INSTANCE || named.containingFile.virtualFile == null) return null
    val module = if (QuintPsiShape.ruleIndex(named) == QuintParser.RULE_module) named else QuintPsiUtils.getContainingModule(named) as? QuintNamedElement ?: return null
    val declarations = QuintPsiUtils.findDeclarations(module).mapNotNull { declaration ->
        if (declaration is QuintNamedElement) declaration else declaration.children.filterIsInstance<QuintNamedElement>().firstOrNull()
    }
    if (named != module && named !in declarations) return null
    if (named != module && QuintPsiShape.ruleIndex(named) == QuintParser.RULE_operDef && named.name?.endsWith("Test") == true &&
        QuintPsiShape.parameterNodes(named).isEmpty() && QuintPsiShape.annotatedParameterNodes(named).isEmpty()) {
        return QuintExecutionTarget(module, QuintExecutionMode.TEST, named.name!!)
    }
    if (named == module) {
        val names = declarations.mapNotNull { it.name }.toSet()
        if ("init" in names && "step" in names) return QuintExecutionTarget(module, QuintExecutionMode.RUN, "")
        if (declarations.any { it.name?.endsWith("Test") == true }) return QuintExecutionTarget(module, QuintExecutionMode.TEST, "")
    }
    return null
}

class QuintRunConfigurationProducer : LazyRunConfigurationProducer<QuintRunConfiguration>(), DumbAware {
    override fun getConfigurationFactory(): ConfigurationFactory = ConfigurationTypeUtil.findConfigurationType(QuintRunConfigurationType::class.java).configurationFactories.single()

    override fun setupConfigurationFromContext(configuration: QuintRunConfiguration, context: ConfigurationContext, sourceElement: Ref<PsiElement>): Boolean {
        val target = targetAt(context.psiLocation) ?: return false
        configuration.sourcePath = target.module.containingFile.virtualFile.path
        configuration.mainModule = target.module.name ?: return false
        configuration.mode = target.mode
        configuration.testName = target.test
        configuration.name = "Quint ${target.mode.name.lowercase()}: ${target.test.ifEmpty { configuration.mainModule }} (saved files)"
        sourceElement.set(context.psiLocation)
        return true
    }

    override fun isConfigurationFromContext(configuration: QuintRunConfiguration, context: ConfigurationContext): Boolean {
        val target = targetAt(context.psiLocation) ?: return false
        return configuration.sourcePath == target.module.containingFile.virtualFile.path && configuration.mainModule == target.module.name &&
            configuration.mode == target.mode && configuration.testName == target.test
    }
}

class QuintRunLineMarkerContributor : RunLineMarkerContributor(), DumbAware {
    override fun getInfo(element: PsiElement): Info? {
        if (element.firstChild != null) return null
        val named = PsiTreeUtil.getParentOfType(element, QuintNamedElement::class.java) ?: return null
        if (named.nameIdentifier?.textRange?.startOffset != element.textRange.startOffset) return null
        val target = targetAt(element) ?: return null
        return Info(AllIcons.Actions.Execute, ExecutorAction.getActions()) { "Quint ${target.mode.name.lowercase()} (saved files)" }
    }
}
