// AGP 9 compiles Kotlin itself. This pins the Kotlin version it uses.
buildscript {
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
}
