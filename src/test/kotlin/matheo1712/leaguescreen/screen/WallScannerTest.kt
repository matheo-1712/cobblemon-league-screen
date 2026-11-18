package matheo1712.leaguescreen.screen

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WallScannerTest {

    /** Un mur est décrit par l'ensemble des positions de ses blocs d'écran. */
    private fun rect(x: IntRange, y: IntRange, z: Int): Set<BlockPos> =
        x.flatMap { px -> y.map { py -> BlockPos(px, py, z) } }.toSet()

    private fun scan(start: BlockPos, facing: Direction, wall: Set<BlockPos>): WallRect? =
        WallScanner.scan(start, facing) { it in wall }

    @Test
    fun `mur 5x3 face au sud`() {
        val wall = rect(x = 0..4, y = 64..66, z = 10)
        val found = scan(BlockPos(2, 65, 10), Direction.SOUTH, wall)
        assertEquals(WallRect(BlockPos(0, 64, 10), 5, 3, Direction.SOUTH), found)
    }

    @Test
    fun `face au nord, la gauche est a l'est`() {
        val wall = rect(x = 0..4, y = 64..66, z = 10)
        val found = scan(BlockPos(0, 64, 10), Direction.NORTH, wall)
        assertEquals(WallRect(BlockPos(4, 64, 10), 5, 3, Direction.NORTH), found)
    }

    @Test
    fun `face a l'est, la largeur court le long de z`() {
        val wall = (0..3).flatMap { z -> (0..1).map { y -> BlockPos(7, y, z) } }.toSet()
        val found = scan(BlockPos(7, 1, 2), Direction.EAST, wall)
        assertEquals(WallRect(BlockPos(7, 0, 3), 4, 2, Direction.EAST), found)
    }

    @Test
    fun `un bloc seul est un mur 1x1`() {
        val pos = BlockPos(3, 70, -5)
        assertEquals(WallRect(pos, 1, 1, Direction.WEST), scan(pos, Direction.WEST, setOf(pos)))
    }

    @Test
    fun `un rectangle troue n'est pas un mur`() {
        val wall = rect(x = 0..4, y = 64..66, z = 10) - BlockPos(2, 65, 10)
        assertNull(scan(BlockPos(0, 64, 10), Direction.SOUTH, wall))
    }

    @Test
    fun `une forme en L n'est pas un mur`() {
        val wall = rect(x = 0..2, y = 0..0, z = 10) + rect(x = 0..0, y = 1..2, z = 10)
        assertNull(scan(BlockPos(0, 0, 10), Direction.SOUTH, wall))
    }

    @Test
    fun `17 de large est refuse`() {
        assertNull(scan(BlockPos(0, 0, 10), Direction.SOUTH, rect(x = 0..16, y = 0..0, z = 10)))
    }

    @Test
    fun `10 de haut est refuse`() {
        assertNull(scan(BlockPos(0, 0, 10), Direction.SOUTH, rect(x = 0..0, y = 0..9, z = 10)))
    }

    @Test
    fun `16x9 est accepte`() {
        val found = scan(BlockPos(8, 4, 10), Direction.SOUTH, rect(x = 0..15, y = 0..8, z = 10))
        assertEquals(WallRect(BlockPos(0, 0, 10), 16, 9, Direction.SOUTH), found)
    }

    @Test
    fun `un bloc qui n'est pas un ecran ne donne rien`() {
        val wall = rect(x = 0..4, y = 64..66, z = 10)
        assertNull(scan(BlockPos(9, 64, 10), Direction.SOUTH, wall))
    }

    @Test
    fun `les blocs devant ou derriere le plan sont ignores`() {
        val wall = rect(x = 0..4, y = 64..66, z = 10) + BlockPos(2, 65, 11) + BlockPos(2, 65, 9)
        val found = scan(BlockPos(2, 65, 10), Direction.SOUTH, wall)
        assertEquals(WallRect(BlockPos(0, 64, 10), 5, 3, Direction.SOUTH), found)
    }

    @Test
    fun `blocks rend width x height positions, toutes contenues`() {
        val wall = WallRect(BlockPos(4, 64, 10), 5, 3, Direction.NORTH)
        val blocks = wall.blocks()
        assertEquals(15, blocks.toSet().size)
        assertEquals(rect(x = 0..4, y = 64..66, z = 10), blocks.toSet())
        assertTrue(blocks.all { wall.contains(it) })
        assertFalse(wall.contains(BlockPos(5, 64, 10)))
        assertFalse(wall.contains(BlockPos(2, 67, 10)))
        assertFalse(wall.contains(BlockPos(2, 65, 11)))
    }

    @Test
    fun `le bloc de controle est le coin bas gauche`() {
        val wall = WallRect(BlockPos(0, 64, 10), 5, 3, Direction.SOUTH)
        assertEquals(BlockPos(0, 64, 10), wall.controller)
    }
}
