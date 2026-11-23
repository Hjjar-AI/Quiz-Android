import java.io.File
import java.security.MessageDigest

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.5.0"
        id("com.android.library") version "8.5.0"
        id("org.jetbrains.kotlin.android") version "1.9.0"
        id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Mukhtabir Android"
include(":app", ":shared", ":demo")
project(":shared").projectDir = file("app/src/shared")
project(":demo").projectDir = file("app/src/demo")


// Follow the effective Gradle home and registered local namespace; preserve portable defaults.
val accountHome = gradle.gradleUserHomeDir
val checkout = settingsDir.canonicalPath
val checkoutId = MessageDigest.getInstance("SHA-256").digest(checkout.toByteArray(Charsets.UTF_8))
    .joinToString("") { "%02x".format(it.toInt() and 0xff) }.take(16)
val accountRoot = if (gradle.extensions.extraProperties.has("androidSharedWorkDirectory"))
    gradle.extensions.extraProperties.get("androidSharedWorkDirectory") as File
else File(accountHome, "mukhtabir/$checkoutId")
if (gradle.startParameter.projectCacheDir == null) gradle.startParameter.projectCacheDir = File(accountRoot, "project-cache")
buildCache {
    local {
        directory = File(accountRoot, "task-cache")
        isEnabled = true
    }
}
