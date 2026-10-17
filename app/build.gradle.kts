import java.io.File

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "io.github.crankshift.niagaraprivatespace"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.crankshift.niagaraprivatespace"
        // Private Space does not exist below Android 15.
        minSdk = 35
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        // Android 15 and later run on 64-bit ABIs, so DexKit's 32-bit libraries would only add size.
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Release signing arrives with the release pipeline; until then the debug key keeps it installable.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    packaging {
        // Vector loads DexKit's native library from the module's own library directory, so it has to be extracted.
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            merges += "META-INF/xposed/*"
        }
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
            // The optional fingerprint test reads these two files. Declaring their contents as inputs makes Gradle
            // rerun it when a new Niagara APK or DexKit library appears at the same path.
            it.inputs.files(providers.environmentVariable("NIAGARA_APK").map { listOf(File(it)) }.orElse(emptyList()))
            it.inputs.files(providers.environmentVariable("DEXKIT_LIBRARY").map { listOf(File(it)) }.orElse(emptyList()))
        }
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    compileOnly(libs.libxposed.api)
    implementation(libs.dexkit)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
