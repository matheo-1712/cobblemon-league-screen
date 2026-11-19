package matheo1712.leaguescreen.theme

import matheo1712.leaguescreen.theme.ThemeRules.Answer
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeRulesTest {

    @Test
    fun `cas nominal`() = assertEquals(Answer.APPLY, ThemeRules.decide(3.0, wallValid = true, themeExists = true))

    @Test
    fun `huit blocs sont acceptes, au-dela non`() {
        assertEquals(Answer.APPLY, ThemeRules.decide(8.0, wallValid = true, themeExists = true))
        assertEquals(Answer.TOO_FAR, ThemeRules.decide(8.01, wallValid = true, themeExists = true))
    }

    @Test
    fun `trop loin passe avant tout le reste`() {
        assertEquals(Answer.TOO_FAR, ThemeRules.decide(40.0, wallValid = false, themeExists = false))
    }

    @Test
    fun `sans mur valide`() = assertEquals(Answer.NO_WALL, ThemeRules.decide(2.0, wallValid = false, themeExists = false))

    @Test
    fun `theme inconnu`() = assertEquals(Answer.UNKNOWN_THEME, ThemeRules.decide(2.0, wallValid = true, themeExists = false))

    @Test
    fun `chaque reponse a sa cle de message`() {
        assertEquals("message.cobblemon-league-screen.theme_applied", Answer.APPLY.messageKey)
        assertEquals("message.cobblemon-league-screen.no_wall", Answer.NO_WALL.messageKey)
        assertEquals(Answer.entries.size, Answer.entries.map { it.messageKey }.toSet().size)
    }
}
