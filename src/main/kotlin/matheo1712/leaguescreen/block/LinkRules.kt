package matheo1712.leaguescreen.block

import C_MAX_DISTANCE
import net.minecraft.core.GlobalPos

object LinkRules {

    /** Même dimension, et [C_MAX_DISTANCE] blocs au plus en ligne droite. */
    fun inRange(terminal: GlobalPos, wallBlock: GlobalPos): Boolean =
        terminal.dimension() == wallBlock.dimension() &&
                terminal.pos().distSqr(wallBlock.pos()) <= (C_MAX_DISTANCE * C_MAX_DISTANCE).toDouble()
}
