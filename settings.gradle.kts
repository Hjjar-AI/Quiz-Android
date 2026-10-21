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
include(":app")


// OS-account home, never the shared checkout or another account's GRADLE_USER_HOME.
val accountHome = File(System.getProperty("user.home"))
val checkout = settingsDir.canonicalPath
val checkoutId = MessageDigest.getInstance("SHA-256").digest(checkout.toByteArray(Charsets.UTF_8))
    .joinToString("") { "%02x".format(it.toInt() and 0xff) }.take(16)
val accountRoot = File(accountHome, ".gradle/mukhtabir/$checkoutId")
gradle.startParameter.projectCacheDir = File(accountRoot, "project-cache")
buildCache {
    local {
        directory = File(accountRoot, "task-cache")
        isEnabled = true
    }
}
