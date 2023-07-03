plugins {
    java
    id("net.minecraftforge.gradle")
    id("wtf.gofancy.koremods.gradle")
}

val minecraftVersion: String by project
val forgeVersion: String by project

minecraft {
    mappings("official", minecraftVersion)

    runs {
        create("gameTestServer") {
            properties(
                mapOf(
                    "forge.logging.markers" to "REGISTRIES",
                    "forge.logging.console.level" to "debug",
                    "forge.enabledGameTestNamespaces" to "koremods_test"
                )
            )
            workingDirectory = project.file("run").canonicalPath
        }
    }
}

repositories {
    mavenCentral()
    maven {
        name = "Garden of Fancy"
        url = uri("https://maven.gofancy.wtf/releases")
    }
}

dependencies {
    minecraft(group = "net.minecraftforge", name = "forge", version = "$minecraftVersion-$forgeVersion")

    koremods(project(":"))
}
