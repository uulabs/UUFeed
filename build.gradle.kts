buildscript {
    dependencies {
        // AGP 9's built-in Kotlin defaults to an older compiler. Lunchy uses 2.4.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
