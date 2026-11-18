package matheo1712.leaguescreen.battle

import POLL_INTERVAL
import com.cobblemon.mod.common.api.battles.model.PokemonBattle
import com.cobblemon.mod.common.api.battles.model.actor.ActorType
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor
import com.cobblemon.mod.common.api.battles.model.actor.EntityBackedBattleActor
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.battles.BattleRegistry
import matheo1712.leaguescreen.LeagueScreen
import matheo1712.leaguescreen.arena.ArenaTerminals
import matheo1712.leaguescreen.arena.AutoStart
import matheo1712.leaguescreen.block.ScreenBlockEntity
import matheo1712.leaguescreen.screen.WallRect
import matheo1712.leaguescreen.block.TerminalBlockEntity
import matheo1712.leaguescreen.board.BoardState
import matheo1712.leaguescreen.screen.ScreenWalls
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.core.GlobalPos
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3
import java.util.UUID

/**
 * Le cœur côté serveur : tient un [BattleRecord] pour chaque combat entre dresseurs, et pousse
 * vers chaque mur abonné ce qu'il doit afficher.
 *
 * Tous les combats non sauvages sont suivis **dès leur début**, mur ou pas : quand un dresseur
 * clique en plein combat, l'historique (qui a déjà été envoyé) est complet.
 *
 * Deux sources se recoupent : les événements de Cobblemon (envoi d'un Pokémon, victoire, fuite),
 * et une sonde toutes les [POLL_INTERVAL] ticks, qui relit qui est sur le terrain et qui est K.O.,
 * et qui éteint les murs d'un combat disparu sans prévenir (déconnexion, arrêt forcé).
 */
object BattleTracker {
    private val records = hashMapOf<UUID, BattleRecord>()

    /** Où se tenait chaque dresseur au début de chaque combat : c'est ce qui décide des côtés. */
    private val startPositions = hashMapOf<UUID, Map<UUID, Vec3>>()
    private val book = FollowBook()

    /** Le dernier tableau en direct de chaque mur : sert de base à l'écran de victoire. */
    private val lastBoards = hashMapOf<GlobalPos, BoardState>()
    private val victories = hashMapOf<GlobalPos, BoardState>()

    private var server: MinecraftServer? = null
    private var tick = 0L

    fun register() {
        CobblemonEvents.BATTLE_STARTED_POST.subscribe { event -> onStart(event.battle) }
        CobblemonEvents.POKEMON_SENT_POST.subscribe { event ->
            val id = event.pokemon.uuid
            records.values.forEach { it.markSent(id) }
        }
        CobblemonEvents.BATTLE_VICTORY.subscribe { event ->
            end(event.battle, event.battle.battleId, event.winners.firstOrNull()?.uuid)
        }
        CobblemonEvents.BATTLE_FLED.subscribe { event -> end(event.battle, event.battle.battleId, null) }

        ServerLifecycleEvents.SERVER_STARTED.register { server = it }
        ServerLifecycleEvents.SERVER_STOPPED.register { clear() }
        ServerTickEvents.END_SERVER_TICK.register { onTick(it) }
    }

    /** Un dresseur clique sur la borne. Répond dans sa barre d'action ; abonne le mur si tout va bien. */
    fun onTerminalUsed(player: ServerPlayer, terminal: TerminalBlockEntity) {
        val battle = BattleRegistry.getBattleByParticipatingPlayer(player)
        // Chaque écran de la borne répond pour lui-même ; le joueur voit le résumé.
        val results = ScreenWalls.linkedWalls(player.server, terminal).map { linked ->
            val controller = linked.controller
            val rect = linked.rect
            val wall = controller?.let { GlobalPos.of(it.level!!.dimension(), it.blockPos) }
            val result = ClickRules.decide(
                ClickContext(
                    wallValid = rect != null,
                    playerBattle = battle?.battleId,
                    hasWildActor = battle?.actors?.any { it.type == ActorType.WILD } ?: false,
                    actorCount = battle?.actors?.count() ?: 0,
                    wallBattle = wall?.let { book.battleOf(it) },
                )
            )
            if (result == ClickResult.FOLLOW && battle != null && wall != null && rect != null) {
                val clicker = battle.actors.first { player.uuid in it.getPlayerUUIDs() }
                val opponent = battle.actors.first { it !== clicker }
                controller.applyTheme(terminal.theme)
                startFollowing(wall, rect, battle, clicker, opponent)
            }
            result
        }
        player.displayClientMessage(Component.translatable(ClickRules.summary(results).messageKey), true)
    }

    /** Le mur a changé de forme ou a disparu : il oublie ce qu'il suivait. Son bloc s'est déjà éteint. */
    fun wallChanged(level: ServerLevel, wall: GlobalPos) {
        book.drop(wall)
        lastBoards.remove(wall)
        victories.remove(wall)
    }

    private fun onStart(battle: PokemonBattle) {
        if (battle.actors.any { it.type == ActorType.WILD }) return
        records[battle.battleId] = recordOf(battle)
        startPositions[battle.battleId] = battle.actors.mapNotNull { actor -> positionOf(actor)?.let { actor.uuid to it } }.toMap()
        autoStart(battle)
    }

    /**
     * Abonne [wall] au combat, chacun du côté où il se tenait au début du combat (pas où il est
     * maintenant : celui qui clique a marché jusqu'à la borne). [first] reste à gauche si c'est ambigu.
     */
    private fun startFollowing(wall: GlobalPos, rect: WallRect, battle: PokemonBattle, first: BattleActor, second: BattleActor) {
        val record = records.getOrPut(battle.battleId) { recordOf(battle) }
        val at = { actor: BattleActor -> startPositions[battle.battleId]?.get(actor.uuid) ?: positionOf(actor) }
        val left = SideRules.leftActor(rect, first.uuid, at(first), second.uuid, at(second))
        victories.remove(wall)
        book.follow(wall, battle.battleId, left, tick)
        observe(battle, record)
        pushLive(wall, battle, record, left)
    }

    /** Les bornes dont l'arène contient les deux dresseurs lancent leur écran toutes seules. */
    private fun autoStart(battle: PokemonBattle) {
        val server = server ?: return
        val actors = battle.actors.toList()
        if (actors.size != 2) return
        val positions = actors.map { startPositions[battle.battleId]?.get(it.uuid) }
        // Les dresseurs doivent être dans le même monde que la borne : on prend celui de leur entité.
        val dimensions = actors.map { (it as? EntityBackedBattleActor<*>)?.entity?.level()?.dimension() }

        for (spot in ArenaTerminals.all()) {
            if (dimensions.any { it != spot.dimension() }) continue
            val level = server.getLevel(spot.dimension()) ?: continue
            if (!level.isLoaded(spot.pos())) continue
            val terminal = level.getBlockEntity(spot.pos()) as? TerminalBlockEntity ?: continue
            var followed = false
            for (linked in ScreenWalls.linkedWalls(server, terminal)) {
                val controller = linked.controller
                val rect = linked.rect
                val wall = controller?.let { GlobalPos.of(it.level!!.dimension(), it.blockPos) }
                val follow = AutoStart.shouldFollow(
                    settings = terminal.arena,
                    terminal = terminal.blockPos,
                    facing = terminal.facing,
                    positions = positions,
                    actorCount = actors.size,
                    hasWildActor = actors.any { it.type == ActorType.WILD },
                    wallValid = rect != null,
                    wallBusy = wall != null && book.battleOf(wall) != null,
                )
                if (!follow || wall == null || rect == null) continue
                controller.applyTheme(terminal.theme)
                startFollowing(wall, rect, battle, actors[0], actors[1])
                followed = true
            }
            if (!followed) continue
            actors.flatMap { it.getPlayerUUIDs() }.mapNotNull(server.playerList::getPlayer).forEach {
                it.displayClientMessage(Component.translatable("message.${LeagueScreen.MOD_ID}.arena_started"), true)
            }
        }
    }

    /** Où se tient le dresseur, s'il a une entité dans le monde (joueur ou PNJ). */
    private fun positionOf(actor: BattleActor): Vec3? = (actor as? EntityBackedBattleActor<*>)?.entity?.position()

    private fun recordOf(battle: PokemonBattle): BattleRecord = BattleRecord(
        battle.battleId,
        battle.actors.map { actor -> SideRecord(actor.uuid, actor.pokemonList.map(BoardSnapshots::idOf)) },
    )

    private fun observe(battle: PokemonBattle, record: BattleRecord) {
        val active = battle.actors.flatMap { actor ->
            actor.activePokemon.mapNotNull { it.battlePokemon?.let(BoardSnapshots::idOf) }
        }.toSet()
        val fainted = battle.actors.flatMap { actor ->
            actor.pokemonList.filter { it.health <= 0 }.map(BoardSnapshots::idOf)
        }.toSet()
        record.observe(active, fainted)
    }

    /**
     * Le combat est fini. Avec un vainqueur, ses murs passent à l'écran de victoire ; sans, ils
     * s'éteignent. [battle] peut être nul si le combat a déjà disparu du registre.
     */
    private fun end(battle: PokemonBattle?, battleId: UUID, winnerActor: UUID?) {
        startPositions.remove(battleId)
        val record = records.remove(battleId) ?: return
        if (record.finished) return
        if (battle != null) observe(battle, record)
        record.finish(winnerActor)

        val hasWinner = record.winnerSide >= 0
        if (hasWinner) {
            val winnerActorId = record.sides[record.winnerSide].actorId
            for (follow in book.wallsOf(battleId).filter { it.live }) {
                val live = battle?.let { BoardSnapshots.live(it, record, follow.leftActor) } ?: lastBoards[follow.wall]
                if (live == null || live == BoardState.OFF) continue
                val winnerOnBoard = if (winnerActorId == follow.leftActor) 0 else 1
                victories[follow.wall] = BoardSnapshots.victory(live, winnerOnBoard)
            }
        }
        val dropped = book.finish(battleId, hasWinner, tick)
        dropped.forEach { switchOff(it) }
        // Un mur dont on n'a jamais eu de tableau n'a pas de victoire à montrer : il s'éteint.
        book.wallsOf(battleId).filter { it.wall !in victories }.forEach {
            book.drop(it.wall)
            switchOff(it.wall)
        }
        book.wallsOf(battleId).forEach { follow -> victories[follow.wall]?.let { push(follow.wall, it) } }
    }

    private fun onTick(server: MinecraftServer) {
        this.server = server
        tick++
        book.expire(tick).forEach(::switchOff)
        if (tick % POLL_INTERVAL != 0L) return

        for ((id, record) in records.toList()) {
            val battle = BattleRegistry.getBattle(id)
            if (battle == null || battle.ended) {
                end(battle, id, battle?.winners?.firstOrNull()?.uuid)
                continue
            }
            observe(battle, record)
        }

        for (follow in book.all()) {
            if (follow.live) {
                val record = records[follow.battleId]
                val battle = BattleRegistry.getBattle(follow.battleId)
                if (record == null || battle == null) {
                    book.drop(follow.wall)
                    switchOff(follow.wall)
                    continue
                }
                pushLive(follow.wall, battle, record, follow.leftActor)
            } else {
                // Repoussé à chaque sonde : un mur dont le tronçon vient d'être rechargé retrouve sa victoire.
                victories[follow.wall]?.let { push(follow.wall, it) }
            }
        }
    }

    private fun pushLive(wall: GlobalPos, battle: PokemonBattle, record: BattleRecord, leftActor: UUID) {
        val board = BoardSnapshots.live(battle, record, leftActor)
        lastBoards[wall] = board
        push(wall, board)
    }

    private fun switchOff(wall: GlobalPos) {
        lastBoards.remove(wall)
        victories.remove(wall)
        push(wall, BoardState.OFF)
    }

    /** Envoie un tableau au bloc de contrôle, s'il est chargé. Un mur qui n'existe plus est oublié. */
    private fun push(wall: GlobalPos, board: BoardState) {
        val level = server?.getLevel(wall.dimension()) ?: return
        if (!level.isLoaded(wall.pos())) return
        val entity = level.getBlockEntity(wall.pos()) as? ScreenBlockEntity
        if (entity?.wall == null) {
            book.drop(wall)
            lastBoards.remove(wall)
            victories.remove(wall)
            return
        }
        entity.show(board)
    }

    /** Le bloc de contrôle du mur relié à cette borne, s'il existe, est chargé et à portée. */
    private fun clear() {
        records.clear()
        startPositions.clear()
        ArenaTerminals.clear()
        book.clear()
        lastBoards.clear()
        victories.clear()
        server = null
        tick = 0
    }
}
