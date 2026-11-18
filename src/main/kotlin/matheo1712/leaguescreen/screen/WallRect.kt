package matheo1712.leaguescreen.screen

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag

/**
 * Le rectangle d'un mur d'écran.
 *
 * [origin] est le bloc en bas à gauche **vu de face**, c'est-à-dire par quelqu'un qui regarde la
 * dalle. La largeur s'étend vers sa droite ([Direction.getCounterClockWise] de [facing]), la
 * hauteur vers le haut. [facing] est la direction vers laquelle la dalle est tournée, donc vers
 * le spectateur ; elle est toujours horizontale.
 */
data class WallRect(val origin: BlockPos, val width: Int, val height: Int, val facing: Direction) {

    /** Le bloc qui porte l'état du mur et qui le dessine. */
    val controller: BlockPos get() = origin

    private val right: Direction get() = facing.counterClockWise

    fun contains(pos: BlockPos): Boolean {
        val dx = pos.x - origin.x
        val dz = pos.z - origin.z
        val depth = dx * facing.stepX + dz * facing.stepZ
        val along = dx * right.stepX + dz * right.stepZ
        val up = pos.y - origin.y
        return depth == 0 && along in 0 until width && up in 0 until height
    }

    fun blocks(): List<BlockPos> =
        (0 until height).flatMap { up -> (0 until width).map { along -> origin.relative(right, along).above(up) } }

    fun toTag(): CompoundTag {
        val tag = CompoundTag()
        tag.putInt(X, origin.x)
        tag.putInt(Y, origin.y)
        tag.putInt(Z, origin.z)
        tag.putInt(WIDTH, width)
        tag.putInt(HEIGHT, height)
        tag.putString(FACING, facing.getName())
        return tag
    }

    /**
     * Ce que le bloc de contrôle sauvegarde : la taille seule. Son origine est le bloc lui-même et
     * son orientation celle du bloc, si bien qu'un mur copié ou tourné (bloc de structure) reste juste.
     */
    fun toSizeTag(): CompoundTag {
        val tag = CompoundTag()
        tag.putInt(WIDTH, width)
        tag.putInt(HEIGHT, height)
        return tag
    }

    companion object {
        const val MAX_WIDTH = 16
        const val MAX_HEIGHT = 9

        private const val X = "x"
        private const val Y = "y"
        private const val Z = "z"
        private const val WIDTH = "width"
        private const val HEIGHT = "height"
        private const val FACING = "facing"

        /** Relit un mur sauvegardé par [toSizeTag] (ou par l'ancien [toTag]) au bloc [origin], orienté [facing]. */
        fun fromSizeTag(tag: CompoundTag, origin: BlockPos, facing: Direction): WallRect? {
            if (facing.axis.isVertical) return null
            val width = tag.getInt(WIDTH)
            val height = tag.getInt(HEIGHT)
            if (width !in 1..MAX_WIDTH || height !in 1..MAX_HEIGHT) return null
            return WallRect(origin.immutable(), width, height, facing)
        }

        /** Lit un mur ; rend `null` pour un tag vide, une orientation non horizontale ou des dimensions hors limites. */
        fun fromTag(tag: CompoundTag): WallRect? {
            val facing = Direction.byName(tag.getString(FACING)) ?: return null
            if (facing.axis.isVertical) return null
            val width = tag.getInt(WIDTH)
            val height = tag.getInt(HEIGHT)
            if (width !in 1..MAX_WIDTH || height !in 1..MAX_HEIGHT) return null
            return WallRect(BlockPos(tag.getInt(X), tag.getInt(Y), tag.getInt(Z)), width, height, facing)
        }
    }
}
