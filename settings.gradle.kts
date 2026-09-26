pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.minecraftforge.net/") { name = "Forge" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.architectury.dev/") { name = "Architectury" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        fun target(project: String, loader: String, minecraft: String = project) {
            version("$project-$loader", minecraft).buildscript("build.$loader.gradle.kts")
        }

        target("1.20.1", "forge")
        target("1.21.1", "neoforge")
        vcsVersion = "1.21.1-neoforge"
    }
}

rootProject.name = "Axiomata"

