import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    `maven-publish`
    id("dev.architectury.loom")
    id("architectury-plugin")
    id("com.gradleup.shadow")
}

val modId = project.property("mod.id") as String
val modName = project.property("mod.name") as String
val modVersion = project.property("mod.version") as String
val modGroup = project.property("mod.group") as String
val modDescription = project.property("mod.description") as String
val modAuthors = project.property("mod.authors") as String
val modLicense = project.property("mod.license") as String
val modDisplayUrl = "https://github.com/mess1re/axiomata"
val minecraftRange = project.property("mod.minecraft_range") as String
val fabricLoaderVersion = project.property("deps.fabric_loader") as String
val fabricApiVersion = project.property("deps.fabric_api") as String
val architecturyVersion = project.property("deps.architectury") as String
val javaVersion = project.property("mod.java").toString().toInt()
val mcVersion = sc.current.version

val commonPath = ":$mcVersion"
evaluationDependsOn(commonPath)
val commonProject = project(commonPath)

group = modGroup
version = "$modVersion+$mcVersion"

base {
    archivesName.set("$modId-fabric")
}

architectury {
    platformSetupLoomIde()
    fabric()
}

loom {
    silentMojangMappingsLicense()

    // Remap mixin targets directly in the jar, so neither common nor fabric mixins need a refmap.
    mixin {
        useLegacyMixinAp.set(false)
    }

    // Lets Fabric Loader treat the common output as part of this mod in development, e.g. to find the access widener.
    mods {
        maybeCreate("main").apply {
            sourceSet(sourceSets.main.get())
            sourceSet(commonProject.sourceSets.main.get(), commonProject)
        }
    }

    runs {
        named("client") {
            client()
            providers.gradleProperty("client.server").orNull?.let { programArgs("--quickPlayMultiplayer", it) }
            providers.gradleProperty("client.username").orNull?.let { programArgs("--username", it) }
            runDir(rootProject.file("run/$mcVersion-fabric").relativeTo(projectDir).path)
        }
        named("server") {
            server()
            runDir(rootProject.file("run/$mcVersion-fabric").relativeTo(projectDir).path)
            programArg("--nogui")
        }
    }
}

val common: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}
val shadowBundle: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}
configurations {
    compileClasspath.get().extendsFrom(common)
    runtimeClasspath.get().extendsFrom(common)
    named("developmentFabric").get().extendsFrom(common)
}

repositories {
    maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/") { content { includeGroupByRegex("software\\.bernie.*") } }
    maven("https://maven.nucleoid.xyz/") { content { includeGroup("eu.pb4") } }
    mavenCentral()
    maven("https://maven.architectury.dev/") { name = "Architectury" }
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
    maven("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/") {
        name = "Fuzs"
        content { includeGroup("fuzs.forgeconfigapiport") }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

    modImplementation("dev.architectury:architectury-fabric:$architecturyVersion")
    modImplementation(project.property("deps.forge_config_api_port") as String)
    modImplementation("eu.pb4:common-protection-api:1.0.0")
    include("eu.pb4:common-protection-api:1.0.0")
    modCompileOnly("software.bernie.geckolib:geckolib-fabric-$mcVersion:${project.property("deps.geckolib")}") { isTransitive = false }
    // GeckoLib's API uses javax.annotation; the source remapper must resolve those types too.
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    common(project(path = commonPath, configuration = "namedElements")) { isTransitive = false }
    shadowBundle(project(path = commonPath, configuration = "transformProductionFabric"))
}

java {
    withSourcesJar()
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaVersion)
}

tasks.named<ProcessResources>("processResources") {
    val props = mapOf(
        "mod_id" to modId,
        "mod_name" to modName,
        "mod_version" to modVersion,
        "mod_description" to modDescription,
        "mod_authors" to modAuthors,
        "mod_license" to modLicense,
        "mod_display_url" to modDisplayUrl,
        "minecraft_range" to minecraftRange,
        "fabric_loader_version" to fabricLoaderVersion,
        "fabric_api_version" to fabricApiVersion.substringBefore('+'),
        "architectury_version" to architecturyVersion,
        "config_api_version" to project.property("deps.forge_config_api_port").toString().substringAfterLast(':'),
        "java_version" to javaVersion,
        "java_compatibility" to "JAVA_$javaVersion"
    )
    inputs.properties(props)
    filesMatching(listOf("fabric.mod.json", "axiomata.fabric.mixins.json")) {
        expand(props)
        if (name == "axiomata.fabric.mixins.json" && mcVersion != "1.20.1") {
            filter { line -> if (line.contains("\"ServerStructureReachMixin\"")) "" else line }
        }
    }
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    configurations = listOf(shadowBundle)
    archiveClassifier.set("dev-shadow")
    exclude("architectury.common.json")
}

tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    inputFile.set(tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar").flatMap { it.archiveFile })
    archiveClassifier.set(null as String?)
}

tasks.named<JavaExec>("runServer") {
    standardInput = System.`in`
}

tasks.register<Copy>("buildAndCollect") {
    dependsOn("build")
    from(tasks.named("remapJar"), tasks.named("remapSourcesJar"))
    into(rootProject.layout.buildDirectory.dir("libs/$modVersion"))
}

// One artifact per loader and Minecraft version, so the Fabric and NeoForge 1.21.1 builds do not share a name.
publishing {
    publications {
        create<MavenPublication>("mod") {
            val jitpackBuild = System.getenv("JITPACK") == "true"
            groupId = if (jitpackBuild) project.group.toString() else System.getenv("GROUP") ?: project.group.toString()
            artifactId = if (jitpackBuild) "$mcVersion-fabric" else System.getenv("ARTIFACT") ?: "$modId-fabric-$mcVersion"
            version = if (jitpackBuild) project.version.toString() else System.getenv("VERSION") ?: project.version.toString()
            pom.withXml {
                val dependencies = asNode().appendNode("dependencies")
                for (coordinate in listOf(
                    "dev.architectury:architectury-fabric:$architecturyVersion",
                    "net.fabricmc.fabric-api:fabric-api:$fabricApiVersion",
                    project.property("deps.forge_config_api_port").toString()
                )) {
                    val parts = coordinate.split(':')
                    val dependency = dependencies.appendNode("dependency")
                    dependency.appendNode("groupId", parts[0])
                    dependency.appendNode("artifactId", parts[1])
                    dependency.appendNode("version", parts[2])
                    dependency.appendNode("scope", "runtime")
                }
            }
            artifact(tasks.named("remapJar"))
            artifact(tasks.named("remapSourcesJar")) { classifier = "sources" }
        }
    }
}

tasks.named<Jar>("sourcesJar") { from(commonProject.sourceSets.main.get().allSource) }
tasks.withType<Jar>().configureEach {
    if (!name.startsWith("remap")) {
        from(rootProject.file("LICENSE")) { into("META-INF") }
        from(rootProject.file("LICENSE-ASSETS.md")) { into("META-INF") }
    }
}

// Runtime integration tests are isolated from the distributed mod.
val testmod = sourceSets.create("testmod")
configurations.named(testmod.implementationConfigurationName) { extendsFrom(configurations.implementation.get()) }
configurations.named(testmod.runtimeOnlyConfigurationName) { extendsFrom(configurations.runtimeOnly.get()) }
testmod.compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output
testmod.runtimeClasspath += sourceSets.main.get().runtimeClasspath + sourceSets.main.get().output
loom.createRemapConfigurations(testmod)
dependencies {
    add("modTestmodImplementation", "net.fabricmc.fabric-api:fabric-gametest-api-v1:${if (mcVersion == "1.20.1") "1.2.15+1802ada577" else "2.0.4+6fc22b9919"}")
}
loom.mods {
    maybeCreate("axiomata_fabric_tests").sourceSet(testmod)
}
loom.runs {
    register("gameTest") {
        server()
        source(testmod)
        runDir(rootProject.file("run/$mcVersion-fabric-gametest").relativeTo(projectDir).path)
        vmArg("-Dfabric-api.gametest")
        vmArg("-Dfabric-api.gametest.report-file=${layout.buildDirectory.file("test-results/fabric-gametest.xml").get().asFile}")
    }
}
tasks.named<ProcessResources>("processTestmodResources") {
    inputs.property("mod_version", modVersion)
    filesMatching("fabric.mod.json") { expand("mod_version" to modVersion) }
}

tasks.withType<JavaExec>().matching { it.name == "runGameTest" }.configureEach {
    doFirst {
        val directory = rootProject.file("run/$mcVersion-fabric-gametest")
        directory.mkdirs()
        directory.resolve("eula.txt").writeText("eula=true\n")
        layout.buildDirectory.dir("test-results").get().asFile.mkdirs()
    }
}
