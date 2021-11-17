import net.minecraftforge.gradle.common.util.RunConfig
import java.time.LocalDateTime

plugins {
    kotlin("jvm")
    id("net.minecraftforge.gradle") version "5.1.+"
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

evaluationDependsOn(":koremods-script")

val scriptProj = project(":koremods-script")
val repackPackagePath: String by project
val relocatePackages: ((String, String) -> Unit) -> Unit by scriptProj.extra

minecraft {
    mappings("official", "1.16.5")

    runs {
        val config = Action<RunConfig> {
            properties(mapOf(
                "forge.logging.markers" to "REGISTRIES",
                "forge.logging.console.level" to "debug"
            ))
            workingDirectory = project.file("run").canonicalPath
        }

        create("client", config)
        create("server", config)
    }
}

configurations.mavenRuntime {
    extendsFrom(scriptProj.configurations.mavenRuntime.get())
}

afterEvaluate { 
    sourceSets.main {
        runtimeClasspath = runtimeClasspath.filter { !output.files.contains(it) } + files(tasks["fullJar"])
    }
}

repositories {
    maven("https://su5ed.jfrog.io/artifactory/maven")
}

dependencies {
    minecraft(group = "net.minecraftforge", name = "forge", version = "1.16.5-36.2.0")
    
    compileOnly(scriptProj)
    compileOnly(scriptProj.sourceSets["splash"].output)
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
        dependsOn("fullJar")
        isEnabled = false
    }
    
    shadowJar {
        configurations = emptyList()
        
        manifest.attributes(manifestAttributes)
        relocatePackages(::relocate)
        
        archiveClassifier.set("shade")
    }
    
    register<Jar>("fullJar") {
        val shadowJar = scriptProj.tasks.getByName<Jar>("shadowJar")
        val kotlinDepsJar = scriptProj.tasks.getByName<Jar>("kotlinDepsJar")
        dependsOn(project.tasks.shadowJar, shadowJar, kotlinDepsJar)
        
        from(zipTree(project.tasks.shadowJar.get().archiveFile))
        from(zipTree(shadowJar.archiveFile))
        from(kotlinDepsJar.archiveFile)
        
        manifest {
            attributes(manifestAttributes)
            attributes("Additional-Dependencies-Kotlin" to kotlinDepsJar.archiveFile.get().asFile.name)
        }
    }
}

configurations.all { 
    outgoing.artifacts.removeIf { 
        it.buildDependencies.getDependencies(null).contains(tasks["jar"])
    }
    outgoing.artifact(tasks["fullJar"])
}
