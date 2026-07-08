import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    kotlin("jvm") version "2.1.0"

    // Fat-jar: bundles the Kotlin stdlib into the final jar (the server has no
    // Kotlin on its classpath).
    id("com.gradleup.shadow") version "8.3.11"

    // Generates plugin.yml from the `bukkit { }` block below — no hand-written
    // descriptor to keep in sync.
    id("net.minecrell.plugin-yml.paper") version "0.6.0"

    // Adds `runServer` (Paper) and, once enabled below, `runFolia` tasks that
    // download a server and launch it with your plugin for quick testing.
    id("xyz.jpenilla.run-paper") version "2.3.0"
}

group = property("group") as String
version = property("version") as String

val foliaApiVersion = property("foliaApiVersion") as String
// Single source of truth: derive the Minecraft version from the Folia API
// version (e.g. "1.21.11-R0.1-SNAPSHOT" -> "1.21.11").
val minecraftVersion = foliaApiVersion.substringBefore("-R")

repositories {
    mavenCentral()
    // PaperMC repository — hosts the Folia API artifact.
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Provided by the server at runtime — must NOT be bundled.
    compileOnly("dev.folia:folia-api:$foliaApiVersion")

    // Also provided by the server (Paper/Folia bundle Gson). Used by the
    // update checker to parse Modrinth's JSON.
    compileOnly("com.google.code.gson:gson:2.11.0")

    // Bundled AND relocated (see shadowJar): anonymous usage metrics.
    implementation("org.bstats:bstats-bukkit:3.1.0")

    // Bundled via shadowJar (the server doesn't ship Kotlin).
    implementation(kotlin("stdlib"))

    // Unit tests — pure logic only, no running server required.
    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(21) // Folia 1.21.11 requires Java 21
}

// ---- plugin.yml generation (plugin-yml) ------------------------------------
// This replaces a hand-maintained src/main/resources/plugin.yml. The generated
// file is added to the main resources and so ends up inside the shaded jar.
paper {
    name = "FoliaTemplate"
    main = "com.example.foliatemplate.FoliaTemplatePlugin"
    apiVersion = "1.21"
    foliaSupported = true // REQUIRED — Folia refuses to load plugins without it
    authors = listOf("YourName")
    description = "A Folia 1.21.11 Kotlin plugin template with an annotation-based command system."
    website = "https://example.com"
    // No `commands { }` block: commands are registered programmatically by
    // CommandManager from the @Command annotations.
}

// ---- build / packaging -----------------------------------------------------
tasks {
    shadowJar {
        archiveClassifier.set("") // produce folia-template-1.0.0.jar (no -all suffix)
        // bStats must be relocated per its terms so every plugin ships its own copy.
        relocate("org.bstats", "com.example.foliatemplate.libs.bstats")
        // OPTIONAL (recommended for shipped plugins): relocate the Kotlin runtime
        // so it can't clash with other Kotlin plugins.
        // relocate("kotlin", "com.example.foliatemplate.libs.kotlin")
    }
    build { dependsOn(shadowJar) }
    // Only the shaded jar should land in build/libs.
    jar { enabled = false }
    // Run the JUnit 5 unit tests on `gradle build` / `gradle test`.
    test { useJUnitPlatform() }
}

// ---- test servers (run-paper) ----------------------------------------------
// Pin the test server to Java 21 regardless of the JDK Gradle itself runs on.
val java21 = javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) }

tasks.runServer {
    minecraftVersion(minecraftVersion) // `./gradlew runServer` → a Paper test server
    javaLauncher.set(java21)
}

runPaper {
    folia {
        // Registers the `runFolia` task. It reuses runServer's Minecraft version
        // and auto-detects your built (shadow) jar.
        registerTask {
            javaLauncher.set(java21)
        }
    }
}
