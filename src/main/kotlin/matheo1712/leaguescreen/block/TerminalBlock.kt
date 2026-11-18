package matheo1712.leaguescreen.block

import matheo1712.leaguescreen.battle.BattleTracker
import matheo1712.leaguescreen.theme.ThemeService
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.phys.BlockHitResult

/**
 * La borne d'enregistrement : un dresseur en combat clique dessus pour que ses écrans suivent son
 * combat ; accroupi, mains vides, on ouvre son menu.
 *
 * Elle a une orientation (elle regarde le joueur qui la pose) : ses écrans et sa zone d'arène sont
 * retenus dans son repère, si bien qu'une arène copiée et tournée avec un bloc de structure les garde.
 */
class TerminalBlock(properties: Properties) : Block(properties), EntityBlock {

    init {
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState =
        defaultBlockState().setValue(FACING, context.horizontalDirection.opposite)

    override fun rotate(state: BlockState, rotation: Rotation): BlockState =
        state.setValue(FACING, rotation.rotate(state.getValue(FACING)))

    override fun mirror(state: BlockState, mirror: Mirror): BlockState =
        state.rotate(mirror.getRotation(state.getValue(FACING)))

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = TerminalBlockEntity(pos, state)

    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult,
    ): InteractionResult {
        if (level.isClientSide) return InteractionResult.SUCCESS
        val serverPlayer = player as? ServerPlayer ?: return InteractionResult.PASS
        val terminal = level.getBlockEntity(pos) as? TerminalBlockEntity ?: return InteractionResult.PASS
        // Accroupi (le jeu n'appelle cette méthode accroupi que les mains vides) : le menu de la borne.
        if (serverPlayer.isShiftKeyDown) ThemeService.openMenu(serverPlayer, terminal) else BattleTracker.onTerminalUsed(serverPlayer, terminal)
        return InteractionResult.CONSUME
    }

    companion object {
        val FACING: DirectionProperty = BlockStateProperties.HORIZONTAL_FACING
    }
}
