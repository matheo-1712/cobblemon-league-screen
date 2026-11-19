package matheo1712.leaguescreen.arena

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutoStartTest {

    private val terminal = BlockPos(0, 64, 0)
    private val on = ArenaSettings(true, ArenaZone(24, 24, 8))
    private val inside = listOf<Vec3?>(Vec3(-8.0, 64.0, 0.5), Vec3(9.0, 64.0, 0.5))

    /** Un combat qui coche toutes les cases. */
    private fun decide(
        settings: ArenaSettings = on,
        positions: List<Vec3?> = inside,
        actorCount: Int = 2,
        hasWildActor: Boolean = false,
        wallValid: Boolean = true,
        wallBusy: Boolean = false,
    ) = AutoStart.shouldFollow(settings, terminal, Direction.NORTH, positions, actorCount, hasWildActor, wallValid, wallBusy)

    @Test
    fun `cas nominal`() = assertTrue(decide())

    @Test
    fun `interrupteur eteint`() = assertFalse(decide(settings = on.copy(auto = false)))

    @Test
    fun `un dresseur hors de la zone`() = assertFalse(decide(positions = listOf(Vec3(-8.0, 64.0, 0.5), Vec3(40.0, 64.0, 0.5))))

    @Test
    fun `une position inconnue`() = assertFalse(decide(positions = listOf(Vec3(-8.0, 64.0, 0.5), null)))

    @Test
    fun `un combat sauvage`() = assertFalse(decide(hasWildActor = true))

    @Test
    fun `plus de deux dresseurs`() = assertFalse(decide(actorCount = 4, positions = inside + inside))

    @Test
    fun `mur absent ou invalide`() = assertFalse(decide(wallValid = false))

    @Test
    fun `mur deja occupe`() = assertFalse(decide(wallBusy = true))
}
