package matheo1712.leaguescreen.block

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Rotation
import kotlin.test.Test
import kotlin.test.assertEquals

class TerminalLinksTest {

    private val terminal = BlockPos(10, 64, -5)
    private val horizontal = listOf(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)

    @Test
    fun `aller-retour monde vers borne dans les quatre orientations`() {
        val wall = BlockPos(3, 70, 12)
        for (facing in horizontal) {
            val local = TerminalLinks.toLocal(terminal, facing, wall)
            assertEquals(wall, TerminalLinks.toWorld(terminal, facing, local), "$facing")
        }
    }

    @Test
    fun `le repere de la borne regarde devant elle`() {
        // Une borne tournée vers le nord : devant est le nord (z -), sa droite est l'est (x +).
        assertEquals(BlockPos(0, 2, 5), TerminalLinks.toLocal(terminal, Direction.NORTH, terminal.offset(0, 2, -5)))
        assertEquals(BlockPos(3, 0, 0), TerminalLinks.toLocal(terminal, Direction.NORTH, terminal.offset(3, 0, 0)))
        // Tournée vers l'est : devant est l'est, sa droite est le sud.
        assertEquals(BlockPos(0, 0, 5), TerminalLinks.toLocal(terminal, Direction.EAST, terminal.offset(5, 0, 0)))
        assertEquals(BlockPos(3, 0, 0), TerminalLinks.toLocal(terminal, Direction.EAST, terminal.offset(0, 0, 3)))
    }

    @Test
    fun `une arene tournee garde ses liens`() {
        // Une structure tournée d'un quart de tour : la borne et le mur tournent ensemble autour de l'origine.
        val wall = BlockPos(4, 70, -12)
        for (facing in horizontal) for (rotation in Rotation.entries) {
            val local = TerminalLinks.toLocal(terminal, facing, wall)
            val movedTerminal = terminal.rotate(rotation)
            val movedWall = wall.rotate(rotation)
            val movedFacing = rotation.rotate(facing)
            assertEquals(movedWall, TerminalLinks.toWorld(movedTerminal, movedFacing, local), "$facing $rotation")
        }
    }

    @Test
    fun `ajouter un ecran`() {
        val a = BlockPos(1, 2, 3)
        assertEquals(TerminalLinks.Added(listOf(a)), TerminalLinks.add(emptyList(), a))
        assertEquals(TerminalLinks.AlreadyLinked, TerminalLinks.add(listOf(a), a))
        val full = (1..TerminalLinks.MAX_WALLS).map { BlockPos(it, 0, 0) }
        assertEquals(TerminalLinks.Full, TerminalLinks.add(full, BlockPos(99, 0, 0)))
    }

    @Test
    fun `huit ecrans au plus`() = assertEquals(8, TerminalLinks.MAX_WALLS)
}
