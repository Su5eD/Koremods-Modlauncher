pluginManagement { 
    repositories { 
        gradlePluginPortal()
        maven("https://maven.minecraftforge.net")
    }
    
    plugins {
        val kotlinVersion: String by settings
        kotlin("jvm") version kotlinVersion apply false
    }
    
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "net.minecraftforge.gradle") {
                useModule("${requested.id}:ForgeGradle:${requested.version}")
            }
        }
    }
}

rootProject.name = "koremods-modlauncher"
