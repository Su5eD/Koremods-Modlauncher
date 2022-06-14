pluginManagement { 
    repositories { 
        gradlePluginPortal()
        maven("https://maven.minecraftforge.net")
    }
    
    plugins {
        val kotlinVersion: String by settings
        kotlin("jvm") version kotlinVersion apply false
    }
}

rootProject.name = "koremods-modlauncher"
