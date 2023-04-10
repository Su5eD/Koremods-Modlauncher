@file:Suppress("UnstableApiUsage")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.matthewprenger.cursegradle.CurseArtifact
import com.matthewprenger.cursegradle.CurseProject
import net.minecraftforge.gradle.common.util.RunConfig
import net.minecraftforge.jarjar.metadata.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import wtf.gofancy.changelog.generateChangelog
import wtf.gofancy.koremods.gradle.KoremodsGradlePlugin
import java.util.*

plugins {
    kotlin("jvm")
    `maven-publish`
    id("net.minecraftforge.gradle") version "5.1.+"
    id("wtf.gofancy.koremods.gradle") version "0.1.23" apply false
    id("com.github.johnrengelman.shadow") version "7.1.0" apply false
    id("org.cadixdev.licenser") version "0.6.1"
    id("wtf.gofancy.git-changelog") version "1.0.+"
    id("me.qoomon.git-versioning") version "6.3.+"
    id("com.matthewprenger.cursegradle") version "1.4.+"
    id("com.modrinth.minotaur") version "2.+"
}

group = "wtf.gofancy.koremods"
version = "0.0.0-SNAPSHOT"

gitVersioning.apply {
    rev {
        version = "\${describe.tag.version.major}.\${describe.tag.version.minor}.\${describe.tag.version.patch.plus.describe.distance}"
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    withSourcesJar()
}

val kotlinVersion: String by project
val koremodsScriptVersion: String by project
val minecraftVersion: String by project
val forgeVersion: String by project
val curseForgeProjectID: String by project
val modrinthProjectID: String by project
val publishReleaseType = System.getenv("PUBLISH_RELEASE_TYPE") ?: "release"
val changelogText = provider { generateChangelog(1, true) }

val manifestAttributes = mapOf(
    "Specification-Title" to project.name,
    "Specification-Vendor" to "Garden of Fancy",
    "Specification-Version" to 1,
    "Implementation-Title" to project.name,
    "Implementation-Version" to project.version,
    "Implementation-Vendor" to "Garden of Fancy"
)

val mod: SourceSet by sourceSets.creating
val service: SourceSet by sourceSets.creating
val script: Configuration by configurations.creating {
    attributes {
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.SHADOWED))
    }
}
val shadeKotlin: Configuration by configurations.creating
val embeddedRuntimeElements: Configuration by configurations.creating {
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EMBEDDED))
        attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named(TargetJvmEnvironment.STANDARD_JVM))
        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, java.toolchain.languageVersion.get().asInt())
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
    }

    afterEvaluate {
        outgoing.artifact(fullJar)
    }
}

val kotlinDepsJar by tasks.registering(ShadowJar::class) {
    configurations = listOf(shadeKotlin)
    exclude("META-INF/versions/**")

    dependencies {
        exclude(dependency("net.java.dev.jna:jna"))
    }

    archiveBaseName.set("koremods-deps-kotlin")
    archiveVersion.set(kotlinVersion)
}

val modJar by tasks.registering(Jar::class) {
    dependsOn("modClasses")

    from(mod.output)
    manifest.attributes(manifestAttributes)

    archiveClassifier.set("mod")
}

val serviceJar by tasks.registering(Jar::class) {
    dependsOn("serviceClasses")

    from(service.output)
    manifest.attributes(manifestAttributes)

    archiveClassifier.set("service")
}

configurations {
    all {
        if (System.getenv("REFRESH_DYNAMIC_VERSIONS").toBoolean()) {
            resolutionStrategy.cacheDynamicVersionsFor(0, TimeUnit.MILLISECONDS)
        }
    }

    implementation {
        extendsFrom(script)
    }

    "modImplementation" {
        extendsFrom(minecraft.get())
    }

    "serviceImplementation" {
        extendsFrom(implementation.get())
    }

    runtimeElements {
        setExtendsFrom(setOf(script))

        outgoing { 
            artifact(serviceJar)
        }
        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(KoremodsGradlePlugin.SCRIPT_COMPILER_CLASSPATH_USAGE))
    }

    apiElements {
        setExtendsFrom(setOf(script))
        outgoing { 
            artifact(serviceJar)
        }
    }
}

minecraft {
    mappings("official", minecraftVersion)

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
                fullJar.get().archiveFile.get().asFile.absolutePath
            }
        }

        create("client", config)
        create("server", config)
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

    shadeKotlin(kotlin("stdlib"))
    shadeKotlin(kotlin("stdlib-jdk8"))
    shadeKotlin(kotlin("reflect"))
    shadeKotlin(kotlin("scripting-common"))
    shadeKotlin(kotlin("scripting-jvm"))
    (shadeKotlin(kotlin("scripting-jvm-host")) as ExternalModuleDependency).run {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-scripting-compiler-embeddable")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-compiler-embeddable")
    }

    "modImplementation"(sourceSets.main.map { it.output })

    script(group = "wtf.gofancy.koremods", name = "koremods-script", version = koremodsScriptVersion)
}

license {
    header("NOTICE")

    properties {
        set("year", "2021-${Calendar.getInstance().get(Calendar.YEAR)}")
        set("name", "Garden of Fancy")
        set("app", "Koremods")
    }
}

val fullJar by tasks.registering(Jar::class) {
    dependsOn(tasks.jar, kotlinDepsJar, serviceJar, modJar)
    val kotlinDeps = kotlinDepsJar.flatMap(Jar::getArchiveFile)
    val modJarFile = modJar.flatMap(Jar::getArchiveFile)
    val serviceJarFile = serviceJar.flatMap(Jar::getArchiveFile)

    from(zipTree(tasks.jar.get().archiveFile))
    doFirst { from(zipTree(script.singleFile)) }
    from(kotlinDeps)
    from(serviceJarFile)
    from(modJarFile)

    manifest {
        attributes(manifestAttributes)
        attributes(
            "Additional-Dependencies-Kotlin" to kotlinDeps.get().asFile.name,
            "Additional-Dependencies-Service" to serviceJarFile.get().asFile.name,
            "Additional-Dependencies-Mod" to modJarFile.get().asFile.name
        )
    }
}

tasks {
    jar {
        manifest {
            attributes(manifestAttributes)
            attributes("FMLModType" to "LIBRARY")
        }

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        archiveClassifier.set("slim")
    }

    named<Jar>("sourcesJar") {
        from(service.allSource)
        from(mod.allSource)
    }

    whenTaskAdded {
        if (name == "prepareRuns") dependsOn(fullJar)
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    withType<Wrapper> {
        gradleVersion = "7.5.1"
        distributionType = Wrapper.DistributionType.BIN
    }

    assemble {
        dependsOn(fullJar)
    }
}

afterEvaluate {
    val javaComponent = components["java"] as AdhocComponentWithVariants
    javaComponent.addVariantsFromConfiguration(embeddedRuntimeElements) {
        mapToMavenScope("runtime")
    }
}

publishing {
    publications {
        register<MavenPublication>(project.name) {
            from(components["java"])
        }
    }

    repositories {
        val mavenUser = System.getenv("GOFANCY_MAVEN_USER")
        val mavenToken = System.getenv("GOFANCY_MAVEN_TOKEN")

        if (mavenUser != null && mavenToken != null) {
            maven {
                name = "gofancy"
                url = uri("https://maven.gofancy.wtf/releases")

                credentials {
                    username = mavenUser
                    password = mavenToken
                }
            }
        }
    }
}

curseforge {
    apiKey = System.getenv("CURSEFORGE_TOKEN") ?: "UNKNOWN"
    project(closureOf<CurseProject> {
        id = curseForgeProjectID
        changelogType = "markdown"
        changelog = changelogText.get()
        releaseType = publishReleaseType
        mainArtifact(fullJar.get(), closureOf<CurseArtifact> {
            displayName = "Koremods ${project.version}"
        })
        addGameVersion("Forge")
        addGameVersion(minecraftVersion)
        addGameVersion("1.19.2")
    })
}

modrinth {
    token.set(System.getenv("MODRINTH_TOKEN"))
    projectId.set(modrinthProjectID)
    versionName.set("Koremods ${project.version}")
    versionType.set(publishReleaseType)
    uploadFile.set(fullJar.get())
    gameVersions.addAll(minecraftVersion, "1.19.2")
    changelog.set(changelogText)
}
