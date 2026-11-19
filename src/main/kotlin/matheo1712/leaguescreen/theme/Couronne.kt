package matheo1712.leaguescreen.theme

import ID
import net.minecraft.resources.ResourceLocation

/**
 * Le thème livré avec le mod, et celui de tout mur qui n'en a pas choisi d'autre.
 *
 * Il est lu dans le jar du mod lui-même, pas dans les datapacks : il sert de repli quand un thème
 * manque, et fournit `colors`, `pokemon` et `slots` aux thèmes qui ne les redéfinissent pas.
 */
object Couronne {

    val id: ResourceLocation = ResourceLocation.parse(ID)

    private const val PATH = "/data/cobblemon-league-screen/leaguescreen/themes/couronne.json"

    /** Le JSON livré, tel quel. */
    val json: String by lazy {
        val stream = Couronne::class.java.getResourceAsStream(PATH) ?: error("Thème Couronne absent du jar ($PATH)")
        stream.reader(Charsets.UTF_8).use { it.readText() }
    }

    /** Lu une seule fois. Un échec ici est un défaut du mod, pas d'un pack : on le laisse remonter. */
    val theme: Theme by lazy { ThemeParser.parse(id, json, null).getOrThrow() }
}
