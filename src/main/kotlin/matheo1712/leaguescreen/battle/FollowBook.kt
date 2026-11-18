package matheo1712.leaguescreen.battle

import VICTORY_TICKS
import net.minecraft.core.GlobalPos
import java.util.UUID

/**
 * Un mur abonné à un combat. [wall] est la position de son bloc de contrôle ; [leftActor] le
 * dresseur affiché à gauche (celui qui a cliqué). [victoryUntil] vaut [LIVE] tant que le combat
 * dure, puis le tick où l'écran de victoire s'éteint.
 */
data class Follow(val wall: GlobalPos, val battleId: UUID, val leftActor: UUID, val victoryUntil: Long) {

    val live: Boolean get() = victoryUntil == LIVE

    companion object {
        const val LIVE = -1L
    }
}

/**
 * Qui suit quoi : un mur suit au plus un combat, un combat peut être suivi par plusieurs murs.
 * Ne connaît ni le monde ni Cobblemon ; le temps lui est donné en ticks.
 */
class FollowBook {

    private val follows = linkedMapOf<GlobalPos, Follow>()

    /** Abonne le mur ; remplace ce qu'il suivait, écran de victoire compris. */
    fun follow(wall: GlobalPos, battleId: UUID, leftActor: UUID, now: Long) {
        follows[wall] = Follow(wall, battleId, leftActor, Follow.LIVE)
    }

    /** Le combat que ce mur suit **en direct** : nul s'il est libre ou s'il montre une victoire. */
    fun battleOf(wall: GlobalPos): UUID? = follows[wall]?.takeIf { it.live }?.battleId

    fun get(wall: GlobalPos): Follow? = follows[wall]

    fun wallsOf(battleId: UUID): List<Follow> = follows.values.filter { it.battleId == battleId }

    fun liveBattles(): Set<UUID> = follows.values.filter { it.live }.mapTo(linkedSetOf()) { it.battleId }

    fun all(): List<Follow> = follows.values.toList()

    /**
     * Le combat est fini. Avec un vainqueur, ses murs passent à l'écran de victoire pour
     * [VICTORY_TICKS] ; sans vainqueur, ils sont retirés tout de suite.
     *
     * @return les murs retirés sur le moment, à éteindre
     */
    fun finish(battleId: UUID, hasWinner: Boolean, now: Long): List<GlobalPos> {
        val concerned = follows.values.filter { it.battleId == battleId && it.live }
        if (hasWinner) {
            concerned.forEach { follows[it.wall] = it.copy(victoryUntil = now + VICTORY_TICKS) }
            return emptyList()
        }
        concerned.forEach { follows.remove(it.wall) }
        return concerned.map { it.wall }
    }

    /** @return les murs dont l'écran de victoire vient de se terminer, retirés du carnet, à éteindre */
    fun expire(now: Long): List<GlobalPos> {
        val done = follows.values.filter { !it.live && now >= it.victoryUntil }.map { it.wall }
        done.forEach { follows.remove(it) }
        return done
    }

    fun drop(wall: GlobalPos) {
        follows.remove(wall)
    }

    fun clear() = follows.clear()
}
