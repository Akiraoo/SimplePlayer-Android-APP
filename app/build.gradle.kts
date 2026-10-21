import java.io.File
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Optional release signing. keystore.properties (never committed, see .gitignore):
//   storeFile=simpleplayer.jks      (absolute, or relative to keystore.properties itself)
//   storePassword=...
//   keyAlias=...
//   keyPassword=...
// Looked up in this order:
//   1. keystore.properties in the project root
//   2. the Gradle property simpleplayer.keystoreProperties
//      (e.g. in ~/.gradle/gradle.properties, so the path stays out of this repo)
//   3. the environment variable SIMPLEPLAYER_KEYSTORE_PROPERTIES
// With it, assembleRelease produces a signed APK. Without it, the release APK is unsigned.
val keystorePropertiesFile: File? = listOfNotNull(
    rootProject.file("keystore.properties"),
    (findProperty("simpleplayer.keystoreProperties") as String?)?.takeIf { it.isNotBlank() }?.let { File(it) },
    System.getenv("SIMPLEPLAYER_KEYSTORE_PROPERTIES")?.takeIf { it.isNotBlank() }?.let { File(it) }
).firstOrNull { it.isFile }

val keystoreProperties = Properties().apply {
    keystorePropertiesFile?.inputStream()?.use { load(it) }
}
val hasReleaseKey = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
    .all { !keystoreProperties.getProperty(it).isNullOrBlank() }

android {
    namespace = "com.akira.simpleplayer"
    compileSdk = 35

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.media3.common.util.UnstableApi"
        )
    }

    defaultConfig {
        applicationId = "com.akira.simpleplayer"
        minSdk = 26
        targetSdk = 35
        // Must stay above every APK already released (v1.1.0 shipped with 42),
        // otherwise Android refuses to install the update over it.
        versionCode = 48
        // The in-app updater compares this with the GitHub Release tag (v1.2.0 -> "1.2.0"),
        // so it must match the tag of the release this APK is published under.
        versionName = "1.4.0"
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                val path = File(keystoreProperties.getProperty("storeFile"))
                storeFile = if (path.isAbsolute) path else File(keystorePropertiesFile!!.parentFile, path.path)
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        // Compose is dramatically slower in debug builds (no R8, no inlining/AOT).
        // Judge scroll / animation smoothness on THIS build, not on debug:
        //   gradlew assembleRelease
        //   -> app/build/outputs/apk/release/app-release.apk
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseKey) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-session:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}