plugins {
    id("com.android.application") apply false
    id("org.jetbrains.kotlin.android") apply false
    id("org.jetbrains.kotlin.plugin.serialization") apply false
}

// Settings has already selected the account/checkout cache namespace.
val accountBuildRoot = requireNotNull(gradle.startParameter.projectCacheDir).parentFile.resolve("build")
allprojects {
    val module = if (path == ":") "root" else path.removePrefix(":").replace(':', '/')
    layout.buildDirectory.set(accountBuildRoot.resolve(module))
}
