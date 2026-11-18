package matheo1712.leaguescreen.block

import MAX_DISTANCE
import net.minecraft.core.GlobalPos

object LinkRules {

    /** Même dimension, et [MAX_DISTANCE] blocs au plus en ligne droite. */
    fun inRange(terminal: GlobalPos, wallBlock: GlobalPos): Boolean =
        terminal.dimension() == wallBlock.dimension() &&
                terminal.pos().distSqr(wallBlock.pos()) <= (MAX_DISTANCE * MAX_DISTANCE).toDouble()
}
