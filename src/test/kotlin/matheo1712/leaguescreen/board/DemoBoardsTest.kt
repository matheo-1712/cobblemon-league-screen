package matheo1712.leaguescreen.board

import kotlin.test.Test
import kotlin.test.assertEquals

class DemoBoardsTest {

    private val uuid = "0f0e0d0c-0b0a-0908-0706-050403020100"

    @Test
    fun `le tableau de demo garde ses etats`() {
        val board = DemoBoards.board("Matheo", uuid, Phase.LIVE)
        assertEquals(Phase.LIVE, board.phase)
        assertEquals(-1, board.winner)
        assertEquals(
            listOf(SlotState.FAINTED, SlotState.BENCH, SlotState.ACTIVE, SlotState.HIDDEN, SlotState.HIDDEN, SlotState.HIDDEN),
            board.sides[0].slots.map { it.state },
        )
        assertEquals(
            listOf(SlotState.FAINTED, SlotState.BENCH, SlotState.ACTIVE, SlotState.HIDDEN, SlotState.EMPTY, SlotState.EMPTY),
            board.sides[1].slots.map { it.state },
        )
    }

    @Test
    fun `le joueur est a gauche avec son skin`() {
        val left = DemoBoards.board("Matheo", uuid, Phase.LIVE).sides[0]
        assertEquals(DisplayName("Matheo", false), left.name)
        assertEquals(Portrait(PortraitKind.PLAYER, uuid), left.portrait)
    }

    @Test
    fun `la victoire est celle du joueur`() = assertEquals(0, DemoBoards.board("Matheo", uuid, Phase.VICTORY).winner)

    @Test
    fun `eteint donne un mur eteint`() = assertEquals(BoardState.OFF, DemoBoards.board("Matheo", uuid, Phase.OFF))
}
