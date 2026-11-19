package matheo1712.leaguescreen.client.theme

import matheo1712.leaguescreen.LeagueScreen
import matheo1712.leaguescreen.theme.Couronne
import matheo1712.leaguescreen.theme.Theme
import matheo1712.leaguescreen.theme.ThemeRegistry
import net.minecraft.resources.ResourceLocation

/**
 * Les thèmes que le serveur a envoyés à ce client, lus avec le même parseur que le serveur.
 *
 * Avant toute réception, ou pour un thème que le serveur ne connaît plus, c'est la Couronne :
 * un mur se dessine toujours. Le choix du mur, lui, n'est pas touché, donc le thème revient si
 * le pack revient.
 */
object ClientThemes {

    @Volatile
    private var themes: Map<ResourceLocation, Theme> = mapOf(Couronne.id to Couronne.theme)

    fun accept(raw: Map<String, String>) {
        val byId = raw.mapNotNull { (id, json) ->
            val location = ResourceLocation.tryParse(id)
            if (location == null) LeagueScreen.LOGGER.warn("Thème reçu avec un identifiant illisible : {}", id)
            location?.let { it to json }
        }.toMap()
        val result = ThemeRegistry.loadById(byId)
        result.errors.forEach { LeagueScreen.LOGGER.warn("Thème reçu écarté — {}", it) }
        themes = result.themes
    }

    /** Le thème à dessiner pour un mur : le sien, ou la Couronne s'il est inconnu ici. */
    fun forWall(id: ResourceLocation): Theme = themes[id] ?: themes[Couronne.id] ?: Couronne.theme

    /** La Couronne d'abord, puis les autres par identifiant. */
    fun list(): List<Theme> = themes.values.sortedWith(compareBy<Theme> { it.id != Couronne.id }.thenBy { it.id.toString() })

    /** En quittant un serveur : on revient à la seule Couronne. */
    fun clear() {
        themes = mapOf(Couronne.id to Couronne.theme)
    }
}
