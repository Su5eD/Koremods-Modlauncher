pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name = "MinecraftForge"
            url = uri("https://maven.minecraftforge.net")
        }
        maven {
            name = "Garden of Fancy"
            url = uri("https://maven.gofancy.wtf/releases")
        }
    }

    plugins {
        val kotlinVersion: String by settings
        kotlin("jvm") version kotlinVersion apply false
        id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.+"
    }
}

rootProject.name = "koremods-modlauncher"

include("koremods-test")