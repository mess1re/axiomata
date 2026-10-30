plugins {
    java
    id("net.neoforged.moddev") version "2.0.140"
}

evaluationDependsOn(":neoforge:1.21.1")
evaluationDependsOn(":1.21.1")
val common = project(":1.21.1")
val platformProject = project(":neoforge:1.21.1")
val libraryJar = platformProject.tasks.named("remapJar")

repositories {
    mavenCentral()
    maven("https://maven.architectury.dev/")
}

dependencies {
    compileOnly(files(libraryJar.map { it.outputs.files }))
    implementation("dev.architectury:architectury-neoforge:13.0.11")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Load the assembled production mod and tests in one JPMS module, so package-private
// test access works and no duplicate Axiomata package is exported by a test mod.
val unpackedMod = layout.buildDirectory.dir("production-mod")
val unpackMod = tasks.register<Sync>("unpackMod") {
    dependsOn(libraryJar)
    from(libraryJar.map { zipTree(it.outputs.files.singleFile) })
    into(unpackedMod)
}
sourceSets.main { output.dir(mapOf("builtBy" to unpackMod), unpackedMod) }

sourceSets.test {
    java.setSrcDirs(common.extensions.getByType<SourceSetContainer>().getByName("test").java.srcDirs +
        listOf(rootProject.file("neoforge/src/test/java")))
}

neoForge {
    version = "21.1.248"
    mods { register("axiomata") { sourceSet(sourceSets.main.get()) } }
    unitTest {
        enable()
        testedMod = mods.getByName("axiomata")
    }
}
java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    dependsOn(common.tasks.matching { it.name.startsWith("stonecutterGenerate") })
}
tasks.withType<Test>().configureEach { useJUnitPlatform() }
