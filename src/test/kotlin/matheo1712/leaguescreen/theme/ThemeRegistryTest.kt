package matheo1712.leaguescreen.theme

import net.minecraft.resources.ResourceLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThemeRegistryTest {

    private fun file(namespace: String, path: String) =
        ResourceLocation.fromNamespaceAndPath(namespace, "leaguescreen/themes/$path.json")

    private val indigo = """{ "name": "Indigo", "colors": { "accent": "#C0C0C0" } }"""

    @Test
    fun `les fichiers deviennent des identifiants`() {
        val result = ThemeRegistry.load(mapOf(file("mon_pack", "kanto/indigo") to indigo))
        assertTrue(ResourceLocation.parse("mon_pack:kanto/indigo") in result.themes)
        assertEquals(indigo, result.raw[ResourceLocation.parse("mon_pack:kanto/indigo")])
    }

    @Test
    fun `un theme reprend de la couronne ce qu'il ne definit pas`() {
        val result = ThemeRegistry.load(mapOf(file("mon_pack", "indigo") to indigo))
        val theme = result.themes.getValue(ResourceLocation.parse("mon_pack:indigo"))
        assertEquals(0xFFC0C0C0.toInt(), theme.colors["accent"])
        assertEquals(Couronne.theme.colors["left"], theme.colors["left"])
        assertEquals(Couronne.theme.slots, theme.slots)
    }

    @Test
    fun `un theme casse est ecarte et les autres restent`() {
        val result = ThemeRegistry.load(
            mapOf(
                file("mon_pack", "indigo") to indigo,
                file("mon_pack", "casse") to """{ "background": "bleu" }""",
            ),
        )
        assertTrue(ResourceLocation.parse("mon_pack:indigo") in result.themes)
        assertFalse(ResourceLocation.parse("mon_pack:casse") in result.themes)
        assertFalse(ResourceLocation.parse("mon_pack:casse") in result.raw)
        assertTrue(result.errors.single().contains("mon_pack:casse"))
        assertTrue(result.errors.single().contains("background"))
    }

    @Test
    fun `les avertissements nomment leur theme`() {
        val result = ThemeRegistry.load(
            mapOf(file("mon_pack", "indigo") to """{ "live": [ { "type": "hologramme", "w": 1, "h": 1 } ] }"""),
        )
        assertTrue(result.warnings.single().startsWith("mon_pack:indigo"))
    }

    @Test
    fun `la couronne est toujours la`() {
        val result = ThemeRegistry.load(emptyMap())
        assertEquals(Couronne.theme, result.themes[Couronne.id])
        assertEquals(Couronne.json, result.raw[Couronne.id])
    }

    @Test
    fun `une couronne illisible dans un pack laisse celle du mod`() {
        val result = ThemeRegistry.load(mapOf(file("cobblemon-league-screen", "couronne") to "{ cassé"))
        assertEquals(Couronne.theme, result.themes[Couronne.id])
        assertEquals(1, result.errors.size)
    }

    @Test
    fun `un fichier hors du dossier des themes est ignore`() {
        val stray = ResourceLocation.fromNamespaceAndPath("mon_pack", "autre/indigo.json")
        val result = ThemeRegistry.load(mapOf(stray to indigo))
        assertEquals(setOf(Couronne.id), result.themes.keys)
    }
}
