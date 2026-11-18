package matheo1712.leaguescreen.board

import net.minecraft.nbt.CompoundTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BoardStateTest {

    private fun side(name: String, slots: List<SlotView>, portrait: Portrait = Portrait.NONE): SideView =
        SideView(DisplayName(name, false), portrait, slots + List(6 - slots.size) { SlotView.EMPTY })

    private fun live(left: List<SlotView>, right: List<SlotView> = List(6) { SlotView.HIDDEN }): BoardState =
        BoardState(Phase.LIVE, listOf(side("Matheo", left), side("Maître", right)), -1)

    @Test
    fun `une case cachee ne garde ni espece ni aspects`() {
        val slot = SlotView.of(SlotState.HIDDEN, "cobblemon:pikachu", setOf("shiny"))
        assertEquals(SlotState.HIDDEN, slot.state)
        assertNull(slot.species)
        assertTrue(slot.aspects.isEmpty())
    }

    @Test
    fun `une case vide ne garde rien non plus`() {
        val slot = SlotView.of(SlotState.EMPTY, "cobblemon:pikachu", setOf("shiny"))
        assertNull(slot.species)
        assertTrue(slot.aspects.isEmpty())
    }

    @Test
    fun `une case revelee garde espece et aspects`() {
        for (state in listOf(SlotState.BENCH, SlotState.ACTIVE, SlotState.FAINTED)) {
            val slot = SlotView.of(state, "cobblemon:pikachu", setOf("shiny"))
            assertEquals("cobblemon:pikachu", slot.species, "$state")
            assertEquals(setOf("shiny"), slot.aspects, "$state")
        }
    }

    @Test
    fun `seuls banc, terrain et ko sont des etats reveles`() {
        assertEquals(
            setOf(SlotState.BENCH, SlotState.ACTIVE, SlotState.FAINTED),
            SlotState.entries.filter { it.revealed }.toSet(),
        )
    }

    @Test
    fun `deux cases de meme contenu sont egales`() {
        assertEquals(
            SlotView.of(SlotState.ACTIVE, "cobblemon:milotic", setOf("female")),
            SlotView.of(SlotState.ACTIVE, "cobblemon:milotic", setOf("female")),
        )
        assertEquals(SlotView.HIDDEN, SlotView.of(SlotState.HIDDEN, "cobblemon:milotic", emptySet()))
    }

    @Test
    fun `aller-retour NBT d'un tableau en direct`() {
        val board = BoardState(
            Phase.LIVE,
            listOf(
                side(
                    "Matheo",
                    listOf(
                        SlotView.of(SlotState.FAINTED, "cobblemon:lucario", emptySet()),
                        SlotView.of(SlotState.BENCH, "cobblemon:dragonite", setOf("shiny", "male")),
                        SlotView.of(SlotState.ACTIVE, "cobblemon:garchomp", emptySet()),
                        SlotView.HIDDEN,
                    ),
                    Portrait(PortraitKind.PLAYER, "0f0e0d0c-0b0a-0908-0706-050403020100"),
                ),
                SideView(
                    DisplayName("trainer.royaume-loutre-mer-trainers.maitre.name", true),
                    Portrait(PortraitKind.TRAINER, "royaume-loutre-mer-trainers:ligue/maitre"),
                    List(6) { SlotView.HIDDEN },
                ),
            ),
            -1,
        )
        assertEquals(board, BoardState.fromTag(board.toTag()))
    }

    @Test
    fun `aller-retour NBT d'une victoire`() {
        val board = live(listOf(SlotView.of(SlotState.ACTIVE, "cobblemon:garchomp", emptySet()))).copy(
            phase = Phase.VICTORY,
            winner = 1,
        )
        val read = BoardState.fromTag(board.toTag())
        assertEquals(Phase.VICTORY, read.phase)
        assertEquals(1, read.winner)
        assertEquals(board, read)
    }

    @Test
    fun `le tag d'une case cachee ne contient pas le mot pikachu`() {
        val board = live(left = listOf(SlotView.of(SlotState.HIDDEN, "cobblemon:pikachu", setOf("shiny"))))
        val written = board.toTag().toString()
        assertFalse(written.contains("pikachu"), written)
        assertFalse(written.contains("shiny"), written)
    }

    @Test
    fun `un portrait de type texture garde son chemin`() {
        val texture = Portrait(PortraitKind.TEXTURE, "royaume-loutre-mer-trainers:textures/trainers/arisoutre.png")
        val board = BoardState(
            Phase.LIVE,
            listOf(side("Matheo", emptyList()), side("Maître", emptyList(), texture)),
            -1,
        )
        assertEquals(texture, BoardState.fromTag(board.toTag()).sides[1].portrait)
    }

    @Test
    fun `un tag vide donne un mur eteint`() = assertEquals(BoardState.OFF, BoardState.fromTag(CompoundTag()))

    @Test
    fun `un tag abime donne un mur eteint`() {
        val tag = live(emptyList()).toTag()
        tag.putString("phase", "N_IMPORTE_QUOI")
        assertEquals(BoardState.OFF, BoardState.fromTag(tag))
    }

    @Test
    fun `une case d'etat inconnu est lue comme cachee`() {
        val tag = live(listOf(SlotView.of(SlotState.ACTIVE, "cobblemon:garchomp", emptySet()))).toTag()
        val firstSlot = tag.getList("sides", 10).getCompound(0).getList("slots", 10).getCompound(0)
        firstSlot.putString("state", "ETAT_DU_FUTUR")
        val read = BoardState.fromTag(tag)
        assertEquals(SlotView.HIDDEN, read.sides[0].slots[0])
    }

    @Test
    fun `le mur eteint n'a ni camp ni vainqueur`() {
        assertEquals(Phase.OFF, BoardState.OFF.phase)
        assertTrue(BoardState.OFF.sides.isEmpty())
        assertEquals(-1, BoardState.OFF.winner)
    }
}
