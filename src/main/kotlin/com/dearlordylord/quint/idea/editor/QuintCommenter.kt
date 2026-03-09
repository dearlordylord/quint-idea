package com.dearlordylord.quint.idea.editor

import com.intellij.lang.Commenter

class QuintCommenter : Commenter {
    override fun getLineCommentPrefix(): String = "// "
    override fun getBlockCommentPrefix(): String = "/* "
    override fun getBlockCommentSuffix(): String = " */"
    override fun getCommentedBlockCommentPrefix(): String? = null
    override fun getCommentedBlockCommentSuffix(): String? = null
}
