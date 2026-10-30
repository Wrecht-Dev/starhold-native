import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Release versioning: CI passes -PstarholdVersion=<tag without v> and
// -PstarholdVersionCode=<run number>. Local builds fall back to these defaults.
// Native installers (Dmg/Msi) require MAJOR > 0, so release tags start at
// v1.0.0 rather than v0.x.
val starholdVersionName: String =
    (findProperty("starholdVersion") as String?)?.removePrefix("v")?.ifBlank { null } ?: "1.0.0"
val starholdVersionCode: Int =
    (findProperty("starholdVersionCode") as String?)?.toIntOrNull() ?: 1
// jpackage accepts only numeric versions (no -rc.1 suffix).
val desktopPackageVersion: String =
    starholdVersionName.substringBefore('-').ifBlank { "1.0.0" }

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "StarholdApp"
            isStatic = true
        }
    }

    jvmToolchain(21)

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.components.resources)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.exoplayer.hls)
            implementation(libs.androidx.media3.ui)
            implementation(libs.kotlinx.coroutines.android)
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "dev.starhold.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.starhold.app"
        minSdk = 26
        targetSdk = 36
        versionCode = starholdVersionCode
        versionName = starholdVersionName
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Release signing is optional: CI decodes a keystore into
    // STARHOLD_KEYSTORE_PATH. Without these variables `assembleRelease`
    // produces an unsigned APK/AAB, which is still useful for testing.
    signingConfigs {
        val keystore = System.getenv("STARHOLD_KEYSTORE_PATH")
        if (!keystore.isNullOrBlank() && file(keystore).exists()) {
            create("release") {
                storeFile = file(keystore)
                storePassword = System.getenv("STARHOLD_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("STARHOLD_KEY_ALIAS")
                keyPassword = System.getenv("STARHOLD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "dev.starhold.app.resources"
    generateResClass = always
}

compose.desktop {
    application {
        mainClass = "dev.starhold.app.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "Starhold"
            packageVersion = desktopPackageVersion
            description = "Starhold native client"
            vendor = "Starhold"
            linux {
                iconFile.set(project.file("src/desktopMain/resources/icon.png"))
            }
        }
    }
}
