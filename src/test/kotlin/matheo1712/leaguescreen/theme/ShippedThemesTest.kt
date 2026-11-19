package matheo1712.leaguescreen.theme

import net.minecraft.resources.ResourceLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Chaque thème livré avec le mod se lit sans avertissement et montre tout le combat. */
class ShippedThemesTest {

    private val shipped = listOf("couronne", "bandeau")

    private fun load(name: String): Theme {
        val path = "/data/cobblemon-league-screen/leaguescreen/themes/$name.json"
        val json = assertNotNull(javaClass.getResourceAsStream(path), "$path absent").reader(Charsets.UTF_8).use { it.readText() }
        val id = ResourceLocation.fromNamespaceAndPath("cobblemon-league-screen", name)
        return ThemeParser.parse(id, json, if (name == "couronne") null else Couronne.theme).getOrThrow()
    }

    @Test
    fun `chaque theme livre se lit sans avertissement`() {
        for (name in shipped) assertTrue(load(name).warnings.isEmpty(), "$name : ${load(name).warnings}")
    }

    @Test
    fun `chaque theme livre montre les douze cases, les portraits et les noms`() {
        for (name in shipped) {
            val live = load(name).live
            for (side in listOf(SideRef.LEFT, SideRef.RIGHT)) {
                assertEquals((1..6).toList(), live.filterIsInstance<SlotLayer>().filter { it.side == side }.map { it.index }.sorted(), "$name $side")
                assertEquals(1, live.filterIsInstance<PortraitLayer>().count { it.side == side }, "$name portrait $side")
                assertEquals(1, live.filterIsInstance<NameLayer>().count { it.side == side }, "$name nom $side")
            }
        }
    }

    @Test
    fun `chaque theme livre a un ecran de victoire au nom du vainqueur`() {
        for (name in shipped) {
            val victory = load(name).victory
            assertTrue(victory.filterIsInstance<PortraitLayer>().any { it.side == SideRef.WINNER }, name)
            assertTrue(victory.filterIsInstance<NameLayer>().any { it.side == SideRef.WINNER }, name)
        }
    }

    @Test
    fun `aucun calque ne sort de la maquette`() {
        for (name in shipped) {
            val theme = load(name)
            for (layer in theme.live + theme.victory) {
                val box = layer.box
                assertTrue(box.x >= 0f && box.y >= 0f && box.x + box.w <= theme.canvasWidth + 0.01f && box.y + box.h <= theme.canvasHeight + 0.01f, "$name : $layer")
            }
        }
    }

    @Test
    fun `chaque theme livre a un nom traduit`() {
        val fr = javaClass.getResourceAsStream("/assets/cobblemon-league-screen/lang/fr_fr.json")!!.reader().use { it.readText() }
        for (name in shipped) assertTrue(fr.contains("\"${load(name).name}\""), name)
    }
}
