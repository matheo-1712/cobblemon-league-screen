package matheo1712.leaguescreen.client.arena

import matheo1712.leaguescreen.arena.ArenaZone
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ZonePreviewTest {

    private val terminal = BlockPos(10, 64, -3)
    private val zone = ArenaZone(24, 24, 8)

    @AfterTest
    fun reset() = ZonePreview.hide()

    @Test
    fun `eteint par defaut`() {
        assertNull(ZonePreview.shown(now = 0))
    }

    @Test
    fun `la zone reste affichee deux minutes`() {
        ZonePreview.show(terminal, Direction.EAST, zone, auto = true, now = 100)
        assertEquals(terminal, ZonePreview.shown(now = 100)?.terminal)
        assertEquals(terminal, ZonePreview.shown(now = 100 + ZonePreview.DURATION - 1)?.terminal)
        assertNull(ZonePreview.shown(now = 100 + ZonePreview.DURATION))
    }

    @Test
    fun `la zone affichee garde l'orientation de la borne`() {
        ZonePreview.show(terminal, Direction.EAST, zone, auto = true, now = 0)
        assertEquals(Direction.EAST, ZonePreview.shown(now = 1)?.facing)
    }

    @Test
    fun `masquer arrete tout de suite`() {
        ZonePreview.show(terminal, Direction.EAST, zone, auto = true, now = 0)
        ZonePreview.hide()
        assertNull(ZonePreview.shown(now = 1))
    }

    @Test
    fun `la zone suit les curseurs de la meme borne`() {
        ZonePreview.show(terminal, Direction.EAST, zone, auto = false, now = 0)
        ZonePreview.update(terminal, ArenaZone(40, 12, 6), auto = true)
        val shown = ZonePreview.shown(now = 5)
        assertEquals(ArenaZone(40, 12, 6), shown?.zone)
        assertTrue(shown!!.auto)
    }

    @Test
    fun `les curseurs d'une autre borne ne changent rien`() {
        ZonePreview.show(terminal, Direction.EAST, zone, auto = false, now = 0)
        ZonePreview.update(BlockPos(0, 0, 0), ArenaZone(40, 12, 6), auto = true)
        assertEquals(zone, ZonePreview.shown(now = 5)?.zone)
    }

    @Test
    fun `isShowing dit si cette borne est affichee`() {
        assertFalse(ZonePreview.isShowing(terminal, now = 0))
        ZonePreview.show(terminal, Direction.EAST, zone, auto = false, now = 0)
        assertTrue(ZonePreview.isShowing(terminal, now = 1))
        assertFalse(ZonePreview.isShowing(BlockPos(0, 0, 0), now = 1))
    }
}
