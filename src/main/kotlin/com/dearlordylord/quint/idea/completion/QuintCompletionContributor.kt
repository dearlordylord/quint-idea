package com.dearlordylord.quint.idea.completion

import com.dearlordylord.quint.idea.QuintLanguage
import com.dearlordylord.quint.idea.QuintVocabulary
import com.dearlordylord.quint.idea.annotator.QuintFieldNode
import com.dearlordylord.quint.idea.annotator.QuintTypeFormatter
import com.dearlordylord.quint.idea.psi.QuintPsiShape
import com.dearlordylord.quint.idea.psi.QuintPsiUtils
import com.dearlordylord.quint.idea.references.QuintRecordTypeResolver
import com.dearlordylord.quint.idea.references.QuintScopeResolver
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.util.ProcessingContext

class QuintCompletionContributor : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(QuintLanguage.INSTANCE),
            QuintCompletionProvider()
        )
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(QuintLanguage.INSTANCE),
            QuintRecordFieldStringCompletionProvider()
        )
    }

    class QuintCompletionProvider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            val dotContext = isDotContext(parameters.position)

            if (!dotContext) {
                // Keywords
                for (keyword in QuintVocabulary.KEYWORDS) {
                    result.addElement(
                        LookupElementBuilder.create(keyword)
                            .bold()
                            .withTypeText("keyword")
                    )
                }

                // Type keywords
                for (typeKw in QuintVocabulary.TYPE_KEYWORDS) {
                    result.addElement(
                        LookupElementBuilder.create(typeKw)
                            .bold()
                            .withTypeText("type")
                    )
                }

                // Builtin values
                for ((name, info) in QuintVocabulary.BUILTIN_VALUES) {
                    result.addElement(
                        LookupElementBuilder.create(name)
                            .bold()
                            .withTypeText(info.category)
                            .withTailText("  ${info.signature}", true)
                    )
                }
            } else {
                // Dot-callable keywords (and, or, iff, implies)
                for (keyword in QuintVocabulary.DOT_CALLABLE_KEYWORDS) {
                    result.addElement(
                        LookupElementBuilder.create(keyword)
                            .bold()
                            .withTypeText("keyword")
                    )
                }
            }

            // Record field completions in dot context
            if (dotContext) {
                val scopePos = parameters.originalPosition ?: parameters.position
                val fields = QuintRecordTypeResolver.resolveReceiverRecordFields(scopePos)
                if (fields != null) {
                    addFieldCompletions(fields, result)
                }
            }

            // Builtin operators — shown in both contexts
            for ((name, info) in QuintVocabulary.BUILTIN_OPERATORS) {
                result.addElement(
                    LookupElementBuilder.create(name)
                        .withTypeText(info.category)
                        .withTailText("  ${info.signature}", true)
                )
            }

            // Scope-aware declarations
            // Use originalPosition to preserve VFS context for cross-file import resolution.
            // parameters.position is in a copy file (with dummy identifier) that lacks VFS parent.
            val existingNames = QuintVocabulary.BUILTIN_OPERATORS.keys +
                QuintVocabulary.BUILTIN_VALUES.keys +
                QuintVocabulary.KEYWORDS +
                QuintVocabulary.TYPE_KEYWORDS
            val scopePosition = parameters.originalPosition ?: parameters.position
            val declarations = QuintScopeResolver.findVisibleSymbols(parameters.position, scopePosition)
            for (symbol in declarations) {
                val decl = symbol.declaration
                val name = symbol.name
                if (name in existingNames) continue
                val qualifier = QuintPsiUtils.getDeclarationQualifier(decl) ?: ""
                if (dotContext && !QuintVocabulary.isDotCallableQualifier(qualifier)) continue
                result.addElement(LookupElementBuilder.create(name).withTypeText(qualifier))
            }
        }
    }

    /**
     * Provides record field name completions inside string arguments of `.with("...")`.
     */
    class QuintRecordFieldStringCompletionProvider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            val position = parameters.originalPosition ?: parameters.position

            val fields = QuintRecordTypeResolver.fieldsForWithString(position) ?: return
            addFieldCompletions(fields, result)
        }
    }

    companion object {
        private val DOT_CALLABLE_QUALIFIERS = QuintVocabulary.DOT_CALLABLE_QUALIFIERS

        private fun addFieldCompletions(fields: List<QuintFieldNode>, result: CompletionResultSet) {
            for (field in fields) {
                result.addElement(
                    LookupElementBuilder.create(field.fieldName)
                        .withTypeText(QuintTypeFormatter.format(field.fieldType))
                        .withTailText("  field", true)
                        .bold()
                )
            }
        }

        fun isDotContext(position: PsiElement): Boolean {
            return QuintPsiShape.isInsideNameAfterDot(position)
        }

        fun isDotCallableQualifier(qualifier: String): Boolean = qualifier in DOT_CALLABLE_QUALIFIERS

        val DOT_CALLABLE_KEYWORDS = QuintVocabulary.DOT_CALLABLE_KEYWORDS
        val KEYWORDS = QuintVocabulary.KEYWORDS
        val TYPE_KEYWORDS = QuintVocabulary.TYPE_KEYWORDS
        val BUILTIN_OPERATORS = QuintVocabulary.BUILTIN_OPERATORS
        val BUILTIN_VALUES = QuintVocabulary.BUILTIN_VALUES
    }
}
