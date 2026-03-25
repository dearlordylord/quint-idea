package com.dearlordylord.quint.idea.settings

import com.intellij.util.io.PathEnvironmentVariableUtil
import java.io.File

object QuintBinaryDetector {

    private const val BINARY_NAME = "quint"

    fun detect(): String? {
        PathEnvironmentVariableUtil.findInPath(BINARY_NAME)?.let {
            return it.absolutePath
        }

        val candidates = buildList {
            add("/usr/local/bin/quint")
            add("/opt/homebrew/bin/quint")
            add("/usr/bin/quint")
            System.getProperty("user.home")?.let { home ->
                add("$home/.npm-global/bin/quint")
                add("$home/.nvm/current/bin/quint")
            }
        }

        return candidates.firstNotNullOfOrNull { path ->
            File(path).takeIf { it.exists() && it.canExecute() }?.absolutePath
        }
    }
}
