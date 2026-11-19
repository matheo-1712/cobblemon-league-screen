pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        id("net.fabricmc.fabric-loom-remap") version providers.gradleProperty("loom_version")
        id("org.jetbrains.kotlin.jvm") version providers.gradleProperty("kotlin_version")
        id("me.modmuss50.mod-publish-plugin") version providers.gradleProperty("publish_plugin_version")
    }
}

// Nom du projet et du jar ; l'identifiant du mod (cobblemon_league_screen) est dans fabric.mod.json.
rootProject.name = "cobblemon-league-screen"
