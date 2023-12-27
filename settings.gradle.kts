pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name = "Garden of Fancy"
            url = uri("https://maven.gofancy.wtf/releases")
        }
        maven {
            name = "NeoForged"
            url = uri("https://maven.neoforged.net/releases")
        }
        mavenLocal()
    }

    plugins {
        val kotlinVersion: String by settings
        kotlin("jvm") version kotlinVersion apply false
        id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.+"
    }
}

rootProject.name = "koremods-modlauncher"

include("koremods-test")