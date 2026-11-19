package matheo1712.leaguescreen.arena

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.phys.Vec3

/**
 * La zone d'une arène : une boîte centrée sur la borne, qui suit son orientation. Vu depuis la
 * borne, [width] court de gauche à droite, [length] d'avant en arrière, [height] de bas en haut, en
 * blocs. Ainsi une arène copiée et tournée (bloc de structure) garde sa zone.
 */
data class ArenaZone(val width: Int, val length: Int, val height: Int) {

    /** Le coin bas (x, z minimaux) et le coin haut (x, z maximaux) de la boîte autour de [terminal]. */
    fun corners(terminal: BlockPos, facing: Direction): Pair<Vec3, Vec3> {
        val center = Vec3.atCenterOf(terminal)
        // La profondeur suit l'axe vers lequel la borne regarde, la largeur l'autre axe horizontal.
        val alongX = facing.axis == Direction.Axis.X
        val half = Vec3((if (alongX) length else width) / 2.0, height / 2.0, (if (alongX) width else length) / 2.0)
        return center.subtract(half) to center.add(half)
    }

    /** [point] (les pieds d'un dresseur) est-il dans la boîte autour de [terminal] ? Les bords comptent. */
    fun contains(terminal: BlockPos, facing: Direction, point: Vec3): Boolean {
        val (min, max) = corners(terminal, facing)
        return point.x in min.x..max.x && point.y in min.y..max.y && point.z in min.z..max.z
    }

    companion object {
        const val MIN_SIDE = 4
        const val MAX_SIDE = 64
        const val MIN_HEIGHT = 2
        const val MAX_HEIGHT = 32

        fun clamped(width: Int, length: Int, height: Int) = ArenaZone(
            width.coerceIn(MIN_SIDE, MAX_SIDE),
            length.coerceIn(MIN_SIDE, MAX_SIDE),
            height.coerceIn(MIN_HEIGHT, MAX_HEIGHT),
        )
    }
}

/** Les réglages d'arène d'une borne : l'interrupteur « lancer l'écran tout seul » et la zone. */
data class ArenaSettings(val auto: Boolean, val zone: ArenaZone) {

    fun toTag(): CompoundTag {
        val tag = CompoundTag()
        tag.putBoolean(AUTO, auto)
        tag.putInt(WIDTH, zone.width)
        tag.putInt(LENGTH, zone.length)
        tag.putInt(HEIGHT, zone.height)
        return tag
    }

    companion object {
        val DEFAULT = ArenaSettings(false, ArenaZone(24, 24, 8))

        /** Pour le composant d'objet : la borne cassée emporte ses réglages. */
        val CODEC: Codec<ArenaSettings> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.BOOL.fieldOf(AUTO).forGetter(ArenaSettings::auto),
                Codec.INT.fieldOf(WIDTH).forGetter { it.zone.width },
                Codec.INT.fieldOf(LENGTH).forGetter { it.zone.length },
                Codec.INT.fieldOf(HEIGHT).forGetter { it.zone.height },
            ).apply(instance) { auto, width, length, height -> ArenaSettings(auto, ArenaZone.clamped(width, length, height)) }
        }

        /** Pour le réseau ; les tailles sont bornées dès la lecture : un client ne peut pas imposer une zone démesurée. */
        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, ArenaSettings> = StreamCodec.of(
            { buffer, settings ->
                buffer.writeBoolean(settings.auto)
                buffer.writeVarInt(settings.zone.width)
                buffer.writeVarInt(settings.zone.length)
                buffer.writeVarInt(settings.zone.height)
            },
            { buffer -> ArenaSettings(buffer.readBoolean(), ArenaZone.clamped(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt())) },
        )

        private const val AUTO = "auto"
        private const val WIDTH = "width"
        private const val LENGTH = "length"
        private const val HEIGHT = "height"

        /** Lit des réglages ; une valeur absente prend le défaut, une valeur hors limites est ramenée dedans. */
        fun fromTag(tag: CompoundTag): ArenaSettings {
            val zone = DEFAULT.zone
            return ArenaSettings(
                tag.getBoolean(AUTO),
                ArenaZone.clamped(
                    if (tag.contains(WIDTH)) tag.getInt(WIDTH) else zone.width,
                    if (tag.contains(LENGTH)) tag.getInt(LENGTH) else zone.length,
                    if (tag.contains(HEIGHT)) tag.getInt(HEIGHT) else zone.height,
                ),
            )
        }
    }
}
