package matheo1712.leaguescreen.battle

import C_MIN_GAP
import matheo1712.leaguescreen.screen.WallRect
import net.minecraft.world.phys.Vec3
import java.util.UUID

/**
 * Qui s'affiche à gauche : le dresseur qui se tient à gauche dans l'arène, vu des gradins face au
 * mur. L'écran reflète ainsi la place réelle des deux combattants.
 *
 * Les positions sont projetées sur l'axe « droite du spectateur » du mur
 * ([WallRect.facing] tourné d'un quart de tour, comme dans [WallRect]). La hauteur et la distance
 * au mur ne comptent pas.
 */
object SideRules {
    /** La position le long du mur, de la gauche vers la droite du spectateur. */
    fun along(wall: WallRect, position: Vec3): Double {
        val right = wall.facing.counterClockWise
        return (position.x - wall.origin.x) * right.stepX + (position.z - wall.origin.z) * right.stepZ
    }

    /**
     * Le dresseur à afficher à gauche. Si une position manque, ou si les deux dresseurs sont alignés
     * face au mur (l'écran derrière l'un d'eux), celui qui a cliqué reste à gauche.
     */
    fun leftActor(wall: WallRect, clicker: UUID, clickerAt: Vec3?, opponent: UUID, opponentAt: Vec3?): UUID {
        if (clickerAt == null || opponentAt == null) return clicker
        val gap = along(wall, opponentAt) - along(wall, clickerAt)
        return when {
            kotlin.math.abs(gap) < C_MIN_GAP -> clicker
            gap > 0 -> clicker
            else -> opponent
        }
    }
}
