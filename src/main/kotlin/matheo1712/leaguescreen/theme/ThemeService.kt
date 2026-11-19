package matheo1712.leaguescreen.theme

import C_REACH
import matheo1712.leaguescreen.LeagueScreen
import matheo1712.leaguescreen.block.TerminalBlockEntity
import matheo1712.leaguescreen.network.OpenThemeMenuPayload
import matheo1712.leaguescreen.network.SetArenaPayload
import matheo1712.leaguescreen.network.UnlinkWallPayload
import matheo1712.leaguescreen.network.WallEntry
import matheo1712.leaguescreen.network.SetThemePayload
import matheo1712.leaguescreen.network.ThemesPayload
import matheo1712.leaguescreen.screen.ScreenWalls
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3
import kotlin.math.sqrt

/**
 * Le côté serveur des thèmes : envoyer la liste aux joueurs, ouvrir le menu d'une borne, et
 * appliquer le thème choisi après l'avoir vérifié.
 */
object ThemeService {

    /** Un paquet est limité à 1 Mio : on garde une marge. */
    private const val MAX_PAYLOAD_BYTES = 900_000

    fun register() {
        PayloadTypeRegistry.playS2C().register(ThemesPayload.TYPE, ThemesPayload.CODEC)
        PayloadTypeRegistry.playS2C().register(OpenThemeMenuPayload.TYPE, OpenThemeMenuPayload.CODEC)
        PayloadTypeRegistry.playC2S().register(SetThemePayload.TYPE, SetThemePayload.CODEC)
        PayloadTypeRegistry.playC2S().register(SetArenaPayload.TYPE, SetArenaPayload.CODEC)
        PayloadTypeRegistry.playC2S().register(UnlinkWallPayload.TYPE, UnlinkWallPayload.CODEC)
        ServerPlayNetworking.registerGlobalReceiver(UnlinkWallPayload.TYPE) { payload, context -> unlink(context.player(), payload) }
        ServerPlayNetworking.registerGlobalReceiver(SetArenaPayload.TYPE) { payload, context -> applyArena(context.player(), payload) }

        ServerPlayNetworking.registerGlobalReceiver(SetThemePayload.TYPE) { payload, context -> apply(context.player(), payload) }
        ServerPlayConnectionEvents.JOIN.register { handler, _, _ -> sendThemes(handler.player) }
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register { server, _, success ->
            if (success) server.playerList.players.forEach(::sendThemes)
        }
    }

    /** Envoie tous les thèmes à ce joueur, s'il a le mod. */
    fun sendThemes(player: ServerPlayer) {
        if (!ServerPlayNetworking.canSend(player, ThemesPayload.TYPE)) return
        val all = ThemeRegistry.all().mapKeys { it.key.toString() }
        val (kept, omitted) = ThemesPayload.capped(all, MAX_PAYLOAD_BYTES)
        if (omitted > 0) {
            LeagueScreen.LOGGER.warn("{} thème(s) trop volumineux pour être envoyés à {}", omitted, player.gameProfile.name)
        }
        ServerPlayNetworking.send(player, ThemesPayload(kept))
    }

    /**
     * Accroupi + clic droit sur une borne : ouvre son menu, avec tout ce qu'elle retient. Il s'ouvre
     * même sans écran relié : l'arène et le thème se règlent sur la borne elle-même.
     */
    fun openMenu(player: ServerPlayer, terminal: TerminalBlockEntity) {
        if (!ServerPlayNetworking.canSend(player, OpenThemeMenuPayload.TYPE)) return
        val walls = ScreenWalls.linkedWalls(player.server, terminal).map { linked ->
            WallEntry(linked.link.pos(), linked.rect?.width ?: 0, linked.rect?.height ?: 0, linked.status)
        }
        ServerPlayNetworking.send(
            player,
            OpenThemeMenuPayload(terminal.blockPos, terminal.theme.toString(), terminal.arena, terminal.facing, walls),
        )
    }

    /** La borne visée par un paquet, si elle existe et est chargée dans le monde du joueur. */
    private fun terminalAt(player: ServerPlayer, pos: BlockPos): TerminalBlockEntity? {
        val level = player.serverLevel()
        return if (level.isLoaded(pos)) level.getBlockEntity(pos) as? TerminalBlockEntity else null
    }

    private fun distance(player: ServerPlayer, pos: BlockPos): Double = sqrt(player.distanceToSqr(Vec3.atCenterOf(pos)))

    /**
     * Le joueur a choisi un thème. Tout est revérifié ici : le paquet vient du client. Le thème est
     * gardé par la borne et donné à tous ses écrans prêts.
     */
    private fun apply(player: ServerPlayer, payload: SetThemePayload) {
        val terminal = terminalAt(player, payload.terminal)
        val theme = ResourceLocation.tryParse(payload.theme)?.let(ThemeRegistry::get)
        val answer = ThemeRules.decide(distance(player, payload.terminal), terminal != null, theme != null)
        if (answer == ThemeRules.Answer.APPLY && terminal != null && theme != null) {
            terminal.applyTheme(theme.id)
            ScreenWalls.linkedWalls(player.server, terminal).forEach { it.controller?.applyTheme(theme.id) }
            player.displayClientMessage(Component.translatable(answer.messageKey, Component.translatable(theme.name)), true)
        } else {
            player.displayClientMessage(Component.translatable(answer.messageKey), true)
        }
    }

    /** Le joueur a réglé l'arène de la borne. Même portée que pour le thème ; les tailles sont déjà bornées. */
    private fun applyArena(player: ServerPlayer, payload: SetArenaPayload) {
        val terminal = terminalAt(player, payload.terminal)
        when {
            distance(player, payload.terminal) > C_REACH -> player.displayClientMessage(Component.translatable(ThemeRules.Answer.TOO_FAR.messageKey), true)
            terminal == null -> player.displayClientMessage(Component.translatable(ThemeRules.Answer.NO_WALL.messageKey), true)
            else -> {
                terminal.applyArena(payload.arena)
                player.displayClientMessage(Component.translatable("message.${LeagueScreen.MOD_ID}.arena_applied"), true)
            }
        }
    }

    /** Le joueur retire un écran de la borne, depuis l'onglet Écrans. */
    private fun unlink(player: ServerPlayer, payload: UnlinkWallPayload) {
        val terminal = terminalAt(player, payload.terminal)
        when {
            distance(player, payload.terminal) > C_REACH -> player.displayClientMessage(Component.translatable(ThemeRules.Answer.TOO_FAR.messageKey), true)
            terminal != null && terminal.removeWall(payload.wall) ->
                player.displayClientMessage(Component.translatable("message.${LeagueScreen.MOD_ID}.wall_unlinked"), true)
        }
    }
}
