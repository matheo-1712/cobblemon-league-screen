import me.modmuss50.mpp.ReleaseType
import net.fabricmc.loom.task.RemapJarTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("net.fabricmc.fabric-loom-remap")
    id("org.jetbrains.kotlin.jvm")
    id("me.modmuss50.mod-publish-plugin")
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

    // Jeu de développement seulement (runClient) : jamais embarqué ni publié comme dépendance.
    // Cobblemon Trainers, pour voir les skins des dresseurs ; Sodium et Iris, pour tester le mur
    // avec un pack de shaders (à déposer dans run/shaderpacks).
    modLocalRuntime("maven.modrinth:khlinTrc:${providers.gradleProperty("cobblemon_trainers_version").get()}")
    modLocalRuntime("maven.modrinth:sodium:${providers.gradleProperty("sodium_version").get()}")
    modLocalRuntime("maven.modrinth:iris:${providers.gradleProperty("iris_version").get()}")

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

/**
 * Même chose pour Iris : sans les bibliothèques qu'il embarque, le jeu de dev plante dès qu'on
 * ouvre la liste des packs de shaders (`ClassNotFoundException: org.anarres.cpp...`). Ses autres
 * jars imbriqués sont des modules de Fabric API, déjà là.
 */
val irisArchive: Configuration by configurations.creating { isTransitive = false }

dependencies {
    irisArchive("maven.modrinth:iris:${providers.gradleProperty("iris_version").get()}")
}

val irisLibsDir = layout.buildDirectory.dir("iris-libs")

val irisLibs = tasks.register<Sync>("irisLibs") {
    description = "Extrait les bibliothèques imbriquées dans le jar d'Iris."
    group = "fabric"

    from(irisArchive.elements.map { archives -> archives.map { zipTree(it) } }) {
        include("META-INF/jars/antlr4-runtime-*.jar", "META-INF/jars/glsl-transformer-*.jar", "META-INF/jars/jcpp-*.jar")
        eachFile { relativePath = RelativePath(true, name) }
    }
    into(irisLibsDir)
    includeEmptyDirs = false
}

dependencies {
    localRuntime(fileTree(irisLibsDir) {
        include("*.jar")
        builtBy(irisLibs)
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

/**
 * Type de release, réglable en ligne de commande pour que le workflow de release pilote le build
 * sans modifier de fichier suivi : `./gradlew publishMods -Prelease_type=beta`.
 */
val releaseType = when (providers.gradleProperty("release_type").getOrElse("stable").lowercase()) {
    "alpha" -> ReleaseType.ALPHA
    "beta" -> ReleaseType.BETA
    else -> ReleaseType.STABLE
}

// Pris ici exprès : dans `publishMods`, `version` désigne la propriété de l'extension, et
// l'interpoler donnerait sa description Gradle plutôt que le numéro.
val modVersion = project.version.toString()

publishMods {
    // `remapJar`, pas `jar` : ce dernier garde les noms de développement et planterait hors du
    // jeu de dev. En CI, on publie le jar déjà construit et vérifié par le job `build`.
    file = providers.gradleProperty("release_file")
        .map { layout.projectDirectory.file(it) }
        .orElse(tasks.named<RemapJarTask>("remapJar").flatMap { it.archiveFile })
    displayName = modVersion
    version = modVersion
    type = releaseType
    modLoaders.add("fabric")

    // Le workflow passe ici le texte de la release GitHub. En local, un simple lien.
    changelog = providers.environmentVariable("CHANGELOG")
        .orElse("See https://github.com/matheo-1712/cobblemon-league-screen/releases/tag/v$modVersion")

    // Sans jeton, tout se fait à blanc : les fichiers sont écrits dans build/mod-publish au lieu
    // d'être envoyés. `./gradlew publishMods` reste donc sans risque en local.
    dryRun = providers.environmentVariable("MODRINTH_TOKEN").orNull == null

    modrinth {
        accessToken = providers.environmentVariable("MODRINTH_TOKEN").orElse("dry-run")
        projectId = providers.gradleProperty("modrinth_id")
        minecraftVersions.add(providers.gradleProperty("minecraft_version").get())

        // Le mod est requis des deux côtés : le serveur suit les combats, le client dessine le mur.
        environment = CLIENT_AND_SERVER

        // Les versions exactes contre lesquelles le mod est construit, lues dans gradle.properties.
        requires {
            slug = "cobblemon"
            version = providers.gradleProperty("cobblemon_version")
        }
        requires {
            slug = "fabric-api"
            version = providers.gradleProperty("fabric_api_version")
        }
        requires {
            slug = "fabric-language-kotlin"
            version = providers.gradleProperty("fabric_kotlin_version")
        }
    }
}
