@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.apk.dist)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

android {
    compileSdk = 36

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "app.iremote"
        minSdk = 24
        targetSdk = 36
        versionCode = 560
        versionName = "3.4.4"

        androidResources {
            localeFilters += setOf("en", "ar", "de", "es-rES", "es-rUS", "fr", "hr", "hu", "in", "it", "ja", "pl", "pt-rBR", "ru-rRU", "sv", "tr", "uk", "zh")
        }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }


    val enableApkSplits = (providers.gradleProperty("enableApkSplits").orNull ?: "true").toBoolean()
    val includeUniversalApk = (providers.gradleProperty("includeUniversalApk").orNull ?: "true").toBoolean()
    val targetAbi = providers.gradleProperty("targetAbi").orNull

    splits {
        abi {
            isEnable = enableApkSplits
            reset()
            if (enableApkSplits) {
                if (targetAbi != null) {
                    include(targetAbi)
                } else {
                    include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
                }
            }
            isUniversalApk = includeUniversalApk && enableApkSplits
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "${rootProject.projectDir}/release.keystore")
            storePassword = System.getenv("STORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = false
            enableV4Signing = false
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = false
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("Long", "BUILD_TIME", "0L")
            isShrinkResources = true
        }
        getByName("debug") {
            isShrinkResources = false
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "2.0.0"
    }

    namespace = "app.iremote"


    dependenciesInfo {
        includeInApk = false
    }
}

// Configure all tasks that are instances of AbstractArchiveTask
tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

dependencies {
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.lifecycle.viewmodel.navigation3)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlin.stdlib)
    implementation(libs.core.ktx)

    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.material)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.runtime)
    implementation(libs.androidx.animation)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.material3.android)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)

    implementation(libs.kmp.settings.core)

    implementation(libs.kmp.settings.ui.compose)
    ksp(libs.kmp.settings.ksp)

    // TV Compose
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.tv.foundation)

    // Local jars (if any)
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // Debug tooling
    debugImplementation(libs.androidx.ui.tooling)
}
