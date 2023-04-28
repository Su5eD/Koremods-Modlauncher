pluginManagement { 
    repositories { 
        gradlePluginPortal()
        maven("https://maven.minecraftforge.net")
        maven("https://maven.gofancy.wtf/releases")
    }
    
    plugins {
        val kotlinVersion: String by settings
        kotlin("jvm") version kotlinVersion apply false
    }
}

rootProject.name = "koremods-modlauncher"
