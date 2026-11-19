package matheo1712.leaguescreen.compat

import com.cobblemon.mod.common.entity.npc.NPCEntity
import matheo1712.cobbletrainers.trainers.TrainerRegistry
import matheo1712.leaguescreen.board.DisplayName

/**
 * Le seul fichier côté serveur qui parle à Cobblemon Trainers. Chargé par son nom depuis
 * [TrainerLookups], uniquement si ce mod est installé.
 *
 * Un dresseur du mod est un PNJ de Cobblemon marqué par un aspect `trainer_id:` ; c'est par là
 * que Cobblemon Trainers lui-même le retrouve.
 */
class CobblemonTrainersLookup : TrainerLookup {

    override fun describe(npc: NPCEntity): TrainerInfo? {
        val id = TrainerRegistry.idFromAspects(npc.aspects) ?: return null
        val definition = TrainerRegistry.findByAspects(npc.aspects) ?: return null
        // Le nom d'un dresseur est une clé de traduction (ou un texte, que la traduction rend tel quel).
        return TrainerInfo(DisplayName(definition.name, true), id.toString(), definition.skin.textureId()?.toString())
    }
}
