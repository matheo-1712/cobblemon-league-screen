package matheo1712.leaguescreen.client.compat

import matheo1712.cobbletrainers.client.cache.TrainerSkinCache
import net.minecraft.resources.ResourceLocation

/**
 * Le seul fichier côté client qui parle à Cobblemon Trainers : le skin d'un dresseur que le serveur
 * envoie (skin de joueur, image téléchargée…). Chargé par son nom, uniquement si le mod est là.
 *
 * Le premier appel demande le skin au serveur ; on dessine une silhouette en attendant.
 */
@Suppress("unused") // appelé par réflexion depuis LeagueScreenClient
object TrainerSkins {

    @JvmStatic
    fun find(trainerId: String): ResourceLocation? = TrainerSkinCache.get(trainerId)?.texture
}
