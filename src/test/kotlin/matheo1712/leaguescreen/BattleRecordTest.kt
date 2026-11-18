package matheo1712.leaguescreen.battle

import matheo1712.leaguescreen.board.SlotState
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BattleRecordTest {

    private fun ids(count: Int): List<UUID> = List(count) { UUID.randomUUID() }

    private val leftActor = UUID.randomUUID()
    private val rightActor = UUID.randomUUID()

    private fun record(left: List<UUID>, right: List<UUID> = ids(6)): BattleRecord =
        BattleRecord(UUID.randomUUID(), listOf(SideRecord(leftActor, left), SideRecord(rightActor, right)))

    private fun BattleRecord.states(side: Int): List<SlotState> = (0 until 6).map { stateOf(side, it) }

    @Test
    fun `au depart tout est cache`() {
        val record = record(ids(6))
        assertEquals(List(6) { SlotState.HIDDEN }, record.states(0))
        assertEquals(List(6) { SlotState.HIDDEN }, record.states(1))
    }

    @Test
    fun `une equipe de quatre laisse deux cases vides`() {
        val record = record(ids(4))
        assertEquals(List(4) { SlotState.HIDDEN } + List(2) { SlotState.EMPTY }, record.states(0))
    }

    @Test
    fun `un pokemon envoye puis rappele reste vu`() {
        val team = ids(6)
        val record = record(team)
        record.markSent(team[0])
        record.observe(active = setOf(team[1]), fainted = emptySet())
        assertEquals(SlotState.BENCH, record.stateOf(0, 0))
        assertEquals(SlotState.ACTIVE, record.stateOf(0, 1))
        assertEquals(SlotState.HIDDEN, record.stateOf(0, 2))
    }

    @Test
    fun `un actif jamais annonce est quand meme revele`() {
        val team = ids(6)
        val record = record(team)
        record.observe(active = setOf(team[3]), fainted = emptySet())
        assertEquals(SlotState.ACTIVE, record.stateOf(0, 3))
        record.observe(active = emptySet(), fainted = emptySet())
        assertEquals(SlotState.BENCH, record.stateOf(0, 3))
    }

    @Test
    fun `ko l'emporte sur actif`() {
        val team = ids(6)
        val record = record(team)
        record.observe(active = setOf(team[0]), fainted = setOf(team[0]))
        assertEquals(SlotState.FAINTED, record.stateOf(0, 0))
    }

    @Test
    fun `un ko reste ko`() {
        val team = ids(6)
        val record = record(team)
        record.observe(active = emptySet(), fainted = setOf(team[0]))
        record.observe(active = emptySet(), fainted = emptySet())
        assertEquals(SlotState.FAINTED, record.stateOf(0, 0))
    }

    @Test
    fun `deux actifs dans le meme camp en double`() {
        val team = ids(6)
        val record = record(team)
        record.observe(active = setOf(team[0], team[1]), fainted = emptySet())
        assertEquals(listOf(SlotState.ACTIVE, SlotState.ACTIVE, SlotState.HIDDEN), record.states(0).take(3))
    }

    @Test
    fun `les deux camps sont suivis separement`() {
        val left = ids(6)
        val right = ids(6)
        val record = record(left, right)
        record.observe(active = setOf(left[0], right[2]), fainted = emptySet())
        assertEquals(SlotState.ACTIVE, record.stateOf(0, 0))
        assertEquals(SlotState.HIDDEN, record.stateOf(0, 2))
        assertEquals(SlotState.ACTIVE, record.stateOf(1, 2))
        assertEquals(SlotState.HIDDEN, record.stateOf(1, 0))
    }

    @Test
    fun `observe dit s'il y a du nouveau`() {
        val team = ids(6)
        val record = record(team)
        assertTrue(record.observe(active = setOf(team[0]), fainted = emptySet()))
        assertFalse(record.observe(active = setOf(team[0]), fainted = emptySet()))
        assertTrue(record.observe(active = setOf(team[0]), fainted = setOf(team[0])))
        assertFalse(record.observe(active = setOf(team[0]), fainted = setOf(team[0])))
    }

    @Test
    fun `markSent dit s'il y a du nouveau`() {
        val team = ids(6)
        val record = record(team)
        assertTrue(record.markSent(team[0]))
        assertFalse(record.markSent(team[0]))
    }

    @Test
    fun `un pokemon inconnu est ignore`() {
        val record = record(ids(6))
        val stranger = UUID.randomUUID()
        assertFalse(record.markSent(stranger))
        assertFalse(record.observe(active = setOf(stranger), fainted = setOf(stranger)))
        assertEquals(List(6) { SlotState.HIDDEN }, record.states(0))
    }

    @Test
    fun `finish avec vainqueur`() {
        val record = record(ids(6))
        assertFalse(record.finished)
        assertEquals(-1, record.winnerSide)
        record.finish(rightActor)
        assertTrue(record.finished)
        assertEquals(1, record.winnerSide)
        assertEquals(record.sideOf(rightActor), record.winnerSide)
    }

    @Test
    fun `finish sans vainqueur`() {
        val record = record(ids(6))
        record.finish(null)
        assertTrue(record.finished)
        assertEquals(-1, record.winnerSide)
    }

    @Test
    fun `sideOf d'un acteur inconnu vaut -1`() {
        assertEquals(-1, record(ids(6)).sideOf(UUID.randomUUID()))
    }

    @Test
    fun `une equipe de sept est tronquee a six`() {
        val team = ids(7)
        val record = record(team)
        assertEquals(6, record.sides[0].team.size)
        assertFalse(record.markSent(team[6]))
    }

    @Test
    fun `un indice hors des six cases est vide`() {
        assertEquals(SlotState.EMPTY, record(ids(6)).stateOf(0, 6))
    }
}
