import org.jetbrains.compose.desktop.application.dsl.TargetFormat

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
