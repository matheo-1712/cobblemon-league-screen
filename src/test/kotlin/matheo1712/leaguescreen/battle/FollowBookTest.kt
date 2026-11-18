package matheo1712.leaguescreen.battle

import net.minecraft.core.BlockPos
import net.minecraft.core.GlobalPos
import net.minecraft.world.level.Level
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FollowBookTest {

    private fun wall(x: Int): GlobalPos = GlobalPos.of(Level.OVERWORLD, BlockPos(x, 64, 0))

    private val wallA = wall(0)
    private val wallB = wall(20)
    private val battle = UUID.randomUUID()
    private val player = UUID.randomUUID()

    @Test
    fun `un mur suivi rend son combat`() {
        val book = FollowBook()
        assertNull(book.battleOf(wallA))
        book.follow(wallA, battle, player, now = 100)
        assertEquals(battle, book.battleOf(wallA))
        assertEquals(listOf(Follow(wallA, battle, player, Follow.LIVE)), book.wallsOf(battle))
    }

    @Test
    fun `fin avec vainqueur, le mur reste 200 ticks puis expire`() {
        val book = FollowBook()
        book.follow(wallA, battle, player, now = 100)
        book.finish(battle, hasWinner = true, now = 1000)

        assertEquals(1200L, book.wallsOf(battle).single().victoryUntil)
        assertTrue(book.expire(now = 1199).isEmpty())
        assertEquals(listOf(wallA), book.expire(now = 1200))
        assertTrue(book.wallsOf(battle).isEmpty())
        assertTrue(book.expire(now = 1300).isEmpty())
    }

    @Test
    fun `pendant la victoire le mur est libre pour un nouveau combat`() {
        val book = FollowBook()
        book.follow(wallA, battle, player, now = 100)
        book.finish(battle, hasWinner = true, now = 1000)
        assertNull(book.battleOf(wallA))

        val next = UUID.randomUUID()
        book.follow(wallA, next, player, now = 1050)
        assertEquals(next, book.battleOf(wallA))
        assertTrue(book.wallsOf(battle).isEmpty())
        // L'ancienne victoire ne doit pas éteindre le nouveau combat en expirant.
        assertTrue(book.expire(now = 1200).isEmpty())
        assertEquals(next, book.battleOf(wallA))
    }

    @Test
    fun `fin sans vainqueur, le mur est retire tout de suite`() {
        val book = FollowBook()
        book.follow(wallA, battle, player, now = 100)
        val dropped = book.finish(battle, hasWinner = false, now = 1000)
        assertEquals(listOf(wallA), dropped)
        assertNull(book.battleOf(wallA))
        assertTrue(book.wallsOf(battle).isEmpty())
    }

    @Test
    fun `une fin avec vainqueur ne retire aucun mur sur le moment`() {
        val book = FollowBook()
        book.follow(wallA, battle, player, now = 100)
        assertTrue(book.finish(battle, hasWinner = true, now = 1000).isEmpty())
    }

    @Test
    fun `deux murs peuvent suivre le meme combat`() {
        val book = FollowBook()
        book.follow(wallA, battle, player, now = 100)
        book.follow(wallB, battle, player, now = 110)
        assertEquals(setOf(wallA, wallB), book.wallsOf(battle).map { it.wall }.toSet())
    }

    @Test
    fun `drop retire un mur sans toucher aux autres`() {
        val book = FollowBook()
        book.follow(wallA, battle, player, now = 100)
        book.follow(wallB, battle, player, now = 110)
        book.drop(wallA)
        assertNull(book.battleOf(wallA))
        assertEquals(battle, book.battleOf(wallB))
    }

    @Test
    fun `les combats suivis sont listes une fois chacun`() {
        val book = FollowBook()
        val other = UUID.randomUUID()
        book.follow(wallA, battle, player, now = 100)
        book.follow(wallB, battle, player, now = 100)
        book.follow(wall(40), other, player, now = 100)
        assertEquals(setOf(battle, other), book.liveBattles())
    }

    @Test
    fun `clear oublie tout`() {
        val book = FollowBook()
        book.follow(wallA, battle, player, now = 100)
        book.clear()
        assertNull(book.battleOf(wallA))
        assertTrue(book.all().isEmpty())
    }
}
