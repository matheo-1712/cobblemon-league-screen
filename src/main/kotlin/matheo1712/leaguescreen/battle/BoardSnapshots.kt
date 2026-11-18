package matheo1712.leaguescreen.battle

import com.cobblemon.mod.common.api.battles.model.PokemonBattle
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon
import com.cobblemon.mod.common.entity.npc.NPCBattleActor
import matheo1712.leaguescreen.board.BoardState
import matheo1712.leaguescreen.board.DisplayName
import matheo1712.leaguescreen.board.Phase
import matheo1712.leaguescreen.board.Portrait
import matheo1712.leaguescreen.board.PortraitKind
import matheo1712.leaguescreen.board.SideView
import matheo1712.leaguescreen.board.SlotView
import matheo1712.leaguescreen.compat.TrainerLookups
import matheo1712.leaguescreen.screen.BoardLayout
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.contents.TranslatableContents
import java.util.UUID

/** Traduit un combat Cobblemon et son [BattleRecord] en ce qu'un mur affiche. */
object BoardSnapshots {

    /** L'identifiant sous lequel un Pokémon de combat est suivi dans un [BattleRecord]. */
    fun idOf(pokemon: BattlePokemon): UUID = pokemon.effectedPokemon.uuid

    /**
     * Le tableau en direct, vu du côté de [leftActor] (affiché à gauche).
     * Rend [BoardState.OFF] si le combat n'oppose pas exactement deux camps.
     */
    fun live(battle: PokemonBattle, record: BattleRecord, leftActor: UUID): BoardState {
        if (record.sides.size != 2) return BoardState.OFF
        val leftSide = record.sideOf(leftActor).takeIf { it >= 0 } ?: 0
        val sides = listOf(leftSide, 1 - leftSide).map { index ->
            val sideRecord = record.sides[index]
            val actor = battle.actors.firstOrNull { it.uuid == sideRecord.actorId }
            val slots = (0 until BoardLayout.SLOT_COUNT).map { slot ->
                val state = record.stateOf(index, slot)
                val pokemon = sideRecord.team.getOrNull(slot)?.let { id -> actor?.pokemonList?.firstOrNull { idOf(it) == id } }
                // Sous Illusion, c'est le déguisement qu'on montre : le tableau ne trahit pas ce que le combat cache.
                val shown = pokemon?.let { (it.getIllusion() ?: it).effectedPokemon }
                SlotView.of(state, shown?.species?.resourceIdentifier?.toString(), shown?.aspects ?: emptySet())
            }
            SideView(nameOf(actor), portraitOf(actor), slots)
        }
        return BoardState(Phase.LIVE, sides, -1)
    }

    /** L'écran de victoire, à partir du dernier tableau en direct. [winner] est 0 (gauche) ou 1 (droite). */
    fun victory(live: BoardState, winner: Int): BoardState = live.copy(phase = Phase.VICTORY, winner = winner)

    private fun nameOf(actor: BattleActor?): DisplayName {
        if (actor == null) return DisplayName("?", false)
        if (actor is NPCBattleActor) TrainerLookups.current?.describe(actor.npc)?.let { return it.name }
        return displayName(actor.getName())
    }

    private fun portraitOf(actor: BattleActor?): Portrait = when (actor) {
        is PlayerBattleActor -> Portrait(PortraitKind.PLAYER, actor.uuid.toString())
        is NPCBattleActor -> TrainerLookups.current?.describe(actor.npc)?.let { info ->
            info.skinTexture?.let { Portrait(PortraitKind.TEXTURE, it) } ?: Portrait(PortraitKind.TRAINER, info.trainerId)
        } ?: Portrait.NONE
        else -> Portrait.NONE
    }

    /** Une clé de traduction voyage telle quelle : c'est le client qui la traduit, dans sa langue. */
    private fun displayName(component: Component): DisplayName {
        val contents = component.contents
        return if (contents is TranslatableContents) DisplayName(contents.key, true) else DisplayName(component.string, false)
    }
}
