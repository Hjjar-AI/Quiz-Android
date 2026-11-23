import java.io.StringReader
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val storedVersion = Properties().apply {
    load(StringReader(providers.fileContents(rootProject.layout.projectDirectory.file("gradle/app-version.properties")).asText.get()))
}

android {
    namespace = "com.mukhtabir.android.demo"
    compileSdk = 34

    sourceSets.getByName("main").setRoot(".")

    defaultConfig {
        applicationId = "com.mukhtabir.android.demo"
        minSdk = 23
        targetSdk = 34
        versionCode = storedVersion.getProperty("versionCode").toInt()
        versionName = storedVersion.getProperty("versionName")
    }

    buildFeatures {
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
}

dependencies {
    implementation(project(":shared"))
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.6")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}
