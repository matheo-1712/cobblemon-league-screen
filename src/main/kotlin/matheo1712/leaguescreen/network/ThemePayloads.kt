package matheo1712.leaguescreen.network

import matheo1712.leaguescreen.LeagueScreen
import matheo1712.leaguescreen.arena.ArenaSettings
import matheo1712.leaguescreen.theme.Couronne
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
/**
 * Serveur → client : tous les thèmes, en JSON tel que lu dans les datapacks. Le client les lit
 * lui-même avec le même parseur que le serveur.
 */
data class ThemesPayload(val themes: Map<String, String>) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<ThemesPayload> = TYPE

    companion object {
        /** Taille maximale du JSON d'un thème dans un paquet. */
        private const val MAX_JSON = 1_048_576

        val TYPE = CustomPacketPayload.Type<ThemesPayload>(LeagueScreen.id("themes"))

        val CODEC: StreamCodec<FriendlyByteBuf, ThemesPayload> = StreamCodec.of(
            { buffer, payload ->
                buffer.writeVarInt(payload.themes.size)
                payload.themes.forEach { (id, json) ->
                    buffer.writeUtf(id)
                    buffer.writeUtf(json, MAX_JSON)
                }
            },
            { buffer ->
                val count = buffer.readVarInt()
                ThemesPayload(LinkedHashMap<String, String>().apply { repeat(count) { put(buffer.readUtf(), buffer.readUtf(MAX_JSON)) } })
            },
        )

        /**
         * Garde les thèmes tant que leur total reste sous [maxBytes] (un paquet est limité à 1 Mio),
         * la Couronne d'abord.
         *
         * @return les thèmes gardés, et le nombre de thèmes omis
         */
        fun capped(themes: Map<String, String>, maxBytes: Int): Pair<Map<String, String>, Int> {
            val ordered = themes.entries.sortedBy { if (it.key == Couronne.ID) 0 else 1 }
            val kept = LinkedHashMap<String, String>()
            var total = 0
            var omitted = 0
            for ((id, json) in ordered) {
                val size = id.length + json.toByteArray(Charsets.UTF_8).size
                if (total + size > maxBytes && id != Couronne.ID) {
                    omitted++
                    continue
                }
                kept[id] = json
                total += size
            }
            return kept to omitted
        }
    }
}

/** L'état d'un écran relié à une borne, tel que le menu le montre. */
enum class WallStatus { READY, MISSING, TOO_FAR, UNLOADED }

/** Un écran relié : la position retenue par la borne, la taille du mur s'il existe, son état. */
data class WallEntry(val pos: BlockPos, val width: Int, val height: Int, val status: WallStatus)

/**
 * Serveur → client : ouvre le menu de la borne, avec tout ce qu'elle retient — son thème, son
 * arène, son orientation (la zone en dépend) et ses écrans.
 */
data class OpenThemeMenuPayload(
    val terminal: BlockPos,
    val current: String,
    val arena: ArenaSettings,
    val facing: Direction,
    val walls: List<WallEntry>,
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<OpenThemeMenuPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<OpenThemeMenuPayload>(LeagueScreen.id("open_theme_menu"))

        val CODEC: StreamCodec<FriendlyByteBuf, OpenThemeMenuPayload> = StreamCodec.of(
            { buffer, payload ->
                buffer.writeBlockPos(payload.terminal)
                buffer.writeUtf(payload.current)
                ArenaSettings.STREAM_CODEC.encode(buffer, payload.arena)
                buffer.writeEnum(payload.facing)
                buffer.writeVarInt(payload.walls.size)
                payload.walls.forEach { wall ->
                    buffer.writeBlockPos(wall.pos)
                    buffer.writeVarInt(wall.width)
                    buffer.writeVarInt(wall.height)
                    buffer.writeEnum(wall.status)
                }
            },
            { buffer ->
                val terminal = buffer.readBlockPos()
                val current = buffer.readUtf()
                val arena = ArenaSettings.STREAM_CODEC.decode(buffer)
                val facing = buffer.readEnum(Direction::class.java)
                val count = buffer.readVarInt().coerceAtMost(64)
                val walls = List(count) {
                    WallEntry(buffer.readBlockPos(), buffer.readVarInt(), buffer.readVarInt(), buffer.readEnum(WallStatus::class.java))
                }
                OpenThemeMenuPayload(terminal, current, arena, facing, walls)
            },
        )
    }
}

/** Client → serveur : applique ce thème à cette borne et à ses écrans. Vérifié par le serveur. */
data class SetThemePayload(val terminal: BlockPos, val theme: String) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<SetThemePayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<SetThemePayload>(LeagueScreen.id("set_theme"))

        val CODEC: StreamCodec<FriendlyByteBuf, SetThemePayload> = StreamCodec.of(
            { buffer, payload ->
                buffer.writeBlockPos(payload.terminal)
                buffer.writeUtf(payload.theme)
            },
            { buffer -> SetThemePayload(buffer.readBlockPos(), buffer.readUtf()) },
        )
    }
}

/** Client → serveur : enregistre ces réglages d'arène sur cette borne. Vérifié et borné par le serveur. */
data class SetArenaPayload(val terminal: BlockPos, val arena: ArenaSettings) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<SetArenaPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<SetArenaPayload>(LeagueScreen.id("set_arena"))

        val CODEC: StreamCodec<FriendlyByteBuf, SetArenaPayload> = StreamCodec.of(
            { buffer, payload ->
                buffer.writeBlockPos(payload.terminal)
                ArenaSettings.STREAM_CODEC.encode(buffer, payload.arena)
            },
            { buffer -> SetArenaPayload(buffer.readBlockPos(), ArenaSettings.STREAM_CODEC.decode(buffer)) },
        )
    }
}

/** Client → serveur : retire cet écran (sa position dans le monde) de la borne. Vérifié par le serveur. */
data class UnlinkWallPayload(val terminal: BlockPos, val wall: BlockPos) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<UnlinkWallPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<UnlinkWallPayload>(LeagueScreen.id("unlink_wall"))

        val CODEC: StreamCodec<FriendlyByteBuf, UnlinkWallPayload> = StreamCodec.of(
            { buffer, payload ->
                buffer.writeBlockPos(payload.terminal)
                buffer.writeBlockPos(payload.wall)
            },
            { buffer -> UnlinkWallPayload(buffer.readBlockPos(), buffer.readBlockPos()) },
        )
    }
}
