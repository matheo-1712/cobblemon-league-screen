package matheo1712.leaguescreen.client.arena

import matheo1712.leaguescreen.arena.ArenaZone
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

/**
 * L'affichage de la zone d'une arène, propre à ce joueur : le bouton « Afficher la zone » du menu
 * l'allume pour [DURATION] ticks, et les particules restent après la fermeture du menu, pour qu'on
 * puisse juger la zone en marchant dans l'arène.
 *
 * Pur : le temps (en ticks du client) est passé en paramètre. Le dessin est dans [ZoneParticles].
 */
object ZonePreview {

    /** Deux minutes. */
    const val DURATION = 2400L

    data class Shown(val terminal: BlockPos, val facing: Direction, val zone: ArenaZone, val auto: Boolean, val until: Long)

    @Volatile
    private var shown: Shown? = null

    fun show(terminal: BlockPos, facing: Direction, zone: ArenaZone, auto: Boolean, now: Long) {
        shown = Shown(terminal.immutable(), facing, zone, auto, now + DURATION)
    }

    fun hide() {
        shown = null
    }

    /** Les curseurs du menu ont bougé : la zone affichée les suit, si c'est la même borne. */
    fun update(terminal: BlockPos, zone: ArenaZone, auto: Boolean) {
        val current = shown ?: return
        if (current.terminal == terminal) shown = current.copy(zone = zone, auto = auto)
    }

    /** La zone à dessiner maintenant, ou `null` si rien n'est affiché (ou si les deux minutes sont passées). */
    fun shown(now: Long): Shown? {
        val current = shown ?: return null
        if (now >= current.until) {
            shown = null
            return null
        }
        return current
    }

    fun isShowing(terminal: BlockPos, now: Long): Boolean = shown(now)?.terminal == terminal
}
