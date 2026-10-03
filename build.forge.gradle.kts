import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    `java-library`
    `maven-publish`
    id("net.neoforged.moddev.legacyforge") version "2.0.140"
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
    archivesName.set("$modId-forge")
}

repositories {
    maven("https://maven.architectury.dev/") { name = "Architectury" }
    maven("https://maven.blamejared.com/") { name = "Jared" }
    mavenCentral()
}

dependencies {
    implementation("dev.architectury:architectury-forge:$architecturyVersion")
    compileOnly("mezz.jei:jei-${sc.current.version}-common-api:$jeiVersion")
    compileOnly("mezz.jei:jei-${sc.current.version}-forge-api:$jeiVersion")
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

configurations.matching { it.name == "reobfRuntimeElements" }.configureEach {
    extendsFrom(configurations.implementation.get())
}

// LegacyForge does not add the mapped game and loader to the JUnit source set.
sourceSets.test {
    compileClasspath += sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().runtimeClasspath
}

legacyForge {
    version = project.property("deps.forge") as String

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

}

java {
    withSourcesJar()
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
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

sourceSets.test {
    java.exclude("**/collision/**")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
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
        "construction_highlight_iview_uniform" to
            """{ "name": "IViewRotMat", "type": "matrix3x3", "count": 9, "values": [ 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0 ] },"""
    )
    inputs.properties(props)
    filesMatching(listOf(
        "META-INF/mods.toml",
        "pack.mcmeta",
        "assets/axiomata/shaders/core/construction_highlight.json"
    )) { expand(props) }
    exclude("META-INF/neoforge.mods.toml")
}

// Mixins: the pathfinder has to be told that model geometry is in the way, and there is no hook
// for that. Wired the way the sibling mods wire theirs, on the same plugin.
mixin {
    add(sourceSets.main.get(), "$modId.refmap.json")
    config("$modId.mixins.json")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf("MixinConfigs" to "$modId.mixins.json"))
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
