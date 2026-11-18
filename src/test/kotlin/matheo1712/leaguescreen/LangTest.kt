package matheo1712.leaguescreen

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import matheo1712.leaguescreen.battle.ClickResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Une clé absente s'afficherait telle quelle dans la barre d'action : chaque texte doit exister dans les deux langues. */
class LangTest {

    private fun lang(code: String): JsonObject {
        val stream = javaClass.getResourceAsStream("/assets/${LeagueScreen.MOD_ID}/lang/$code.json")
            ?: error("fichier de langue $code introuvable")
        return stream.reader(Charsets.UTF_8).use { JsonParser.parseReader(it).asJsonObject }
    }

    private val needed: List<String> =
        ClickResult.entries.map { it.messageKey } + listOf(
            "block.${LeagueScreen.MOD_ID}.screen",
            "block.${LeagueScreen.MOD_ID}.terminal",
            "itemGroup.${LeagueScreen.MOD_ID}.main",
            "message.${LeagueScreen.MOD_ID}.linked",
            "message.${LeagueScreen.MOD_ID}.too_far",
            "tooltip.${LeagueScreen.MOD_ID}.linked",
            "tooltip.${LeagueScreen.MOD_ID}.unlinked",
            "board.${LeagueScreen.MOD_ID}.victory",
            "theme.${LeagueScreen.MOD_ID}.couronne",
            "message.${LeagueScreen.MOD_ID}.theme_applied",
            "message.${LeagueScreen.MOD_ID}.theme_unknown",
            "message.${LeagueScreen.MOD_ID}.theme_too_far",
            "screen.${LeagueScreen.MOD_ID}.themes",
            "screen.${LeagueScreen.MOD_ID}.apply",
            "screen.${LeagueScreen.MOD_ID}.live",
            "screen.${LeagueScreen.MOD_ID}.victory",
            "screen.${LeagueScreen.MOD_ID}.current",
            "message.${LeagueScreen.MOD_ID}.arena_applied",
            "message.${LeagueScreen.MOD_ID}.arena_started",
            "screen.${LeagueScreen.MOD_ID}.tab_theme",
            "screen.${LeagueScreen.MOD_ID}.tab_arena",
            "screen.${LeagueScreen.MOD_ID}.arena_auto",
            "screen.${LeagueScreen.MOD_ID}.arena_width",
            "screen.${LeagueScreen.MOD_ID}.arena_length",
            "screen.${LeagueScreen.MOD_ID}.arena_height",
            "screen.${LeagueScreen.MOD_ID}.arena_hint",
            "screen.${LeagueScreen.MOD_ID}.arena_show",
            "screen.${LeagueScreen.MOD_ID}.arena_hide",
            "message.${LeagueScreen.MOD_ID}.already_linked",
            "message.${LeagueScreen.MOD_ID}.terminal_full",
            "message.${LeagueScreen.MOD_ID}.wall_unlinked",
            "tooltip.${LeagueScreen.MOD_ID}.wall",
            "tooltip.${LeagueScreen.MOD_ID}.arena",
            "screen.${LeagueScreen.MOD_ID}.tab_walls",
            "screen.${LeagueScreen.MOD_ID}.walls_empty",
            "screen.${LeagueScreen.MOD_ID}.walls_hint",
            "screen.${LeagueScreen.MOD_ID}.wall_row",
            "screen.${LeagueScreen.MOD_ID}.wall_ready",
            "screen.${LeagueScreen.MOD_ID}.wall_missing",
            "screen.${LeagueScreen.MOD_ID}.wall_too_far",
            "screen.${LeagueScreen.MOD_ID}.wall_unloaded",
            "screen.${LeagueScreen.MOD_ID}.unlink",
        )

    @Test
    fun `chaque texte existe en francais`() {
        val fr = lang("fr_fr")
        assertEquals(emptyList(), needed.filterNot { fr.has(it) })
    }

    @Test
    fun `chaque texte existe en anglais`() {
        val en = lang("en_us")
        assertEquals(emptyList(), needed.filterNot { en.has(it) })
    }

    @Test
    fun `les deux langues ont les memes cles`() {
        assertEquals(lang("fr_fr").keySet(), lang("en_us").keySet())
        assertTrue(lang("fr_fr").keySet().isNotEmpty())
    }
}
