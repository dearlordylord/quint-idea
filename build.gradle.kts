import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import java.util.Properties

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.12.0"
    id("antlr")
}

group = "com.dearlordylord.quint.idea"
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

val jflexVersion = "1.9.1"
val jflexConfig by configurations.creating

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(providers.gradleProperty("platformVersion").get())
        testFramework(TestFrameworkType.Platform)
    }
    antlr("org.antlr:antlr4:4.13.2")
    implementation("org.antlr:antlr4-runtime:4.13.2")
    jflexConfig("de.jflex:jflex:$jflexVersion")

    testImplementation("junit:junit:4.13.2")
}

// ANTLR configuration
tasks.generateGrammarSource {
    arguments = arguments + listOf("-visitor", "-package", "com.dearlordylord.quint.idea.parser")
}

// JFlex configuration
tasks.register<JavaExec>("generateLexer") {
    classpath = jflexConfig
    mainClass.set("jflex.Main")
    val outputDir = "${layout.buildDirectory.get()}/generated-src/jflex/com/dearlordylord/quint/idea/lexer"
    val inputFile = "src/main/jflex/com/dearlordylord/quint/idea/lexer/Quint.flex"
    args = listOf("-d", outputDir, inputFile)
    inputs.file(inputFile)
    outputs.dir(outputDir)
}

sourceSets["main"].java {
    srcDir("${layout.buildDirectory.get()}/generated-src/jflex")
}

tasks.named("compileJava") {
    dependsOn("generateLexer", "generateGrammarSource")
}
tasks.named("compileKotlin") {
    dependsOn("generateLexer", "generateGrammarSource")
}

// Prevent ANTLR runtime from leaking into the IntelliJ plugin classpath as a transitive dependency
configurations {
    implementation {
        exclude(group = "org.antlr", module = "antlr4")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

intellijPlatform {
    pluginConfiguration {
        id = "com.dearlordylord.quint.idea"
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")
        changeNotes = provider {
            file("CHANGELOG.md").readText()
                .substringAfter("## [${project.version}]")
                .substringBefore("\n## [")
                .lines()
                .filter { it.isNotBlank() }
                .joinToString("<br>\n") { it.removePrefix("### ").removePrefix("- ") }
        }
    }
    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}

// The plugin and source fixtures do not require Node/npm. This optional gate exercises
// a separately installed, version-pinned Quint CLI and fails when it is unavailable.
val quintTestVersion = Properties().apply {
    file("ci/quint-test-toolchain.properties").inputStream().use { load(it) }
}.getProperty("quintVersion")
val quintTestExecutable = providers.environmentVariable("QUINT_TEST_EXECUTABLE")
val validateQuintTestToolchain by tasks.registering {
    doLast {
        val executable = quintTestExecutable.orNull
            ?: error("realCliTest requires QUINT_TEST_EXECUTABLE pointing to Quint $quintTestVersion")
        require(file(executable).isAbsolute && file(executable).canExecute()) { "QUINT_TEST_EXECUTABLE must be an absolute executable path" }
        val output = providers.exec { commandLine(executable, "--version") }.standardOutput.asText.get().trim()
        require(output == quintTestVersion) { "Expected Quint $quintTestVersion; found $output" }
    }
}
tasks.test {
    inputs.property("quintTestExecutable", quintTestExecutable.orElse(""))
    environment("QUINT_TEST_EXECUTABLE", quintTestExecutable.orElse("").get())
    mustRunAfter(validateQuintTestToolchain)
    if (!quintTestExecutable.orNull.isNullOrBlank()) dependsOn(validateQuintTestToolchain)
}
tasks.register("realCliTest") {
    group = "verification"
    description = "Require the pinned Quint CLI and run editor plus real subprocess fixtures"
    dependsOn(validateQuintTestToolchain, tasks.test)
}

intellijPlatform {
    pluginVerification {
        ides { create(org.jetbrains.intellij.platform.gradle.IntelliJPlatformType.IntellijIdeaCommunity, providers.gradleProperty("platformVersion").get()) }
    }
}
