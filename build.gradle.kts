import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import fr.brouillard.oss.jgitver.GitVersionCalculator
import fr.brouillard.oss.jgitver.Strategies
import net.minecraftforge.gradle.common.util.RunConfig
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.util.*

buildscript {
    dependencies {
        classpath(group = "fr.brouillard.oss", name = "jgitver", version = "0.14.0")
    }
}

plugins {
    kotlin("jvm")
    `maven-publish`
    id("net.minecraftforge.gradle") version "5.1.+"
    id("com.github.johnrengelman.shadow") version "7.1.0" apply false
    id("org.cadixdev.licenser") version "0.6.1"
}

group = "wtf.gofancy.koremods"
version = getGitVersion()

val kotlinVersion: String by project

val script: Configuration by configurations.creating {
    attributes { 
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.SHADOWED))
    }
}
val shadeKotlin: Configuration by configurations.creating

val manifestAttributes = mapOf(
    "Specification-Title" to project.name,
    "Specification-Vendor" to "Garden of Fancy",
    "Specification-Version" to 1,
    "Implementation-Title" to project.name,
    "Implementation-Version" to project.version,
    "Implementation-Vendor" to "Garden of Fancy",
    "FMLModType" to "LIBRARY"
)

configurations {
    compileOnly {
        extendsFrom(script)
    }

    runtimeElements {
        setExtendsFrom(emptySet())
    }
    
    apiElements {
        setExtendsFrom(setOf(script))
        
        afterEvaluate {
            outgoing.artifacts.clear()
            outgoing.artifact(slimJar)
        }
    }

    runtimeClasspath {
        exclude(group = "org.jetbrains.kotlin")
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    withSourcesJar()
}

minecraft {
    mappings("official", "1.18.1")

    runs {
        val config = Action<RunConfig> {
            properties(
                mapOf(
                    "forge.logging.markers" to "REGISTRIES",
                    "forge.logging.console.level" to "debug"
                )
            )
            workingDirectory = project.file("run").canonicalPath
            forceExit = false

            lazyToken("minecraft_classpath") {
                tasks.jar.get().archiveFile.get().asFile.absolutePath
            }
        }

        create("client", config)
        create("server", config)
    }
}

repositories {
    mavenCentral()
    maven("https://su5ed.jfrog.io/artifactory/maven")
    mavenLocal()
}

dependencies {
    minecraft(group = "net.minecraftforge", name = "forge", version = "1.18.2-40.0.36")

    shadeKotlin(kotlin("stdlib"))
    shadeKotlin(kotlin("stdlib-jdk8"))
    shadeKotlin(kotlin("reflect"))
    shadeKotlin(kotlin("scripting-common"))
    shadeKotlin(kotlin("scripting-jvm"))
    shadeKotlin(kotlin("scripting-jvm-host")).cast<ExternalModuleDependency>().run {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-scripting-compiler-embeddable")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-compiler-embeddable")
    }

    script(group = "wtf.gofancy.koremods", name = "koremods-script", version = "0.3.2")
}

license {
    header("NOTICE")

    properties {
        set("year", "2021-${Calendar.getInstance().get(Calendar.YEAR)}")
        set("name", "Garden of Fancy")
        set("app", "Koremods")
    }
}

val kotlinDepsJar by tasks.creating(ShadowJar::class) {
    configurations = listOf(shadeKotlin)
    exclude("META-INF/versions/**")

    dependencies {
        exclude(dependency("net.java.dev.jna:jna"))
    }

    archiveBaseName.set("koremods-deps-kotlin")
    archiveVersion.set(kotlinVersion)
}

val slimJar by tasks.creating(Jar::class) {
    dependsOn("classes")

    from(sourceSets.main.get().output)
    manifest.attributes(manifestAttributes)

    archiveClassifier.set("slim")
}

tasks {
    jar {
        dependsOn(kotlinDepsJar)
        val kotlinDeps = kotlinDepsJar.archiveFile
        
        doFirst { from(zipTree(script.singleFile)) }
        from(kotlinDeps)

        manifest {
            attributes(manifestAttributes)
            attributes(
                "Additional-Dependencies-Kotlin" to kotlinDeps.get().asFile.name
            )
        }
    }

    whenTaskAdded {
        if (name == "prepareRuns") dependsOn(jar)
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    withType<Wrapper> {
        gradleVersion = "7.4.2"
        distributionType = Wrapper.DistributionType.BIN
    }
}

publishing {
    publications {
        create<MavenPublication>(project.name) {
            from(components["java"])

            suppressAllPomMetadataWarnings()
        }
    }

    repositories {
        val ciJobToken = System.getenv("CI_JOB_TOKEN")
        val deployToken = project.findProperty("DEPLOY_TOKEN") as String?
        if (ciJobToken != null || deployToken != null) {
            maven {
                name = "GitLab"
                url = uri("https://gitlab.com/api/v4/projects/32090725/packages/maven")

                credentials(HttpHeaderCredentials::class) {
                    if (ciJobToken != null) {
                        name = "Job-Token"
                        value = ciJobToken
                    } else {
                        name = "Deploy-Token"
                        value = deployToken
                    }
                }
                authentication {
                    create("header", HttpHeaderAuthentication::class)
                }
            }
        }

        if (project.hasProperty("artifactoryPassword")) {
            maven {
                name = "artifactory"
                url = uri("https://su5ed.jfrog.io/artifactory/maven")
                credentials {
                    username = project.properties["artifactoryUser"] as String
                    password = project.properties["artifactoryPassword"] as String
                }
            }
        }
    }
}

fun getGitVersion(): String {
    val jgitver = GitVersionCalculator.location(rootDir)
        .setNonQualifierBranches("master")
        .setStrategy(Strategies.SCRIPT)
        .setScript("print \"\${metadata.CURRENT_VERSION_MAJOR};\${metadata.CURRENT_VERSION_MINOR};\${metadata.CURRENT_VERSION_PATCH + metadata.COMMIT_DISTANCE}\"")
    return jgitver.version
}
