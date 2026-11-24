import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import java.io.File

plugins {
    id("com.android.application") apply false
    id("com.android.library") apply false
    id("org.jetbrains.kotlin.android") apply false
    id("org.jetbrains.kotlin.plugin.serialization") apply false
}

// Settings has already selected the account/checkout cache namespace.
val accountBuildRoot = requireNotNull(gradle.startParameter.projectCacheDir).parentFile.resolve("build")
allprojects {
    val module = if (path == ":") "root" else path.removePrefix(":").replace(':', '/')
    layout.buildDirectory.set(accountBuildRoot.resolve(module))
}

val accountHome = file(System.getProperty("user.home"))
val xdgConfig = providers.environmentVariable("XDG_CONFIG_HOME").orNull?.let(::File)
    ?: File(accountHome, ".config")
val xdgDesktop = File(xdgConfig, "user-dirs.dirs").takeIf { it.isFile }?.readLines()
    ?.firstOrNull { it.trimStart().startsWith("XDG_DESKTOP_DIR=") }?.substringAfter('=')?.trim()
    ?.removeSurrounding("\"")?.replace("\$HOME", accountHome.path)?.replace("\${HOME}", accountHome.path)
val desktopDirectory = providers.gradleProperty("mukhtabir.desktopDir").orNull
    ?: providers.environmentVariable("MUKHTABIR_DESKTOP_DIR").orNull
    ?: xdgDesktop?.takeIf { File(it).isAbsolute && File(it) != accountHome }
    ?: File(accountHome, "Desktop").path
require(File(desktopDirectory).isAbsolute) { "Desktop export directory must be absolute." }

mapOf(":app" to "Mukhtabir", ":demo" to "Mukhtabir/demo").forEach { (modulePath, exportPath) ->
    val module = project(modulePath)
    module.plugins.withId("com.android.application") {
        val androidComponents = module.extensions.getByType<ApplicationAndroidComponentsExtension>()
        androidComponents.onVariants(androidComponents.selector().all()) { variant ->
            val suffix = variant.name.replaceFirstChar { it.uppercaseChar() }
            val versionName = variant.outputs.single().versionName
            // Use the packaging task's artifact provider so a failed build cannot export an old APK.
            val apkExport = module.tasks.register<Copy>("export${suffix}Apk") {
                group = "build"
                description = "Copy packaged APKs to this account's Desktop."
                from(variant.artifacts.get(SingleArtifact.APK))
                include("*.apk")
                into(File(desktopDirectory, "$exportPath/${variant.name}"))
                rename("^(.+\\.apk)$", "Mukhtabir-${versionName.get()}-\$1")
            }
            module.tasks.matching { it.name == "assemble$suffix" || it.name == "install$suffix" }
                .configureEach { dependsOn(apkExport) }

            if (modulePath == ":app") {
                val bundleExport = module.tasks.register<Copy>("export${suffix}Bundle") {
                    group = "build"
                    description = "Copy packaged AABs to this account's Desktop."
                    from(variant.artifacts.get(SingleArtifact.BUNDLE))
                    include("*.aab")
                    into(File(desktopDirectory, "$exportPath/bundles/${variant.name}"))
                    rename("^(.+\\.aab)$", "Mukhtabir-${versionName.get()}-\$1")
                }
                module.tasks.matching { it.name == "bundle$suffix" }.configureEach { dependsOn(bundleExport) }
            }
        }
    }
}
