package matheo1712.leaguescreen.arena

import net.minecraft.core.GlobalPos
import java.util.concurrent.ConcurrentHashMap

/**
 * Les bornes chargées dont l'arène est active. Au début d'un combat, le suivi ne regarde qu'elles,
 * au lieu de chercher des bornes dans le monde.
 *
 * Une borne s'y inscrit en se chargeant ou quand on active son interrupteur, et s'en retire en se
 * déchargeant, en étant cassée ou quand on l'éteint (voir `TerminalBlockEntity`).
 */
object ArenaTerminals {

    private val terminals: MutableSet<GlobalPos> = ConcurrentHashMap.newKeySet()

    fun add(terminal: GlobalPos) {
        terminals += terminal
    }

    fun remove(terminal: GlobalPos) {
        terminals -= terminal
    }

    fun all(): List<GlobalPos> = terminals.toList()

    fun clear() = terminals.clear()
}
