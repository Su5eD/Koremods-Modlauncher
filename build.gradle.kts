@file:Suppress("UnstableApiUsage")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import me.modmuss50.mpp.ReleaseType
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import wtf.gofancy.koremods.gradle.KoremodsGradlePlugin
import java.util.*

plugins {
    kotlin("jvm")
    `maven-publish`
    id("net.neoforged.gradle.userdev") version "7.0.+"
    id("wtf.gofancy.koremods.gradle") version "2.0.0" apply false
    id("com.github.johnrengelman.shadow") version "8.1.+" apply false
    id("org.cadixdev.licenser") version "0.6.1"
    id("wtf.gofancy.git-changelog") version "1.1.+"
    id("me.qoomon.git-versioning") version "6.3.+"
    id("me.modmuss50.mod-publish-plugin") version "0.3.+"
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
val neoVersion: String by project
val curseForgeProjectID: String by project
val modrinthProjectID: String by project

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
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

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
        extendsFrom(implementation.get())
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

println("Configured version: $version, Java: ${System.getProperty("java.version")}, JVM: ${System.getProperty("java.vm.version")} (${System.getProperty("java.vendor")}), Arch: ${System.getProperty("os.arch")}")
runs {
    configureEach {
        systemProperty("forge.logging.markers", "REGISTRIES")
        systemProperty("forge.logging.console.level", "debug")

        modSource(mod)
    }

    create("client")

    create("server") {
        programArgument("--nogui")
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
        gradleVersion = "8.1.1"
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

publishMods {
    file.set(fullJar.flatMap { it.archiveFile })
    changelog.set(provider { project.changelog.generateChangelog(1, true) })
    type.set(providers.environmentVariable("PUBLISH_RELEASE_TYPE").map(ReleaseType::of).orElse(ReleaseType.STABLE))
    modLoaders.add("forge")
    dryRun.set(!providers.environmentVariable("CI").isPresent)
    displayName.set("Koremods ${project.version}")

    curseforge {
        accessToken.set(providers.environmentVariable("CURSEFORGE_TOKEN"))
        projectId.set(curseForgeProjectID)
        minecraftVersions.add(minecraftVersion)
    }
    modrinth {
        accessToken.set(providers.environmentVariable("MODRINTH_TOKEN"))
        projectId.set(modrinthProjectID)
        minecraftVersions.add(minecraftVersion)
    }
}
