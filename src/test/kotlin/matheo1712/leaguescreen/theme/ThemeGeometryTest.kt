package matheo1712.leaguescreen.theme

import matheo1712.leaguescreen.screen.Frame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThemeGeometryTest {

    private val tolerance = 0.001f
    private val crown = 5f / 3f

    private fun assertFrame(expected: Frame, actual: Frame) {
        assertEquals(expected.x, actual.x, tolerance, "x")
        assertEquals(expected.y, actual.y, tolerance, "y")
        assertEquals(expected.width, actual.width, tolerance, "width")
        assertEquals(expected.height, actual.height, tolerance, "height")
    }

    @Test
    fun `un mur 5x3 est rempli par un cadre 5 sur 3`() = assertFrame(Frame(0f, 0f, 5f, 3f), ThemeGeometry.frame(5f, 3f, crown))

    @Test
    fun `16x9 est limite par la hauteur`() = assertFrame(Frame(0.5f, 0f, 15f, 9f), ThemeGeometry.frame(16f, 9f, crown))

    @Test
    fun `8x2 est centre en largeur`() = assertFrame(Frame(2.3333f, 0f, 3.3333f, 2f), ThemeGeometry.frame(8f, 2f, crown))

    @Test
    fun `1x1 est centre en hauteur`() = assertFrame(Frame(0f, 0.2f, 1f, 0.6f), ThemeGeometry.frame(1f, 1f, crown))

    @Test
    fun `le cadre suit la proportion du theme`() {
        // Un thème en bandeau 1600 × 400 sur un mur 16 × 9 : toute la largeur, 4 de haut, centré.
        assertFrame(Frame(0f, 2.5f, 16f, 4f), ThemeGeometry.frame(16f, 9f, 1600f / 400f))
    }

    @Test
    fun `une boite de maquette tombe au bon endroit du mur`() {
        val frame = Frame(0f, 0f, 5f, 3f)
        // Le quart haut gauche de la maquette : en blocs, le haut de la maquette est le haut du mur.
        assertFrame(Frame(0f, 1.5f, 2.5f, 1.5f), ThemeGeometry.toWall(Box(0f, 0f, 750f, 450f), frame, 1500f, 900f))
        // Le quart bas droit.
        assertFrame(Frame(2.5f, 0f, 2.5f, 1.5f), ThemeGeometry.toWall(Box(750f, 450f, 750f, 450f), frame, 1500f, 900f))
    }

    @Test
    fun `une boite suit le decalage du cadre`() {
        val frame = Frame(0.5f, 0f, 15f, 9f)
        assertFrame(Frame(0.5f, 0f, 15f, 9f), ThemeGeometry.toWall(Box(0f, 0f, 1500f, 900f), frame, 1500f, 900f))
    }

    @Test
    fun `les calques avancent d'un centimetre, le premier devant le fond`() {
        assertTrue(ThemeGeometry.layerZ(0) > ThemeGeometry.BACKGROUND_Z)
        assertEquals(0.01f, ThemeGeometry.layerZ(1) - ThemeGeometry.layerZ(0), 0.0001f)
        assertEquals(0.01f, ThemeGeometry.layerZ(17) - ThemeGeometry.layerZ(16), 0.0001f)
    }

    @Test
    fun `les elements d'un calque restent sous le calque suivant`() {
        // Un calque peut empiler jusqu'à quatre éléments à SUB_STEP l'un de l'autre.
        assertTrue(4 * ThemeGeometry.SUB_STEP < ThemeGeometry.LAYER_STEP)
        assertTrue(ThemeGeometry.SUB_STEP >= 0.002f)
    }
}
