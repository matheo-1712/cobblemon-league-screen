package matheo1712.leaguescreen.arena

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.world.phys.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArenaZoneTest {

    /** Une borne posée en (100, 64, -20) : le centre de son bloc est (100.5, 64.5, -19.5). */
    private val terminal = BlockPos(100, 64, -20)

    @Test
    fun `la zone par defaut fait 24 sur 24 sur 8 et est eteinte`() {
        assertEquals(ArenaSettings(false, ArenaZone(24, 24, 8)), ArenaSettings.DEFAULT)
    }

    @Test
    fun `la boite est centree sur la borne`() {
        val zone = ArenaZone(24, 10, 8)
        assertTrue(zone.contains(terminal, Direction.NORTH, Vec3(100.5, 64.0, -19.5)))
        // 12 blocs de chaque côté en Est-Ouest, 5 en Nord-Sud, 4 en hauteur.
        assertTrue(zone.contains(terminal, Direction.NORTH, Vec3(112.5, 64.0, -19.5)))
        assertFalse(zone.contains(terminal, Direction.NORTH, Vec3(112.6, 64.0, -19.5)))
        assertTrue(zone.contains(terminal, Direction.NORTH, Vec3(100.5, 64.0, -14.5)))
        assertFalse(zone.contains(terminal, Direction.NORTH, Vec3(100.5, 64.0, -14.4)))
        assertTrue(zone.contains(terminal, Direction.NORTH, Vec3(100.5, 68.5, -19.5)))
        assertFalse(zone.contains(terminal, Direction.NORTH, Vec3(100.5, 68.6, -19.5)))
        assertFalse(zone.contains(terminal, Direction.NORTH, Vec3(100.5, 60.4, -19.5)))
    }

    @Test
    fun `la largeur va de gauche a droite vu depuis la borne`() {
        // Tournée vers l'est, la largeur court du nord au sud et la profondeur d'ouest en est.
        val zone = ArenaZone(24, 10, 8)
        assertTrue(zone.contains(terminal, Direction.EAST, Vec3(100.5, 64.0, -7.5)))
        assertFalse(zone.contains(terminal, Direction.EAST, Vec3(100.5, 64.0, -7.4)))
        assertTrue(zone.contains(terminal, Direction.EAST, Vec3(105.5, 64.0, -19.5)))
        assertFalse(zone.contains(terminal, Direction.EAST, Vec3(105.6, 64.0, -19.5)))
    }

    @Test
    fun `les tailles sont bornees`() {
        assertEquals(ArenaZone(4, 64, 2), ArenaZone.clamped(1, 500, 0))
        assertEquals(ArenaZone(30, 40, 32), ArenaZone.clamped(30, 40, 99))
    }

    @Test
    fun `les coins de la boite`() {
        val (min, max) = ArenaZone(24, 10, 8).corners(terminal, Direction.NORTH)
        assertEquals(Vec3(88.5, 60.5, -24.5), min)
        assertEquals(Vec3(112.5, 68.5, -14.5), max)
    }

    @Test
    fun `aller-retour NBT des reglages`() {
        val settings = ArenaSettings(true, ArenaZone(30, 18, 6))
        assertEquals(settings, ArenaSettings.fromTag(settings.toTag()))
    }

    @Test
    fun `un tag vide ou abime donne les reglages par defaut bornes`() {
        assertEquals(ArenaSettings.DEFAULT, ArenaSettings.fromTag(CompoundTag()))
        val tag = ArenaSettings(true, ArenaZone(30, 18, 6)).toTag()
        tag.putInt("width", 9999)
        assertEquals(64, ArenaSettings.fromTag(tag).zone.width)
    }

    @Test
    fun `le composant d'objet garde les reglages`() {
        val settings = ArenaSettings(true, ArenaZone(30, 18, 6))
        val encoded = ArenaSettings.CODEC.encodeStart(NbtOps.INSTANCE, settings).getOrThrow()
        assertEquals(settings, ArenaSettings.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow())
    }
}
