package matheo1712.leaguescreen.theme

import net.minecraft.resources.ResourceLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Le style d'un portrait : le buste à plat par défaut, ou la figure entière en 3D. */
class PortraitStyleTest {

    private val id = ResourceLocation.fromNamespaceAndPath("test", "portrait")

    private fun portrait(extra: String): PortraitLayer {
        val json = """{ "live": [ { "type": "portrait", "side": "left", "x": 0, "y": 0, "w": 100, "h": 200 $extra } ] }"""
        return ThemeParser.parse(id, json, null).getOrThrow().live.single() as PortraitLayer
    }

    @Test
    fun `sans style, c'est le buste`() {
        val layer = portrait("")
        assertFalse(layer.figure)
        assertEquals(0f, layer.yaw)
        assertEquals(0f, layer.pitch)
    }

    @Test
    fun `l'inclinaison de la tete est bornee`() {
        assertEquals(-12f, portrait(""", "style": "figure", "pitch": -12""").pitch)
        assertEquals(60f, portrait(""", "style": "figure", "pitch": 400""").pitch)
    }

    @Test
    fun `le style figure et sa rotation`() {
        val layer = portrait(""", "style": "figure", "yaw": 25""")
        assertTrue(layer.figure)
        assertEquals(25f, layer.yaw)
    }

    @Test
    fun `un style inconnu rejette le theme en nommant le champ`() {
        val json = """{ "live": [ { "type": "portrait", "side": "left", "w": 1, "h": 1, "style": "statue" } ] }"""
        val error = ThemeParser.parse(id, json, null).exceptionOrNull()?.message ?: ""
        assertTrue(error.contains("live[0].style"), error)
    }

    @Test
    fun `la couronne livree montre les dresseurs en entier`() {
        val portraits = Couronne.theme.live.filterIsInstance<PortraitLayer>() + Couronne.theme.victory.filterIsInstance<PortraitLayer>()
        assertTrue(portraits.isNotEmpty() && portraits.all { it.figure })
    }
}
