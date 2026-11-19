package matheo1712.leaguescreen.arena

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3

/** Quand une borne lance-t-elle son écran toute seule, au début d'un combat ? */
object AutoStart {

    /**
     * @param positions où se tenait chaque dresseur au début du combat (`null` si inconnu)
     * @return vrai si l'interrupteur est actif, si **tous** les dresseurs sont dans la zone, et si le
     *   combat passe les mêmes règles qu'un clic : pas sauvage, deux dresseurs, mur valide et libre
     */
    fun shouldFollow(
        settings: ArenaSettings,
        terminal: BlockPos,
        facing: Direction,
        positions: List<Vec3?>,
        actorCount: Int,
        hasWildActor: Boolean,
        wallValid: Boolean,
        wallBusy: Boolean,
    ): Boolean = settings.auto &&
        !hasWildActor &&
        actorCount == 2 &&
        positions.size == 2 &&
        positions.all { it != null && settings.zone.contains(terminal, facing, it) } &&
        wallValid &&
        !wallBusy
}
