package matheo1712.leaguescreen.debug

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import matheo1712.leaguescreen.board.BoardState
import matheo1712.leaguescreen.board.DemoBoards
import matheo1712.leaguescreen.board.Phase
import matheo1712.leaguescreen.screen.ScreenWalls
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult

/**
 * `/leaguescreen demo [victory|off]` : pose un tableau de démonstration sur le mur regardé, sans
 * combat. Sert à vérifier le rendu (placement, couleurs, états des cases). Réservé aux opérateurs.
 */
object DemoCommand {

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("leaguescreen")
                .requires { it.hasPermission(2) }
                .then(
                    Commands.literal("demo")
                        .executes { show(it, Phase.LIVE) }
                        .then(Commands.literal("victory").executes { show(it, Phase.VICTORY) })
                        .then(Commands.literal("off").executes { show(it, Phase.OFF) })
                )
        )
    }

    private fun show(context: CommandContext<CommandSourceStack>, phase: Phase): Int {
        val player = context.source.playerOrException
        val hit = player.pick(32.0, 0f, false)
        val controller = (hit as? BlockHitResult)?.takeIf { it.type == HitResult.Type.BLOCK }
            ?.let { ScreenWalls.controllerAt(player.serverLevel(), it.blockPos) }
        if (controller == null) {
            context.source.sendFailure(Component.literal("Regarde un écran de ligue valide (un rectangle plein)."))
            return 0
        }
        controller.show(board(player, phase))
        context.source.sendSuccess({ Component.literal("Tableau de démonstration : ${phase.name}") }, false)
        return 1
    }

    /** Le tableau de démonstration, au nom du joueur qui tape la commande. */
    fun board(player: ServerPlayer, phase: Phase): BoardState =
        DemoBoards.board(player.gameProfile.name, player.uuid.toString(), phase)
}
