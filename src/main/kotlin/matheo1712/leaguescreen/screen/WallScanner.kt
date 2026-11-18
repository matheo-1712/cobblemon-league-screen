package matheo1712.leaguescreen.screen

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

/**
 * Retrouve le mur auquel appartient un bloc d'écran.
 *
 * Ne connaît pas le monde : on lui dit seulement, position par position, s'il y a là un bloc
 * d'écran de la bonne orientation. C'est ce qui le rend testable sans lancer le jeu.
 */
object WallScanner {

    private const val MAX_BLOCKS = WallRect.MAX_WIDTH * WallRect.MAX_HEIGHT

    /**
     * Tous les blocs d'écran reliés à [start] dans le plan du mur, que l'ensemble forme un rectangle
     * ou non. L'exploration s'arrête dès que [limit] est dépassée : le résultat compte alors
     * `limit + 1` blocs, ce qui suffit à savoir que l'ensemble est trop grand.
     *
     * @return un ensemble vide si [start] n'est pas un écran ou si [facing] n'est pas horizontale
     */
    fun connected(start: BlockPos, facing: Direction, limit: Int, isScreen: (BlockPos) -> Boolean): Set<BlockPos> {
        if (facing.axis.isVertical || !isScreen(start)) return emptySet()

        val right = facing.counterClockWise
        val steps = listOf(right, right.opposite, Direction.UP, Direction.DOWN)

        // Diffusion dans le plan du mur : jamais vers l'avant ni vers l'arrière.
        val visited = hashSetOf(start)
        val queue = ArrayDeque(listOf(start))
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            for (step in steps) {
                val next = current.relative(step)
                if (next in visited || !isScreen(next)) continue
                visited += next
                if (visited.size > limit) return visited
                queue += next
            }
        }
        return visited
    }

    /**
     * @param start un bloc du mur
     * @param facing l'orientation de ce bloc
     * @param isScreen vrai s'il y a à cette position un bloc d'écran orienté comme [start]
     * @return le rectangle, ou `null` si [start] n'est pas un écran, si l'ensemble ne forme pas un
     * rectangle plein, ou s'il dépasse [WallRect.MAX_WIDTH] × [WallRect.MAX_HEIGHT]
     */
    fun scan(start: BlockPos, facing: Direction, isScreen: (BlockPos) -> Boolean): WallRect? {
        val visited = connected(start, facing, MAX_BLOCKS, isScreen)
        if (visited.isEmpty() || visited.size > MAX_BLOCKS) return null

        val right = facing.counterClockWise

        fun along(pos: BlockPos): Int = (pos.x - start.x) * right.stepX + (pos.z - start.z) * right.stepZ

        val minAlong = visited.minOf(::along)
        val maxAlong = visited.maxOf(::along)
        val minY = visited.minOf { it.y }
        val maxY = visited.maxOf { it.y }
        val width = maxAlong - minAlong + 1
        val height = maxY - minY + 1

        if (width > WallRect.MAX_WIDTH || height > WallRect.MAX_HEIGHT) return null
        // Un rectangle plein a exactement largeur × hauteur blocs : un trou ou un L en a moins.
        if (visited.size != width * height) return null

        val origin = start.relative(right, minAlong).atY(minY)
        return WallRect(origin, width, height, facing)
    }
}
