package matheo1712.leaguescreen.theme

import C_FOLDER
import matheo1712.leaguescreen.LeagueScreen
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.ResourceManager

/**
 * Les thèmes que le serveur connaît, lus dans `data/<ns>/leaguescreen/themes/` de tous les
 * datapacks, au démarrage et à chaque `/reload`.
 *
 * Un thème cassé est écarté sans empêcher les autres de se charger, et la Couronne est toujours
 * là : si un pack la remplace par un fichier illisible, c'est celle du jar qui reste.
 */
object ThemeRegistry {
    /**
     * @param themes les thèmes lus, Couronne comprise
     * @param raw leur JSON tel quel, ce que le serveur envoie aux joueurs
     */
    data class LoadResult(
        val themes: Map<ResourceLocation, Theme>,
        val raw: Map<ResourceLocation, String>,
        val errors: List<String>,
        val warnings: List<String>,
    )

    @Volatile
    private var current: LoadResult = load(emptyMap())

    fun get(id: ResourceLocation): Theme? = current.themes[id]

    fun contains(id: ResourceLocation): Boolean = id in current.themes

    /** Le JSON de chaque thème, par identifiant : ce que reçoivent les joueurs. */
    fun all(): Map<ResourceLocation, String> = current.raw

    /**
     * Lit des fichiers de thème. Pur : on lui passe l'emplacement et le texte de chaque fichier.
     *
     * L'identifiant d'un thème est son emplacement sans le dossier des thèmes ni `.json` :
     * `mon_pack:leaguescreen/themes/kanto/indigo.json` donne `mon_pack:kanto/indigo`.
     */
    fun load(files: Map<ResourceLocation, String>): LoadResult =
        loadById(files.mapNotNull { (location, text) -> idOf(location)?.let { it to text } }.toMap())

    /** Comme [load], mais à partir des identifiants des thèmes : ce que le client reçoit du serveur. */
    fun loadById(byId: Map<ResourceLocation, String>): LoadResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        val entries = byId.entries.map { it.key to it.value }.sortedBy { it.first.toString() }

        // La Couronne d'abord : elle sert de repli aux autres.
        var couronne = Couronne.theme
        var couronneJson = Couronne.json
        entries.firstOrNull { it.first == Couronne.id }?.let { (id, text) ->
            ThemeParser.parse(id, text, null)
                .onSuccess { couronne = it; couronneJson = text; warnings += it.warnings.map { w -> "$id : $w" } }
                .onFailure { errors += "$id : ${it.message} (celle du mod est gardée)" }
        }

        val themes = linkedMapOf(Couronne.id to couronne)
        val raw = linkedMapOf(Couronne.id to couronneJson)
        for ((id, text) in entries) {
            if (id == Couronne.id) continue
            ThemeParser.parse(id, text, couronne)
                .onSuccess { theme ->
                    themes[id] = theme
                    raw[id] = text
                    warnings += theme.warnings.map { "$id : $it" }
                }
                .onFailure { errors += "$id : ${it.message}" }
        }
        return LoadResult(themes, raw, errors, warnings)
    }

    private fun idOf(location: ResourceLocation): ResourceLocation? {
        val path = location.path
        if (!path.startsWith("$C_FOLDER/") || !path.endsWith(".json")) return null
        val name = path.removePrefix("$C_FOLDER/").removeSuffix(".json")
        return ResourceLocation.tryBuild(location.namespace, name)
    }

    fun register() {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(object : SimpleSynchronousResourceReloadListener {
            override fun getFabricId(): ResourceLocation = LeagueScreen.id("themes")

            override fun onResourceManagerReload(manager: ResourceManager) {
                val files = manager.listResources(C_FOLDER) { it.path.endsWith(".json") }
                    .mapNotNull { (location, resource) ->
                        runCatching { resource.openAsReader().use { it.readText() } }
                            .onFailure { LeagueScreen.LOGGER.warn("Thème {} illisible : {}", location, it.message) }
                            .getOrNull()?.let { location to it }
                    }.toMap()
                val result = load(files)
                current = result
                result.errors.forEach { LeagueScreen.LOGGER.error("Thème écarté — {}", it) }
                result.warnings.forEach { LeagueScreen.LOGGER.warn("Thème — {}", it) }
                LeagueScreen.LOGGER.info("{} thème(s) d'écran chargé(s)", result.themes.size)
            }
        })
    }
}
