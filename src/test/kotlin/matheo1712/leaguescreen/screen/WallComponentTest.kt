package matheo1712.leaguescreen.screen

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** [WallScanner.connected] : l'ensemble des blocs d'écran qui se touchent dans un même plan, rectangle ou non. */
class WallComponentTest {

    private fun rect(x: IntRange, y: IntRange, z: Int): Set<BlockPos> =
        x.flatMap { px -> y.map { py -> BlockPos(px, py, z) } }.toSet()

    @Test
    fun `une forme en L est rendue en entier`() {
        val shape = rect(x = 0..2, y = 0..0, z = 10) + rect(x = 0..0, y = 1..2, z = 10)
        assertEquals(shape, WallScanner.connected(BlockPos(2, 0, 10), Direction.SOUTH, 1024) { it in shape })
    }

    @Test
    fun `deux murs separes ne sont pas melanges`() {
        val left = rect(x = 0..1, y = 0..1, z = 10)
        val right = rect(x = 3..4, y = 0..1, z = 10)
        assertEquals(left, WallScanner.connected(BlockPos(0, 0, 10), Direction.SOUTH, 1024) { it in left + right })
    }

    @Test
    fun `un bloc qui n'est pas un ecran donne un ensemble vide`() {
        assertTrue(WallScanner.connected(BlockPos(0, 0, 0), Direction.SOUTH, 1024) { false }.isEmpty())
    }

    @Test
    fun `l'exploration s'arrete a la limite`() {
        val huge = rect(x = 0..99, y = 0..99, z = 10)
        val found = WallScanner.connected(BlockPos(50, 50, 10), Direction.SOUTH, 200) { it in huge }
        assertTrue(found.size in 200..201, "taille ${found.size}")
    }

    @Test
    fun `les blocs hors du plan sont ignores`() {
        val wall = rect(x = 0..1, y = 0..1, z = 10)
        val found = WallScanner.connected(BlockPos(0, 0, 10), Direction.SOUTH, 1024) { it in wall + BlockPos(0, 0, 11) }
        assertEquals(wall, found)
    }
}
