@file:Suppress("UnstableApiUsage")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.matthewprenger.cursegradle.CurseArtifact
import com.matthewprenger.cursegradle.CurseProject
import fr.brouillard.oss.jgitver.GitVersionCalculator
import fr.brouillard.oss.jgitver.Strategies
import net.minecraftforge.gradle.common.util.RunConfig
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.RevSort
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

buildscript {
    dependencies {
        classpath(group = "org.eclipse.jgit", name = "org.eclipse.jgit", version = "6.1.+")
        classpath(group = "fr.brouillard.oss", name = "jgitver", version = "0.14.0")
    }
}

plugins {
    kotlin("jvm")
    `maven-publish`
    id("net.minecraftforge.gradle") version "5.1.+"
    id("com.github.johnrengelman.shadow") version "7.1.0" apply false
    id("org.cadixdev.licenser") version "0.6.1"
    id("com.matthewprenger.cursegradle") version "1.4.+"
    id("com.modrinth.minotaur") version "2.+"
}

group = "wtf.gofancy.koremods"
version = getGitVersion()

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    withSourcesJar()
}

val SCRIPT_COMPILER_CLASSPATH_USAGE = "script-compiler-classpath"
val kotlinVersion: String by project
val minecraftVersion: String by project
val forgeVersion: String by project
val curseForgeProjectID: String by project
val modrinthProjectID: String by project
val publishReleaseType = System.getenv("PUBLISH_RELEASE_TYPE") ?: "release"
val changelogText = generateChangelog(1, false)

val manifestAttributes = mapOf(
    "Specification-Title" to project.name,
    "Specification-Vendor" to "Garden of Fancy",
    "Specification-Version" to 1,
    "Implementation-Title" to project.name,
    "Implementation-Version" to project.version,
    "Implementation-Vendor" to "Garden of Fancy"
)

val mod: SourceSet by sourceSets.creating
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

    runtimeElements {
        setExtendsFrom(setOf(script))

        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(SCRIPT_COMPILER_CLASSPATH_USAGE))
    }

    apiElements {
        setExtendsFrom(setOf(script))
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
                fullJar.archiveFile.get().asFile.absolutePath
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

    script(group = "wtf.gofancy.koremods", name = "koremods-script", version = "0.4.12")
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

val modJar by tasks.creating(Jar::class) {
    dependsOn("modClasses")
    
    from(mod.output)
    manifest.attributes(manifestAttributes)
    
    archiveClassifier.set("mod")
}

val fullJar by tasks.creating(Jar::class) {
    dependsOn(tasks.jar, kotlinDepsJar, modJar)
    val kotlinDeps = kotlinDepsJar.archiveFile
    val modJarFile = modJar.archiveFile

    from(zipTree(tasks.jar.get().archiveFile))
    doFirst { from(zipTree(script.singleFile)) }
    from(kotlinDeps)
    from(modJarFile)

    manifest {
        attributes(manifestAttributes)
        attributes(
            "Additional-Dependencies-Kotlin" to kotlinDeps.get().asFile.name,
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

        archiveClassifier.set("slim")
    }

    whenTaskAdded {
        if (name == "prepareRuns") dependsOn(fullJar)
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    withType<Wrapper> {
        gradleVersion = "7.4.2"
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
        changelog = changelogText
        releaseType = publishReleaseType
        mainArtifact(fullJar, closureOf<CurseArtifact> {
            displayName = "Koremods ${project.version}"
        })
        addGameVersion("Forge")
        addGameVersion(minecraftVersion)
    })
}

modrinth {
    token.set(System.getenv("MODRINTH_TOKEN"))
    projectId.set(modrinthProjectID)
    versionName.set("Koremods ${project.version}")
    versionType.set(publishReleaseType)
    uploadFile.set(fullJar)
    gameVersions.addAll(minecraftVersion)
    changelog.set(changelogText)
}

fun getGitVersion(): String {
    val jgitver = GitVersionCalculator.location(rootDir)
        .setNonQualifierBranches("1.18.x")
        .setStrategy(Strategies.SCRIPT)
        .setScript("print \"\${metadata.CURRENT_VERSION_MAJOR};\${metadata.CURRENT_VERSION_MINOR};\${metadata.CURRENT_VERSION_PATCH + metadata.COMMIT_DISTANCE}\"")
    return jgitver.version
}

data class ChangelogTag(val name: String, val objectId: ObjectId)

fun generateChangelog(depth: Int = 0, includeStart: Boolean = true): String {
    try {
        val changelog = StringBuilder()
        val git = Git.open(project.rootDir)
        changelog.appendLine("${git.repository.branch} changelog") // changelog title

        val head = git.repository.resolve(Constants.HEAD)
        val walk = RevWalk(git.repository).apply {
            sort(RevSort.REVERSE, true)
            markStart(git.repository.parseCommit(head))
        }
        val tail = walk.next() // First branch commit (tail)
        // Get all tag refs and map them to their respective commits
        val tags: Map<RevCommit, Ref> = git.tagList().call().associateBy { tag -> git.repository.parseCommit(tag.objectId) }
        // Find all major.minor tags and sort them by their respective commit's date, descending
        val versionTagsSorted: List<ChangelogTag> = tags
            .mapValues { (_, ref) -> ChangelogTag(Repository.shortenRefName(ref.name), ref.objectId) }
            .filterValues { tag -> tag.name.split(".").size == 2 }
            .toSortedMap(compareByDescending { commit -> commit.authorIdent.`when` })
            .values
            .toList()
            .let { list -> if (includeStart) list + ChangelogTag("0.0", tail) else list } // Add first branch commit
            .let { list -> if (depth > 0) list.take(depth) else list }
        // Zip each version tag with the previous one and a list of git commits between them
        val versionTagsSortedLog = versionTagsSorted
            .mapIndexed { index, tag ->
                val previous =
                    if (index == 0) head
                    else git.repository.parseCommit(versionTagsSorted[index - 1].objectId).getParent(0)
                val tagCommit = git.repository.parseCommit(tag.objectId) 
                val log = git.log().run {
                    if (tagCommit.parentCount > 0) not(tagCommit.getParent(0)) // Tag commit parent
                    add(tagCommit) // Tag commit
                    add(previous) // Previous tag commit parent or head
                    call().toList()
                }
                Pair(tag, log)
            }
        // Get max tag string length
        val maxLength = versionTagsSortedLog.maxOf { (tag, log) ->
            "${tag.name}.${log.size}".length
        }
        versionTagsSortedLog
            .forEach { (tag, log) ->
                changelog.appendLine(tag.name)
                changelog.appendLine("======")
                
                log.forEachIndexed { index, commit ->
                    val tagName =
                        if (index == log.lastIndex) "${tag.name}.0"
                        else tags[commit]?.let { Repository.shortenRefName(it.name) }
                            ?: "${tag.name}.${log.lastIndex - index}"
                    val pad = maxLength - tagName.length
                    val extendedPad = " ".repeat(3 + tagName.length + pad)
                    commit.fullMessage.lines()
                        .filterNot(String::isEmpty)
                        .forEachIndexed { idx, line ->
                            if (idx == 0) {
                                val firstPad = " ".repeat(pad)
                                changelog.appendLine(" - $tagName$firstPad $line")
                            }
                            else changelog.appendLine("$extendedPad $line")
                        }
                }
                changelog.appendLine()
            }
        walk.close()
        return changelog.toString()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return ""
}
