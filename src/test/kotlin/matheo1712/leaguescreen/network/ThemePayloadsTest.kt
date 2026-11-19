package matheo1712.leaguescreen.network

import C_COURONNE_ID
import io.netty.buffer.Unpooled
import matheo1712.leaguescreen.arena.ArenaSettings
import matheo1712.leaguescreen.arena.ArenaZone
import matheo1712.leaguescreen.theme.Couronne
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.FriendlyByteBuf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThemePayloadsTest {

    private fun <T> roundTrip(value: T, encode: (FriendlyByteBuf, T) -> Unit, decode: (FriendlyByteBuf) -> T): T {
        val buffer = FriendlyByteBuf(Unpooled.buffer())
        encode(buffer, value)
        return decode(buffer)
    }

    @Test
    fun `aller-retour des themes`() {
        val payload = ThemesPayload(mapOf(C_COURONNE_ID to Couronne.json, "mon_pack:indigo" to """{ "name": "Indigo" }"""))
        assertEquals(payload, roundTrip(payload, ThemesPayload.CODEC::encode, ThemesPayload.CODEC::decode))
    }

    @Test
    fun `un theme de plus de 32 767 caracteres passe`() {
        val big = "{ \"name\": \"" + "x".repeat(100_000) + "\" }"
        val payload = ThemesPayload(mapOf("mon_pack:gros" to big))
        assertEquals(payload, roundTrip(payload, ThemesPayload.CODEC::encode, ThemesPayload.CODEC::decode))
    }

    @Test
    fun `aller-retour de l'ouverture du menu et du choix`() {
        val walls = listOf(
            WallEntry(BlockPos(20, 70, 400), 5, 3, WallStatus.READY),
            WallEntry(BlockPos(-4, 66, 380), 8, 2, WallStatus.TOO_FAR),
            WallEntry(BlockPos(0, 0, 0), 0, 0, WallStatus.MISSING),
        )
        val open = OpenThemeMenuPayload(BlockPos(12, -3, 400), C_COURONNE_ID, ArenaSettings(true, ArenaZone(30, 18, 6)), Direction.EAST, walls)
        assertEquals(open, roundTrip(open, OpenThemeMenuPayload.CODEC::encode, OpenThemeMenuPayload.CODEC::decode))
        val set = SetThemePayload(BlockPos(-5, 64, 7), "mon_pack:indigo")
        assertEquals(set, roundTrip(set, SetThemePayload.CODEC::encode, SetThemePayload.CODEC::decode))
    }

    @Test
    fun `aller-retour des reglages d'arene`() {
        val set = SetArenaPayload(BlockPos(3, 70, -9), ArenaSettings(true, ArenaZone(40, 12, 10)))
        assertEquals(set, roundTrip(set, SetArenaPayload.CODEC::encode, SetArenaPayload.CODEC::decode))
    }

    @Test
    fun `aller-retour du retrait d'un ecran`() {
        val unlink = UnlinkWallPayload(BlockPos(3, 70, -9), BlockPos(20, 72, -9))
        assertEquals(unlink, roundTrip(unlink, UnlinkWallPayload.CODEC::encode, UnlinkWallPayload.CODEC::decode))
    }

    @Test
    fun `capped garde la couronne et compte les omis`() {
        val themes = linkedMapOf(C_COURONNE_ID to Couronne.json)
        for (i in 1..100) themes["mon_pack:t$i"] = "x".repeat(20_000)
        val (kept, omitted) = ThemesPayload.capped(themes, 900_000)
        assertTrue(C_COURONNE_ID in kept)
        assertTrue(kept.entries.sumOf { it.key.length + it.value.toByteArray().size } <= 900_000)
        assertEquals(101, kept.size + omitted)
        assertTrue(omitted > 0)
    }

    @Test
    fun `capped ne retire rien sous la limite`() {
        val themes = mapOf(C_COURONNE_ID to Couronne.json, "mon_pack:indigo" to "{}")
        assertEquals(themes to 0, ThemesPayload.capped(themes, 900_000))
    }
}
