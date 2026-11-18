package matheo1712.leaguescreen

import kotlin.test.Test
import kotlin.test.assertEquals

class SmokeTest {

    @Test
    fun `les identifiants du mod sont dans son espace de noms`() {
        assertEquals("cobblemon-league-screen:screen", LeagueScreen.id("screen").toString())
    }
}
