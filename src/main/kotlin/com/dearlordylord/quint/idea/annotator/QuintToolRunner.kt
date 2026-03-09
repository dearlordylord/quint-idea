package com.dearlordylord.quint.idea.annotator

interface QuintToolRunner {
    fun typecheck(filePath: String): QuintTypecheckResult
}
