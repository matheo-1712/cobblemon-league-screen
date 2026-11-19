package matheo1712.leaguescreen.block

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

/**
 * Les écrans d'une borne, retenus **dans le repère de la borne** : à droite, en haut, devant elle.
 *
 * Une position absolue casserait dès qu'on déplace l'arène (bloc de structure, copier-coller,
 * génération). Une position relative à la borne et à son orientation suit l'arène, même tournée :
 * la structure tourne la borne, et avec elle le repère dans lequel ses écrans sont lus.
 */
object TerminalLinks {

    const val MAX_WALLS = 8

    /** [wall] dans le repère d'une borne en [terminal] tournée vers [facing] : (droite, haut, devant). */
    fun toLocal(terminal: BlockPos, facing: Direction, wall: BlockPos): BlockPos {
        val right = facing.clockWise
        val dx = wall.x - terminal.x
        val dz = wall.z - terminal.z
        return BlockPos(
            dx * right.stepX + dz * right.stepZ,
            wall.y - terminal.y,
            dx * facing.stepX + dz * facing.stepZ,
        )
    }

    /** L'inverse de [toLocal]. */
    fun toWorld(terminal: BlockPos, facing: Direction, local: BlockPos): BlockPos {
        val right = facing.clockWise
        return BlockPos(
            terminal.x + local.x * right.stepX + local.z * facing.stepX,
            terminal.y + local.y,
            terminal.z + local.x * right.stepZ + local.z * facing.stepZ,
        )
    }

    sealed interface AddResult
    data class Added(val walls: List<BlockPos>) : AddResult
    data object AlreadyLinked : AddResult
    data object Full : AddResult

    /** Ajoute un écran (désigné par le bloc de contrôle de son mur) à une liste. */
    fun add(walls: List<BlockPos>, wall: BlockPos): AddResult = when {
        wall in walls -> AlreadyLinked
        walls.size >= MAX_WALLS -> Full
        else -> Added(walls + wall.immutable())
    }
}
