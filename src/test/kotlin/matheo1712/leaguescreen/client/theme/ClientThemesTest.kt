package matheo1712.leaguescreen.client.theme

import C_COURONNE_ID
import matheo1712.leaguescreen.theme.Couronne
import net.minecraft.resources.ResourceLocation
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ClientThemesTest {

    private val indigo = ResourceLocation.parse("mon_pack:indigo")
    private val indigoJson = """{ "name": "Indigo", "background": "#101010" }"""

    @AfterTest
    fun reset() = ClientThemes.clear()

    @Test
    fun `un theme recu est trouve`() {
        ClientThemes.accept(mapOf(C_COURONNE_ID to Couronne.json, indigo.toString() to indigoJson))
        assertEquals("Indigo", ClientThemes.forWall(indigo).name)
    }

    @Test
    fun `un theme absent donne la couronne`() {
        assertEquals(Couronne.id, ClientThemes.forWall(indigo).id)
    }

    @Test
    fun `un theme retire puis remis revient`() {
        ClientThemes.accept(mapOf(indigo.toString() to indigoJson))
        ClientThemes.accept(mapOf(C_COURONNE_ID to Couronne.json))
        assertEquals(Couronne.id, ClientThemes.forWall(indigo).id)
        ClientThemes.accept(mapOf(indigo.toString() to indigoJson))
        assertEquals(indigo, ClientThemes.forWall(indigo).id)
    }

    @Test
    fun `un theme casse recu est ignore`() {
        ClientThemes.accept(mapOf(indigo.toString() to """{ "background": "bleu" }"""))
        assertEquals(Couronne.id, ClientThemes.forWall(indigo).id)
    }

    @Test
    fun `un identifiant illisible est ignore`() {
        ClientThemes.accept(mapOf("Pas Un Id!" to indigoJson))
        assertEquals(listOf(Couronne.id), ClientThemes.list().map { it.id })
    }

    @Test
    fun `la liste commence par la couronne`() {
        ClientThemes.accept(
            mapOf(
                "mon_pack:zeta" to """{ "name": "Zeta" }""",
                indigo.toString() to indigoJson,
                C_COURONNE_ID to Couronne.json,
            ),
        )
        assertEquals(listOf(Couronne.id, indigo, ResourceLocation.parse("mon_pack:zeta")), ClientThemes.list().map { it.id })
    }
}
