plugins {
    java
    id("net.neoforged.gradle.userdev")
    id("wtf.gofancy.koremods.gradle")
}

val minecraftVersion: String by project
val neoVersion: String by project

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

runs {
    create("server") {
        systemProperties(
            mapOf(
                "forge.logging.markers" to "REGISTRIES",
                "forge.logging.console.level" to "debug",
                "forge.enabledGameTestNamespaces" to "koremods_test"
            )
        )
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
    implementation(group = "net.neoforged", name = "neoforge", version = neoVersion)

    koremods(project(":"))
}
