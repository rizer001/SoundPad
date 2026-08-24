import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.bundling.Zip

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.serialization)
}

group = "com.rizer01"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://jitpack.io")
    google()
}

dependencies {
    // Compose Desktop
    implementation(libs.compose.desktop)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)

    // Skiko native runtime (Compose rendering)
    runtimeOnly("org.jetbrains.skiko:skiko-awt-runtime-windows-x64:0.8.18")

    // Audio — javax.sound built-in WAV + mp3spi for MP3
    implementation("com.googlecode.soundlibs:mp3spi:1.9.5.4")

    // Global hotkeys
    implementation(libs.jnativehook)

    // SQLite — portable DB (stored next to JAR)
    implementation("org.xerial:sqlite-jdbc:3.45.3.0")

    // JSON serialization
    implementation(libs.serialization.json)

    // Coroutines
    implementation(libs.coroutines.swing)

    // Logging
    implementation(libs.kotlin.logging)
    implementation(libs.logback.classic)

    // Testing
    testImplementation(libs.junit.jupiter)
}

compose.desktop {
    application {
        mainClass = "com.rizer01.soundpad.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "Soundpad"
            packageVersion = "1.0.0"
            description = "Free, open-source soundpad for gamers and streamers"
            vendor = "rizer01"
            licenseFile = file("LICENSE")

            windows {
                menuGroup = "Soundpad"
                upgradeUuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
                iconFile.set(file("src/main/resources/icons/icon.ico"))
            }

            linux {
                iconFile.set(file("src/main/resources/icons/icon.png"))
            }

            macOS {
                iconFile.set(file("src/main/resources/icons/icon.icns"))
                bundleID = "com.rizer01.soundpad"
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

// ══════════════════════════════════════════════════════
// Portable Distribution Tasks
// ══════════════════════════════════════════════════════

val portableDistDir = layout.buildDirectory.dir("portable/Soundpad")

/** Fat JAR with all dependencies for portable distribution */
tasks.register<Jar>("fatJar") {
    archiveClassifier.set("")
    manifest {
        attributes["Main-Class"] = "com.rizer01.soundpad.MainKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }
    from(sourceSets.main.get().output)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

/** Build portable folder: fat JAR + launch scripts + sounds dir + data dir */
tasks.register("portableDistribution") {
    group = "distribution"
    description = "Assembles the portable Soundpad folder (fat JAR + launch scripts)."
    dependsOn("fatJar")

    doLast {
        val rootDir = portableDistDir.get().asFile
        delete(rootDir)
        rootDir.mkdirs()

        // Copy fat JAR
        copy {
            from(tasks.named<Jar>("fatJar").flatMap { it.archiveFile })
            into(rootDir)
            rename { "Soundpad.jar" }
        }

        // Create directories for portable data
        File(rootDir, "sounds").mkdirs()
        File(rootDir, "data").mkdirs()

        // Copy launch scripts
        copy {
            from(layout.projectDirectory.dir("scripts").asFile)
            into(rootDir)
        }

        // Make .sh executable on Linux/Mac
        if (!System.getProperty("os.name").lowercase().contains("windows")) {
            File(rootDir, "Soundpad.sh").setExecutable(true)
        }

        logger.lifecycle("Portable Soundpad assembled at: ${rootDir.absolutePath}")
    }
}

/** Zip the portable folder */
tasks.register<Zip>("portableZip") {
    group = "distribution"
    description = "Bundles the portable Soundpad folder into a zip."
    dependsOn("portableDistribution")
    archiveFileName.set("Soundpad-portable.zip")
    destinationDirectory.set(layout.buildDirectory.dir("portable"))
    from(portableDistDir)
}
