import org.jetbrains.intellij.platform.gradle.TestFrameworkType

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
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

intellijPlatform {
    pluginConfiguration {
        id = "com.dearlordylord.quint.idea"
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")
    }
}
