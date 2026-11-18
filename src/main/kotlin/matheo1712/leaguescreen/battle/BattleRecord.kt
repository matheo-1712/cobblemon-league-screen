package matheo1712.leaguescreen.battle

import matheo1712.leaguescreen.board.SlotState
import java.util.UUID

/** Un camp d'un combat : son dresseur et ses Pokémon, dans l'ordre de l'équipe engagée (six au plus). */
class SideRecord(val actorId: UUID, team: List<UUID>) {
    val team: List<UUID> = team.take(MAX_TEAM)

    companion object {
        const val MAX_TEAM = 6
    }
}

/**
 * Ce que le serveur sait d'un combat, sans rien connaître de Cobblemon : quels Pokémon ont été
 * envoyés, lesquels sont sur le terrain, lesquels sont K.O., et qui a gagné.
 *
 * On l'alimente de deux façons qui se recoupent : [markSent] à chaque envoi annoncé par Cobblemon,
 * et [observe] à chaque sonde de l'état du combat. Un Pokémon vu actif ou K.O. est forcément un
 * Pokémon envoyé, même si l'annonce a été manquée.
 */
class BattleRecord(val battleId: UUID, val sides: List<SideRecord>) {

    private val known: Set<UUID> = sides.flatMap { it.team }.toSet()
    private val sent = hashSetOf<UUID>()
    private val fainted = hashSetOf<UUID>()
    private var active: Set<UUID> = emptySet()

    var finished: Boolean = false
        private set

    /** L'indice du camp vainqueur, ou -1 tant qu'il n'y en a pas (ou s'il n'y en aura pas). */
    var winnerSide: Int = -1
        private set

    /** @return vrai si ce Pokémon fait partie du combat et n'était pas encore marqué comme envoyé */
    fun markSent(pokemon: UUID): Boolean = pokemon in known && sent.add(pokemon)

    /**
     * Prend acte de l'état du combat à cet instant.
     *
     * @param active les Pokémon actuellement sur le terrain, tous camps confondus
     * @param fainted les Pokémon actuellement K.O.
     * @return vrai si quelque chose a changé depuis la dernière fois
     */
    fun observe(active: Set<UUID>, fainted: Set<UUID>): Boolean {
        val nowActive = active.filterTo(hashSetOf()) { it in known }
        val nowFainted = fainted.filter { it in known }

        var changed = nowActive != this.active
        this.active = nowActive
        if (this.fainted.addAll(nowFainted)) changed = true
        if (sent.addAll(nowActive)) changed = true
        if (sent.addAll(nowFainted)) changed = true
        return changed
    }

    fun stateOf(side: Int, index: Int): SlotState {
        val pokemon = sides.getOrNull(side)?.team?.getOrNull(index) ?: return SlotState.EMPTY
        return when (pokemon) {
            in fainted -> SlotState.FAINTED
            in active -> SlotState.ACTIVE
            in sent -> SlotState.BENCH
            else -> SlotState.HIDDEN
        }
    }

    /** L'indice du camp de ce dresseur, ou -1 s'il ne participe pas. */
    fun sideOf(actorId: UUID): Int = sides.indexOfFirst { it.actorId == actorId }

    /** Clôt le combat. [winnerActor] est nul pour une fuite, une déconnexion ou un combat interrompu. */
    fun finish(winnerActor: UUID?) {
        finished = true
        winnerSide = winnerActor?.let(::sideOf) ?: -1
    }
}
