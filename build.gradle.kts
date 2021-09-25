import java.time.LocalDateTime
import net.minecraftforge.gradle.common.util.RunConfig

plugins {
    kotlin("jvm")
    id("net.minecraftforge.gradle") version "5.1.+"
}

evaluationDependsOn(":script")

minecraft {
    mappings("official", "1.16.5")

    runs {
        val config = Action<RunConfig> {
            properties(mapOf(
                "forge.logging.markers" to "SCAN,REGISTRIES",
                "forge.logging.console.level" to "debug"
            ))
            workingDirectory = project.file("run").canonicalPath
            source(sourceSets.main.get())
        }

        create("client", config)
        create("server", config)
    }
}

repositories {
    maven("https://su5ed.jfrog.io/artifactory/maven")
}

dependencies {
    minecraft(group = "net.minecraftforge", name = "forge", version = "1.16.5-36.2.0")
    
    implementation(project(":script"))
}

val manifestAttributes = mapOf(
    "Specification-Title" to "Koremods-Modlauncher",
    "Specification-Vendor" to "Su5eD",
    "Specification-Version" to 1,
    "Implementation-Title" to "Koremods-Modlauncher",
    "Implementation-Version" to project.version,
    "Implementation-Vendor" to "Su5eD",
    "Implementation-Timestamp" to LocalDateTime.now()
)

tasks {
    jar {
        manifest.attributes(manifestAttributes)
    }
    
    register<Jar>("fullJar") {
        from(zipTree(jar.get().archiveFile))
        from(zipTree(project(":script").tasks.getByName<Jar>("shadowJar").archiveFile))
        
        manifest.attributes(manifestAttributes)
        
        archiveClassifier.set("full")
    }
    
    processResources {
        inputs.property("version", project.version)
        
        filesMatching("koremods.info") {
            expand("version" to project.version)
        }
    }
}

publishing {
    publications { 
        named<MavenPublication>(project.name) {
            artifact(tasks.getByName("fullJar"))
        }
    }
}

artifacts { 
    archives(tasks.getByName("fullJar"))
}
