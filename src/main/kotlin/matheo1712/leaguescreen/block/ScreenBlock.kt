package matheo1712.leaguescreen.block

import matheo1712.leaguescreen.screen.ScreenWalls
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
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

/**
 * Un bloc d'écran : un bloc plein dont une face horizontale est la dalle.
 *
 * Seul, il ne fait rien. Des blocs voisins de même orientation, dans le même plan, forment un mur
 * dès qu'ils dessinent un rectangle plein ; c'est [ScreenWalls] qui le recalcule à chaque pose et
 * à chaque retrait.
 */
class ScreenBlock(properties: Properties) : Block(properties), EntityBlock {

    init {
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
    }

    /** La dalle regarde le joueur qui pose le bloc. */
    override fun getStateForPlacement(context: BlockPlaceContext): BlockState =
        defaultBlockState().setValue(FACING, context.horizontalDirection.opposite)

    override fun rotate(state: BlockState, rotation: Rotation): BlockState =
        state.setValue(FACING, rotation.rotate(state.getValue(FACING)))

    override fun mirror(state: BlockState, mirror: Mirror): BlockState =
        state.rotate(mirror.getRotation(state.getValue(FACING)))

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = ScreenBlockEntity(pos, state)

    /**
     * Le jeu appelle `onPlace` avant d'avoir créé l'entité du bloc posé : le mur est recalculé au
     * tick suivant ([tick]), quand elle existe à coup sûr. Le délai d'un tick est invisible.
     */
    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, movedByPiston: Boolean) {
        super.onPlace(state, level, pos, oldState, movedByPiston)
        if (level is ServerLevel && state != oldState) level.scheduleTick(pos, this, 1)
    }

    override fun tick(state: BlockState, level: ServerLevel, pos: BlockPos, random: RandomSource) {
        ScreenWalls.refresh(level, pos)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, movedByPiston: Boolean) {
        // `super` retire l'entité de bloc quand le bloc disparaît vraiment.
        super.onRemove(state, level, pos, newState, movedByPiston)
        if (level is ServerLevel && state != newState) ScreenWalls.refresh(level, pos)
    }

    companion object {
        val FACING: DirectionProperty = BlockStateProperties.HORIZONTAL_FACING
    }
}
