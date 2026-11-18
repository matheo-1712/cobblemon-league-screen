import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("net.fabricmc.fabric-loom-remap")
    id("org.jetbrains.kotlin.jvm")
}

base {
    archivesName.set(rootProject.name)
}

loom {
    runs.configureEach {
        generateRunConfig.set(true)
        preferGradleTask.set(true)
    }
    mods {
        create("cobblemon_league_screen") {
            sourceSet(sourceSets.main.get())
        }
    }
}

repositories {
    // Cobblemon est pris sur le Maven de Modrinth.
    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")
        content {
            includeGroup("maven.modrinth")
        }
    }
}

/**
 * L'archive de Cobblemon seule, sans remappage : elle ne sert qu'à en extraire les
 * bibliothèques qu'elle embarque (voir `cobblemonLibs`).
 */
val cobblemonArchive: Configuration by configurations.creating { isTransitive = false }

dependencies {
    minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${providers.gradleProperty("fabric_kotlin_version").get()}")

    // Les joueurs installent Cobblemon à part : on compile contre lui, on ne l'embarque pas.
    modImplementation("maven.modrinth:cobblemon:${providers.gradleProperty("cobblemon_version").get()}")
    cobblemonArchive("maven.modrinth:cobblemon:${providers.gradleProperty("cobblemon_version").get()}")

    // Optionnel : compilation seulement, jamais embarqué ni exigé. À charger par réflexion
    // (ou derrière FabricLoader.isModLoaded) quand le mod est présent.
    modCompileOnly("maven.modrinth:khlinTrc:${providers.gradleProperty("cobblemon_trainers_version").get()}")

    testImplementation(kotlin("test-junit"))
    testImplementation("junit:junit:${providers.gradleProperty("junit_version").get()}")
}

/**
 * Les bibliothèques embarquées dans le jar de Cobblemon (GraalJS, etc.), extraites pour le jeu
 * de développement. Loom met les classes d'un mod sur le classpath de dev, mais pas les jars
 * imbriqués dedans : sans cette tâche, le jeu démarre puis aucun combat ne peut tourner
 * (`NoClassDefFoundError: org/graalvm/polyglot/HostAccess`).
 */
val cobblemonLibsDir = layout.buildDirectory.dir("cobblemon-libs")

val cobblemonLibs = tasks.register<Sync>("cobblemonLibs") {
    description = "Extrait les bibliothèques imbriquées dans le jar de Cobblemon."
    group = "fabric"

    from(cobblemonArchive.elements.map { archives -> archives.map { zipTree(it) } }) {
        include("META-INF/jars/*.jar")
        // Déjà une vraie dépendance ici ; une seconde copie poserait problème.
        exclude("**/fabric-language-kotlin-*.jar")
        eachFile { relativePath = RelativePath(true, name) }
    }
    into(cobblemonLibsDir)
    includeEmptyDirs = false
}

dependencies {
    localRuntime(fileTree(cobblemonLibsDir) {
        include("*.jar")
        builtBy(cobblemonLibs)
    })
}

tasks.processResources {
    val version = version
    inputs.property("version", version)

    filesMatching("fabric.mod.json") {
        expand("version" to version)
    }
}

tasks.test {
    // La configuration de journalisation de Minecraft écrit relativement au dossier courant.
    val runDirectory = layout.buildDirectory.dir("test-run")
    workingDir(runDirectory)
    doFirst { runDirectory.get().asFile.mkdirs() }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

java {
    // Cobblemon exige Java 21 exactement : sans toolchain, runClient hérite du JDK de Gradle.
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
}
