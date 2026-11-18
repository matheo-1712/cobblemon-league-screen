package matheo1712.leaguescreen.battle

import matheo1712.leaguescreen.screen.WallRect
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class SideRulesTest {

    private val clicker = UUID.randomUUID()
    private val opponent = UUID.randomUUID()

    /** Un mur 5 × 3 posé autour de x = 0..4, z = 0, tourné vers [facing]. */
    private fun wall(facing: Direction) = WallRect(BlockPos(0, 64, 0), 5, 3, facing)

    private fun left(facing: Direction, clickerAt: Vec3?, opponentAt: Vec3?): UUID =
        SideRules.leftActor(wall(facing), clicker, clickerAt, opponent, opponentAt)

    @Test
    fun `face au sud, le plus a l'ouest est a gauche`() {
        // Un spectateur au sud regarde vers le nord : l'ouest (x petit) est à sa gauche.
        assertEquals(opponent, left(Direction.SOUTH, Vec3(10.0, 64.0, 8.0), Vec3(-6.0, 64.0, 8.0)))
        assertEquals(clicker, left(Direction.SOUTH, Vec3(-6.0, 64.0, 8.0), Vec3(10.0, 64.0, 8.0)))
    }

    @Test
    fun `face au nord, le plus a l'est est a gauche`() {
        assertEquals(clicker, left(Direction.NORTH, Vec3(10.0, 64.0, -8.0), Vec3(-6.0, 64.0, -8.0)))
        assertEquals(opponent, left(Direction.NORTH, Vec3(-6.0, 64.0, -8.0), Vec3(10.0, 64.0, -8.0)))
    }

    @Test
    fun `face a l'est, le plus au sud est a gauche`() {
        // Un spectateur à l'est regarde vers l'ouest : le sud (z grand) est à sa gauche.
        assertEquals(clicker, left(Direction.EAST, Vec3(8.0, 64.0, 12.0), Vec3(8.0, 64.0, -4.0)))
        assertEquals(opponent, left(Direction.EAST, Vec3(8.0, 64.0, -4.0), Vec3(8.0, 64.0, 12.0)))
    }

    @Test
    fun `face a l'ouest, le plus au nord est a gauche`() {
        // Un spectateur à l'ouest regarde vers l'est : le nord (z petit) est à sa gauche.
        assertEquals(clicker, left(Direction.WEST, Vec3(-8.0, 64.0, -4.0), Vec3(-8.0, 64.0, 12.0)))
        assertEquals(opponent, left(Direction.WEST, Vec3(-8.0, 64.0, 12.0), Vec3(-8.0, 64.0, -4.0)))
    }

    @Test
    fun `alignes face au mur, celui qui clique reste a gauche`() {
        // L'écran est derrière l'un des deux : même x, seule la distance au mur change.
        assertEquals(clicker, left(Direction.SOUTH, Vec3(2.0, 64.0, 4.0), Vec3(2.5, 64.0, 30.0)))
        assertEquals(clicker, left(Direction.SOUTH, Vec3(2.5, 64.0, 30.0), Vec3(2.0, 64.0, 4.0)))
    }

    @Test
    fun `l'ecart minimal est d'un bloc`() {
        assertEquals(clicker, left(Direction.SOUTH, Vec3(3.0, 64.0, 8.0), Vec3(2.1, 64.0, 8.0)))
        assertEquals(opponent, left(Direction.SOUTH, Vec3(3.0, 64.0, 8.0), Vec3(1.9, 64.0, 8.0)))
    }

    @Test
    fun `sans position connue, celui qui clique reste a gauche`() {
        assertEquals(clicker, left(Direction.SOUTH, null, Vec3(-6.0, 64.0, 8.0)))
        assertEquals(clicker, left(Direction.SOUTH, Vec3(10.0, 64.0, 8.0), null))
    }

    @Test
    fun `la hauteur ne compte pas`() {
        assertEquals(opponent, left(Direction.SOUTH, Vec3(10.0, 64.0, 8.0), Vec3(-6.0, 90.0, 8.0)))
    }
}
