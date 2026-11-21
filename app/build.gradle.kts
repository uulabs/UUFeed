import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

val mapsApiKey = (
    localProperties.getProperty("MAPS_API_KEY")?.trim()?.ifBlank { null }
        ?: System.getenv("MAPS_API_KEY")
        ?: ""
    )
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun configuredSecret(property: String, env: String): String? =
    System.getenv(env)?.trim()?.ifBlank { null }
        ?: keystoreProperties.getProperty(property)?.trim()?.ifBlank { null }

val keystorePath = configuredSecret("storeFile", "UUFEED_KEYSTORE_PATH")
val keystorePassword = configuredSecret("storePassword", "UUFEED_KEYSTORE_PASSWORD")
val keyAlias = configuredSecret("keyAlias", "UUFEED_KEY_ALIAS")
val keyPassword = configuredSecret("keyPassword", "UUFEED_KEY_PASSWORD")
val keystoreFile: File? = keystorePath?.let { path ->
    val file = File(path)
    if (file.isAbsolute) file else rootProject.file(path)
}
val releaseSigningReady = keystoreFile != null &&
    keystorePassword != null &&
    keyAlias != null &&
    keyPassword != null

val releaseTag = Regex("""v(\d+)\.(\d+)\.(\d+)""")
    .matchEntire(System.getenv("GITHUB_REF_NAME").orEmpty().trim())
val appVersionName = releaseTag?.let {
    "${it.groupValues[1]}.${it.groupValues[2]}.${it.groupValues[3]}"
} ?: "1.0.0"
val appVersionCode = releaseTag?.let {
    val major = it.groupValues[1].toLong()
    val minor = it.groupValues[2].toLong()
    val patch = it.groupValues[3].toLong()
    val code = major * 10_000 + minor * 100 + patch
    require(code in 1..2_100_000_000L) {
        "versionCode $code from tag ${it.value} is outside the Android range"
    }
    code.toInt()
} ?: 1

android {
    namespace = "au.com.uulabs.uufeed"
    compileSdk = 37

    defaultConfig {
        applicationId = "au.com.uulabs.uufeed"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
    }

    signingConfigs {
        if (releaseSigningReady) {
            val storeFileForRelease = keystoreFile
            val storePasswordForRelease = keystorePassword
            val aliasForRelease = keyAlias
            val keyPasswordForRelease = keyPassword
            create("release") {
                storeFile = storeFileForRelease
                storePassword = storePasswordForRelease
                keyAlias = aliasForRelease
                keyPassword = keyPasswordForRelease
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (releaseSigningReady) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/LICENSE-notice.md"
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

gradle.taskGraph.whenReady {
    val assemblingRelease = allTasks.any { task ->
        task.project == project && task.name in setOf("assembleRelease", "bundleRelease")
    }
    if (!assemblingRelease) {
        return@whenReady
    }
    if (!releaseSigningReady) {
        error(
            "Release signing is not configured. Set UUFEED_KEYSTORE_PATH, " +
                "UUFEED_KEYSTORE_PASSWORD, UUFEED_KEY_ALIAS, and UUFEED_KEY_PASSWORD, " +
                "or create keystore.properties from keystore.properties.example. " +
                "See MAINTAINERS/RELEASING.md.",
        )
    }
    if (keystoreFile?.isFile != true) {
        error(
            "Release keystore was not found at ${keystoreFile}. " +
                "Check UUFEED_KEYSTORE_PATH or storeFile in keystore.properties.",
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.serialization.json)
    implementation(libs.coroutines.android)

    implementation(libs.coil.compose)
    implementation(libs.coil.okhttp)
    implementation(libs.security.crypto)

    testImplementation(libs.junit)
    testImplementation(libs.mockwebserver)
}
