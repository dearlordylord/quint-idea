package com.dearlordylord.quint.idea.settings

import java.io.File

object QuintBinaryDetector {

    private const val BINARY_NAME = "quint"

    fun detect(): String? {
        val candidates = buildList {
            System.getenv("PATH")?.split(File.pathSeparator)?.forEach { dir ->
                add(File(dir, BINARY_NAME).absolutePath)
            }
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
