package matheo1712.leaguescreen.block

import matheo1712.leaguescreen.registry.ModComponents
import matheo1712.leaguescreen.screen.ScreenWalls
import net.minecraft.ChatFormatting
import net.minecraft.core.GlobalPos
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.block.Block

/**
 * L'objet de la borne. Il porte ses écrans (jusqu'à [TerminalLinks.MAX_WALLS]), son thème et son
 * arène : borne en main, clic droit sur un écran pour l'ajouter ; une borne cassée emporte tout.
 */
class TerminalItem(block: Block, properties: Properties) : BlockItem(block, properties) {

    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level
        val pos = context.clickedPos
        // En s'accroupissant, on pose la borne contre l'écran au lieu de la relier.
        if (context.isSecondaryUseActive || level.getBlockState(pos).block !is ScreenBlock) {
            return super.useOn(context)
        }
        if (!level.isClientSide) {
            val stack = context.itemInHand
            // Le lien vise le bloc de contrôle du mur : deux clics sur le même mur ne comptent qu'une fois.
            val target = ScreenWalls.wallAt(level, pos)?.controller ?: pos
            val wall = GlobalPos.of(level.dimension(), target.immutable())
            val walls = wallsOf(stack)
            val message = when {
                wall in walls -> Component.translatable("message.cobblemon-league-screen.already_linked")
                walls.size >= TerminalLinks.MAX_WALLS ->
                    Component.translatable("message.cobblemon-league-screen.terminal_full", TerminalLinks.MAX_WALLS)
                else -> {
                    stack.set(ModComponents.LINKED_WALLS, walls + wall)
                    stack.remove(ModComponents.LINKED_WALL)
                    Component.translatable("message.cobblemon-league-screen.linked", walls.size + 1, TerminalLinks.MAX_WALLS)
                }
            }
            context.player?.displayClientMessage(message, true)
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag,
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
        val walls = wallsOf(stack)
        if (walls.isEmpty()) {
            tooltipComponents.add(Component.translatable("tooltip.cobblemon-league-screen.unlinked").withStyle(ChatFormatting.GRAY))
        } else {
            tooltipComponents.add(Component.translatable("tooltip.cobblemon-league-screen.linked", walls.size).withStyle(ChatFormatting.GRAY))
            walls.forEach {
                val pos = it.pos()
                tooltipComponents.add(Component.translatable("tooltip.cobblemon-league-screen.wall", pos.x, pos.y, pos.z).withStyle(ChatFormatting.DARK_GRAY))
            }
        }
        if (stack.get(ModComponents.ARENA)?.auto == true) {
            tooltipComponents.add(Component.translatable("tooltip.cobblemon-league-screen.arena").withStyle(ChatFormatting.GOLD))
        }
    }

    private companion object {
        /** Les écrans de l'objet, l'ancien lien unique compris. */
        fun wallsOf(stack: ItemStack): List<GlobalPos> =
            (stack.get(ModComponents.LINKED_WALLS) ?: emptyList()) + listOfNotNull(stack.get(ModComponents.LINKED_WALL))
    }
}
