package matheo1712.leaguescreen.client.arena

import matheo1712.leaguescreen.arena.ArenaZone
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.DustParticleOptions
import org.joml.Vector3f

/** Dessine les arêtes d'une zone d'arène en particules : une par bloc d'arête. */
object ZoneParticles {

    private val GOLD = Vector3f(1f, 0.85f, 0.29f)
    private val GREY = Vector3f(0.6f, 0.65f, 0.75f)

    /** Dorées si l'écran se lance tout seul, grises sinon. Visibles de loin (`force`). */
    fun draw(level: ClientLevel, terminal: BlockPos, facing: Direction, zone: ArenaZone, auto: Boolean) {
        val (min, max) = zone.corners(terminal, facing)
        val particle = DustParticleOptions(if (auto) GOLD else GREY, 1.2f)

        fun edge(steps: Int, point: (Double) -> Triple<Double, Double, Double>) {
            for (i in 0..steps) {
                val (x, y, z) = point(i.toDouble() / steps)
                level.addAlwaysVisibleParticle(particle, true, x, y, z, 0.0, 0.0, 0.0)
            }
        }

        val dx = (max.x - min.x).toInt()
        val dy = (max.y - min.y).toInt()
        val dz = (max.z - min.z).toInt()
        for (y in listOf(min.y, max.y)) for (z in listOf(min.z, max.z)) edge(dx) { t -> Triple(min.x + t * (max.x - min.x), y, z) }
        for (x in listOf(min.x, max.x)) for (z in listOf(min.z, max.z)) edge(dy) { t -> Triple(x, min.y + t * (max.y - min.y), z) }
        for (x in listOf(min.x, max.x)) for (y in listOf(min.y, max.y)) edge(dz) { t -> Triple(x, y, min.z + t * (max.z - min.z)) }
    }
}
