package matheo1712.leaguescreen.compat

import com.cobblemon.mod.common.entity.npc.NPCEntity
import matheo1712.leaguescreen.LeagueScreen
import matheo1712.leaguescreen.board.DisplayName
import net.fabricmc.loader.api.FabricLoader

/**
 * Ce qu'on sait d'un dresseur de Cobblemon Trainers : son nom, son identifiant, et le chemin de sa
 * texture de skin quand il en a une (un skin « texture » vit dans les ressources du pack, que le
 * client possède aussi).
 */
data class TrainerInfo(val name: DisplayName, val trainerId: String, val skinTexture: String?)

/** Reconnaît un PNJ dresseur d'un autre mod. Le reste du mod ne parle qu'à cette interface. */
fun interface TrainerLookup {
    /** @return `null` si ce PNJ n'est pas un dresseur connu */
    fun describe(npc: NPCEntity): TrainerInfo?
}

object TrainerLookups {

    private const val COBBLEMON_TRAINERS = "cobblemon-trainers"
    private const val IMPLEMENTATION = "matheo1712.leaguescreen.compat.CobblemonTrainersLookup"

    /**
     * Le lien avec Cobblemon Trainers s'il est installé, `null` sinon : les PNJ gardent alors leur
     * nom d'entité et une silhouette.
     *
     * La classe est chargée **par son nom**, et seulement si le mod est là : aucune autre classe
     * ne mentionne Cobblemon Trainers, donc rien ne casse quand il manque.
     */
    val current: TrainerLookup? by lazy {
        if (!FabricLoader.getInstance().isModLoaded(COBBLEMON_TRAINERS)) return@lazy null
        try {
            Class.forName(IMPLEMENTATION).getDeclaredConstructor().newInstance() as TrainerLookup
        } catch (e: Throwable) {
            LeagueScreen.LOGGER.warn("Lien avec Cobblemon Trainers impossible, les PNJ seront anonymes", e)
            null
        }
    }
}
