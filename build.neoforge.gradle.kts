import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    `java-library`
    `maven-publish`
    id("net.neoforged.moddev") version "2.0.140"
}

val modId = project.property("mod.id") as String
val modName = project.property("mod.name") as String
val modVersion = project.property("mod.version") as String
val modDescription = project.property("mod.description") as String
val modAuthors = project.property("mod.authors") as String
val modLicense = project.property("mod.license") as String
val minecraftRange = project.property("mod.minecraft_range") as String
val loaderRange = project.property("mod.loader_range") as String
val packFormat = project.property("mod.pack_format") as String
val architecturyVersion = project.property("deps.architectury") as String
val jeiVersion = project.property("deps.jei") as String

group = project.property("mod.group") as String
version = "$modVersion+${sc.current.version}"

base {
    archivesName.set("$modId-neoforge")
}

repositories {
    maven("https://maven.architectury.dev/") { name = "Architectury" }
    maven("https://maven.blamejared.com/") { name = "Jared" }
    mavenCentral()
}

dependencies {
    implementation("dev.architectury:architectury-neoforge:$architecturyVersion")
    compileOnly("mezz.jei:jei-${sc.current.version}-common-api:$jeiVersion")
    compileOnly("mezz.jei:jei-${sc.current.version}-neoforge-api:$jeiVersion")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

neoForge {
    version = project.property("deps.neoforge") as String

    runs {
        register("client") {
            client()
            gameDirectory = rootProject.file("run")
        }
        register("server") {
            server()
            gameDirectory = rootProject.file("run")
            programArgument("--nogui")
        }
    }

    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
        }
    }

    unitTest {
        enable()
        testedMod = mods.getByName(modId)
    }
}

java {
    withSourcesJar()
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            val jitpackBuild = System.getenv("JITPACK") == "true"
            groupId = if (jitpackBuild) project.group.toString()
                else System.getenv("GROUP") ?: project.group.toString()
            artifactId = if (jitpackBuild) project.name
                else System.getenv("ARTIFACT") ?: modId
            version = if (jitpackBuild) project.version.toString()
                else System.getenv("VERSION") ?: project.version.toString()
            from(components["java"])
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.named<ProcessResources>("processResources") {
    val props = mapOf(
        "mod_id" to modId,
        "mod_name" to modName,
        "mod_version" to modVersion,
        "mod_description" to modDescription,
        "mod_authors" to modAuthors,
        "mod_license" to modLicense,
        "minecraft_range" to minecraftRange,
        "loader_range" to loaderRange,
        "architectury_version" to architecturyVersion,
        "pack_format" to packFormat,
        "construction_highlight_iview_uniform" to ""
    )
    inputs.properties(props)
    filesMatching(listOf(
        "META-INF/neoforge.mods.toml",
        "pack.mcmeta",
        "assets/axiomata/shaders/core/construction_highlight.json"
    )) { expand(props) }
    filesMatching(listOf("data/*/recipes/*.json", "data/*/recipe/*.json")) {
        var insideResult = false
        filter { line ->
            if (line.contains("\"result\"")) {
                insideResult = true
            }
            if (insideResult && line.contains("\"item\"")) {
                insideResult = false
                line.replace("\"item\"", "\"id\"")
            } else {
                line
            }
        }
    }
    exclude("META-INF/mods.toml")
    includeEmptyDirs = false
    eachFile {
        path = path
            .replace("/recipes/", "/recipe/")
            .replace("/loot_tables/", "/loot_table/")
            .replace("/structures/", "/structure/")
            .replace("/tags/blocks/", "/tags/block/")
            .replace("/tags/items/", "/tags/item/")
    }
}

tasks.named("createMinecraftArtifacts") {
    dependsOn("stonecutterGenerate")
}

tasks.withType<Jar>().configureEach {
    from(rootProject.file("LICENSE")) { into("META-INF") }
    from(rootProject.file("LICENSE-ASSETS.md")) { into("META-INF") }
}

tasks.register<Copy>("buildAndCollect") {
    dependsOn("build")
    from(tasks.named<Jar>("jar"), tasks.named<Jar>("sourcesJar"))
    into(rootProject.layout.buildDirectory.dir("libs/$modVersion"))
}
