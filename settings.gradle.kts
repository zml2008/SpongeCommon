rootProject.name = "SpongeCommon"

include(":SpongeAPI")
include(":vanilla")
pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://files.minecraftforge.net/maven")
        maven("https://repo.spongepowered.org/maven")
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id.startsWith("org.spongepowered.gradle.")) {
                val version = requested.version ?: "0.11.3-SNAPSHOT"
                useModule("org.spongepowered:SpongeGradle:$version")
            }
        }
    }

}