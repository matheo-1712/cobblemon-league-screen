package matheo1712.leaguescreen.battle

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class ClickRulesTest {

    private val battle = UUID.randomUUID()

    /** Un clic valide : mur relié, joueur en combat contre un seul autre dresseur, mur libre. */
    private val nominal = ClickContext(
        wallValid = true,
        playerBattle = battle,
        hasWildActor = false,
        actorCount = 2,
        wallBattle = null,
    )

    @Test
    fun `sans mur valide`() {
        assertEquals(ClickResult.NO_WALL, ClickRules.decide(nominal.copy(wallValid = false)))
    }

    @Test
    fun `sans mur valide passe avant tout le reste`() {
        val context = nominal.copy(wallValid = false, playerBattle = null)
        assertEquals(ClickResult.NO_WALL, ClickRules.decide(context))
    }

    @Test
    fun `hors combat`() {
        assertEquals(ClickResult.NOT_IN_BATTLE, ClickRules.decide(nominal.copy(playerBattle = null)))
    }

    @Test
    fun `contre un pokemon sauvage`() {
        assertEquals(ClickResult.WILD_BATTLE, ClickRules.decide(nominal.copy(hasWildActor = true)))
    }

    @Test
    fun `a quatre dresseurs`() {
        assertEquals(ClickResult.TOO_MANY_TRAINERS, ClickRules.decide(nominal.copy(actorCount = 4)))
    }

    @Test
    fun `a trois dresseurs`() {
        assertEquals(ClickResult.TOO_MANY_TRAINERS, ClickRules.decide(nominal.copy(actorCount = 3)))
    }

    @Test
    fun `l'adversaire reclique`() {
        assertEquals(ClickResult.ALREADY_FOLLOWED, ClickRules.decide(nominal.copy(wallBattle = battle)))
    }

    @Test
    fun `le mur suit un autre combat`() {
        assertEquals(ClickResult.WALL_BUSY, ClickRules.decide(nominal.copy(wallBattle = UUID.randomUUID())))
    }

    @Test
    fun `cas nominal`() {
        assertEquals(ClickResult.FOLLOW, ClickRules.decide(nominal))
    }

    @Test
    fun `chaque resultat a sa cle de message`() {
        assertEquals("message.cobblemon-league-screen.follow", ClickResult.FOLLOW.messageKey)
        assertEquals("message.cobblemon-league-screen.wall_busy", ClickResult.WALL_BUSY.messageKey)
        assertEquals(ClickResult.entries.size, ClickResult.entries.map { it.messageKey }.toSet().size)
    }

    @Test
    fun `le resume dit suit si un ecran suit`() {
        assertEquals(ClickResult.FOLLOW, ClickRules.summary(listOf(ClickResult.WALL_BUSY, ClickResult.FOLLOW)))
    }

    @Test
    fun `le resume sans ecran dit qu'aucun n'est relie`() {
        assertEquals(ClickResult.NO_WALL, ClickRules.summary(emptyList()))
    }

    @Test
    fun `le resume prefere deja suivi a occupe`() {
        assertEquals(ClickResult.ALREADY_FOLLOWED, ClickRules.summary(listOf(ClickResult.WALL_BUSY, ClickResult.ALREADY_FOLLOWED)))
    }

    @Test
    fun `le resume garde la raison commune`() {
        assertEquals(ClickResult.WILD_BATTLE, ClickRules.summary(listOf(ClickResult.WILD_BATTLE, ClickResult.WILD_BATTLE)))
    }
}
