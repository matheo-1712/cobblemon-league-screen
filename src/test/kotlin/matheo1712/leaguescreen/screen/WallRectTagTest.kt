package matheo1712.leaguescreen.screen

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WallRectTagTest {

    @Test
    fun `aller-retour NBT`() {
        for (facing in listOf(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
            val wall = WallRect(BlockPos(-12, 70, 345), 16, 9, facing)
            assertEquals(wall, WallRect.fromTag(wall.toTag()))
        }
    }

    @Test
    fun `un tag vide ne donne pas de mur`() = assertNull(WallRect.fromTag(CompoundTag()))

    @Test
    fun `une orientation verticale ou inconnue ne donne pas de mur`() {
        val tag = WallRect(BlockPos(0, 0, 0), 5, 3, Direction.SOUTH).toTag()
        tag.putString("facing", "up")
        assertNull(WallRect.fromTag(tag))
        tag.putString("facing", "nulle_part")
        assertNull(WallRect.fromTag(tag))
    }

    @Test
    fun `des dimensions hors limites ne donnent pas de mur`() {
        val tag = WallRect(BlockPos(0, 0, 0), 5, 3, Direction.SOUTH).toTag()
        tag.putInt("width", 17)
        assertNull(WallRect.fromTag(tag))
        tag.putInt("width", 0)
        assertNull(WallRect.fromTag(tag))
        tag.putInt("width", 5)
        tag.putInt("height", 10)
        assertNull(WallRect.fromTag(tag))
    }

    @Test
    fun `le bloc de controle ne sauvegarde que la taille`() {
        val wall = WallRect(BlockPos(4, 70, 9), 5, 3, Direction.EAST)
        val tag = wall.toSizeTag()
        assertEquals(setOf("width", "height"), tag.allKeys)
        // Recopié ailleurs et tourné : l'origine et l'orientation viennent du bloc.
        assertEquals(WallRect(BlockPos(-20, 50, 3), 5, 3, Direction.SOUTH), WallRect.fromSizeTag(tag, BlockPos(-20, 50, 3), Direction.SOUTH))
    }

    @Test
    fun `l'ancien format se relit par sa taille`() {
        val old = WallRect(BlockPos(4, 70, 9), 5, 3, Direction.EAST).toTag()
        assertEquals(WallRect(BlockPos(4, 70, 9), 5, 3, Direction.EAST), WallRect.fromSizeTag(old, BlockPos(4, 70, 9), Direction.EAST))
    }

    @Test
    fun `une taille hors limites ne donne pas de mur`() {
        val tag = WallRect(BlockPos(0, 0, 0), 5, 3, Direction.SOUTH).toSizeTag()
        tag.putInt("width", 40)
        assertNull(WallRect.fromSizeTag(tag, BlockPos(0, 0, 0), Direction.SOUTH))
    }
}
