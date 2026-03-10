package com.dearlordylord.quint.idea.psi

import com.dearlordylord.quint.idea.QuintFileType
import com.dearlordylord.quint.idea.QuintLanguage
import com.dearlordylord.quint.idea.parser.QuintParser
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import org.antlr.intellij.adaptor.lexer.PSIElementTypeFactory
import org.antlr.intellij.adaptor.lexer.RuleIElementType

object QuintPsiUtils {

    private val ruleTypes by lazy {
        PSIElementTypeFactory.getRuleIElementTypes(QuintLanguage.INSTANCE)
    }

    val MODULE_RULE: RuleIElementType get() = ruleTypes[QuintParser.RULE_module]
    val DECLARATION_RULE: RuleIElementType get() = ruleTypes[QuintParser.RULE_declaration]
    val OPER_DEF_RULE: RuleIElementType get() = ruleTypes[QuintParser.RULE_operDef]
    val TYPE_DEF_RULE: RuleIElementType get() = ruleTypes[QuintParser.RULE_typeDef]
    val IMPORT_MOD_RULE: RuleIElementType get() = ruleTypes[QuintParser.RULE_importMod]
    val EXPORT_MOD_RULE: RuleIElementType get() = ruleTypes[QuintParser.RULE_exportMod]
    val QUALIFIER_RULE: RuleIElementType get() = ruleTypes[QuintParser.RULE_qualifier]
    val QUAL_ID_RULE: RuleIElementType get() = ruleTypes[QuintParser.RULE_qualId]
    val EXPR_RULE: RuleIElementType get() = ruleTypes[QuintParser.RULE_expr]
    val DOCUMENTED_DECLARATION_RULE: RuleIElementType get() = ruleTypes[QuintParser.RULE_documentedDeclaration]
    val INSTANCE_MOD_RULE: RuleIElementType get() = ruleTypes[QuintParser.RULE_instanceMod]
    val NORMAL_CALL_NAME_RULE: RuleIElementType get() = ruleTypes[QuintParser.RULE_normalCallName]
    val PARAMETER_RULE: RuleIElementType get() = ruleTypes[QuintParser.RULE_parameter]
    val ANNOTATED_PARAMETER_RULE: RuleIElementType get() = ruleTypes[QuintParser.RULE_annotatedParameter]
    val LAMBDA_UNSUGARED_RULE: RuleIElementType get() = ruleTypes[QuintParser.RULE_lambdaUnsugared]
    val LAMBDA_TUPLE_SUGAR_RULE: RuleIElementType get() = ruleTypes[QuintParser.RULE_lambdaTupleSugar]
    val IDENT_OR_HOLE_RULE: RuleIElementType get() = ruleTypes[QuintParser.RULE_identOrHole]
    val TYPE_DEF_HEAD_RULE: RuleIElementType get() = ruleTypes[QuintParser.RULE_typeDefHead]

    /**
     * Find all module elements in a file.
     */
    fun findModules(file: PsiElement): List<PsiElement> {
        return findChildrenOfRule(file, QuintParser.RULE_module)
    }

    /**
     * Find all declaration elements within a parent element.
     */
    fun findDeclarations(parent: PsiElement): List<PsiElement> {
        val documented = findChildrenOfRule(parent, QuintParser.RULE_documentedDeclaration)
        return documented.flatMap { docDecl ->
            findChildrenOfRule(docDecl, QuintParser.RULE_declaration)
        }
    }

    /**
     * Get the name of a declaration (module, operDef, typeDef, etc.).
     * Returns null if the name cannot be determined.
     */
    fun getDeclarationName(declaration: PsiElement): String? {
        val type = declaration.node?.elementType
        if (type !is RuleIElementType) return null

        return when (type.ruleIndex) {
            QuintParser.RULE_module -> {
                // module: DOCCOMMENT* 'module' qualId '{' ... '}'
                val qualId = findFirstChildOfRule(declaration, QuintParser.RULE_qualId)
                qualId?.text
            }
            QuintParser.RULE_declaration -> {
                val child = declaration.firstChild ?: return null
                val childType = child.node?.elementType
                if (childType is RuleIElementType) {
                    // Sub-rule: operDef, typeDef, importMod, etc.
                    getDeclarationName(child)
                } else {
                    // Keyword-led: const/var/assume — name is in qualId or identOrHole child
                    findFirstChildOfRule(declaration, QuintParser.RULE_qualId)?.text
                        ?: findFirstChildOfRule(declaration, QuintParser.RULE_identOrHole)?.text
                }
            }
            QuintParser.RULE_operDef -> {
                // operDef has qualifier then normalCallName
                val callName = findFirstChildOfRule(declaration, QuintParser.RULE_normalCallName)
                callName?.text
            }
            QuintParser.RULE_typeDef -> {
                // typeDef: 'type' qualId or typeDefHead
                val qualId = findFirstChildOfRule(declaration, QuintParser.RULE_qualId)
                    ?: findFirstChildOfRule(declaration, QuintParser.RULE_typeDefHead)?.let {
                        findFirstChildOfRule(it, QuintParser.RULE_qualId)
                    }
                qualId?.text
            }
            QuintParser.RULE_importMod -> {
                // import name ...
                val name = findFirstChildOfRule(declaration, QuintParser.RULE_name)
                name?.text
            }
            QuintParser.RULE_exportMod -> {
                val name = findFirstChildOfRule(declaration, QuintParser.RULE_name)
                name?.text
            }
            QuintParser.RULE_instanceMod -> {
                val moduleName = findFirstChildOfRule(declaration, QuintParser.RULE_moduleName)
                moduleName?.text
            }
            QuintParser.RULE_parameter, QuintParser.RULE_annotatedParameter -> {
                // parameter → identOrHole → qualId
                val identOrHole = findFirstChildOfRule(declaration, QuintParser.RULE_identOrHole)
                identOrHole?.text
            }
            else -> null
        }
    }

    /**
     * Get the qualifier keyword of a declaration (val, def, pure val, action, etc.)
     */
    fun getDeclarationQualifier(declaration: PsiElement): String? {
        val type = declaration.node?.elementType
        if (type !is RuleIElementType) return null

        return when (type.ruleIndex) {
            QuintParser.RULE_module -> "module"
            QuintParser.RULE_declaration -> {
                // Look at the first keyword token to determine the type
                val firstChild = declaration.firstChild ?: return null
                val firstChildType = firstChild.node?.elementType
                if (firstChildType is RuleIElementType) {
                    when (firstChildType.ruleIndex) {
                        QuintParser.RULE_operDef -> {
                            val qualifier = findFirstChildOfRule(firstChild, QuintParser.RULE_qualifier)
                            qualifier?.text
                        }
                        QuintParser.RULE_typeDef -> "type"
                        QuintParser.RULE_importMod -> "import"
                        QuintParser.RULE_exportMod -> "export"
                        QuintParser.RULE_instanceMod -> "import"
                        else -> null
                    }
                } else {
                    // Direct keyword tokens like 'const', 'var', 'assume'
                    firstChild.text
                }
            }
            QuintParser.RULE_operDef -> {
                val qualifier = findFirstChildOfRule(declaration, QuintParser.RULE_qualifier)
                qualifier?.text
            }
            QuintParser.RULE_typeDef -> "type"
            QuintParser.RULE_importMod -> "import"
            QuintParser.RULE_exportMod -> "export"
            QuintParser.RULE_parameter, QuintParser.RULE_annotatedParameter -> "parameter"
            else -> null
        }
    }

    /**
     * Find children of a given rule type (with recursive search up to maxDepth).
     */
    fun findChildrenOfRule(parent: PsiElement, ruleIndex: Int): List<PsiElement> {
        val result = mutableListOf<PsiElement>()
        findChildrenOfRuleRecursive(parent, ruleIndex, result, maxDepth = 5)
        return result
    }

    private fun findChildrenOfRuleRecursive(
        element: PsiElement,
        ruleIndex: Int,
        result: MutableList<PsiElement>,
        maxDepth: Int
    ) {
        if (maxDepth <= 0) return
        var child = element.firstChild
        while (child != null) {
            val childType = child.node?.elementType
            if (childType is RuleIElementType && childType.ruleIndex == ruleIndex) {
                result.add(child)
            } else {
                findChildrenOfRuleRecursive(child, ruleIndex, result, maxDepth - 1)
            }
            child = child.nextSibling
        }
    }

    /**
     * Find the first direct child matching a given rule type.
     */
    fun findFirstChildOfRule(parent: PsiElement, ruleIndex: Int): PsiElement? {
        var child = parent.firstChild
        while (child != null) {
            val childType = child.node?.elementType
            if (childType is RuleIElementType && childType.ruleIndex == ruleIndex) {
                return child
            }
            child = child.nextSibling
        }
        return null
    }

    /**
     * Walk parents to find the enclosing module node.
     */
    fun getContainingModule(element: PsiElement): PsiElement? {
        var current = element.parent
        while (current != null) {
            val type = current.node?.elementType
            if (type is RuleIElementType && type.ruleIndex == QuintParser.RULE_module) {
                return current
            }
            current = current.parent
        }
        return null
    }

    /**
     * Create a fresh qualId PSI node from a name string by parsing a dummy file.
     */
    fun createQualIdFromText(project: Project, name: String): PsiElement? {
        val dummyFile = PsiFileFactory.getInstance(project)
            .createFileFromText("dummy.qnt", QuintFileType.INSTANCE, "module _R { val $name = 0 }")
        val module = findModules(dummyFile).firstOrNull() ?: return null
        val decl = findDeclarations(module).firstOrNull() ?: return null
        return findChildrenOfRule(decl, QuintParser.RULE_qualId).firstOrNull()
    }

    /**
     * Returns true if this qualId node is at a declaration site (not a usage).
     */
    fun isDeclarationIdentifier(qualId: PsiElement): Boolean {
        val parent = qualId.parent ?: return false
        val parentType = parent.node?.elementType as? RuleIElementType ?: return false

        return when (parentType.ruleIndex) {
            QuintParser.RULE_normalCallName -> {
                // Declaration if inside operDef, usage if inside expr
                val gp = parent.parent
                val gpType = gp?.node?.elementType as? RuleIElementType
                gpType?.ruleIndex == QuintParser.RULE_operDef
            }
            QuintParser.RULE_module -> true
            QuintParser.RULE_typeDefHead -> true
            QuintParser.RULE_typeDef -> {
                // 'type' qualId (abstract type) — qualId is direct child of typeDef
                true
            }
            QuintParser.RULE_declaration -> {
                // const/var: qualId is direct child of declaration
                val firstChild = parent.firstChild
                firstChild?.text == "const" || firstChild?.text == "var"
            }
            QuintParser.RULE_identOrHole -> {
                val gp = parent.parent
                val gpType = gp?.node?.elementType as? RuleIElementType
                when (gpType?.ruleIndex) {
                    QuintParser.RULE_parameter, QuintParser.RULE_annotatedParameter -> true
                    QuintParser.RULE_declaration -> gp.firstChild?.text == "assume"
                    else -> false
                }
            }
            // qualId inside name/moduleName/qualifiedName/identOrStar rules (import/export targets)
            QuintParser.RULE_name -> {
                // name inside instanceMod parameter bindings (e.g. VALS = Set(...)) are
                // references to const declarations in the target module, not declarations
                val gp = parent.parent
                val gpType = gp?.node?.elementType as? RuleIElementType
                gpType?.ruleIndex != QuintParser.RULE_instanceMod
            }
            QuintParser.RULE_moduleName, QuintParser.RULE_qualifiedName,
            QuintParser.RULE_identOrStar -> true
            else -> false
        }
    }
}
