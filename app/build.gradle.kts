import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

val configuredApiBaseUrl = providers.gradleProperty("MUKHTABIR_API_BASE_URL")
    .orElse(Properties().apply {
        val file = rootProject.file("server.properties")
        if (file.exists()) file.inputStream().use { load(it) }
    }.getProperty("api.base.url", localProperties.getProperty("api.base.url", "http://10.0.2.2:8000/api/v1/")))

fun quotedBuildConfigValue(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

android {
    namespace = "com.mukhtabir.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mukhtabir.android"
        minSdk = 23
        targetSdk = 34
        versionCode = 3
        versionName = "0.1.2"

        vectorDrawables.useSupportLibrary = true
        buildConfigField(
            "String",
            "API_BASE_URL",
            quotedBuildConfigValue(configuredApiBaseUrl.get()),
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.2"
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    // Intentionally no ndk.abiFilters and no ABI splits. This project has no
    // native libraries, so one APK/AAB supports ARM32, ARM64, x86 and x86_64.

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material")
    // Material extended icons — used by the navigation bar, the More
    // screen, and every screen that renders a semantic icon.
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.navigation:navigation-compose:2.7.6")
    implementation("androidx.security:security-crypto:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:html:4.6.2")
    implementation("io.noties.markwon:ext-strikethrough:4.6.2")
    implementation("io.noties.markwon:ext-tables:4.6.2")
    implementation("io.noties.markwon:simple-ext:4.6.2")

    debugImplementation("androidx.compose.ui:ui-tooling")

    // ── Unit test dependencies (Phase 0) ───────────────────────────
    // MockWebServer exercises the ApiClient over a real local HTTP
    // server so the interceptor chain, cookie jar, and CSRF retry
    // path are all covered end to end.
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.robolectric:robolectric:4.12.2")
    testImplementation("androidx.test:core:1.5.0")
}
