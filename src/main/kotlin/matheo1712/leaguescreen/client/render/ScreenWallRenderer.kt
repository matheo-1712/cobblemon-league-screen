package matheo1712.leaguescreen.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import matheo1712.leaguescreen.block.ScreenBlockEntity
import matheo1712.leaguescreen.board.Phase
import matheo1712.leaguescreen.client.theme.ClientThemes
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider

/**
 * Le rendu d'un mur entier, attaché à son bloc de contrôle (le coin bas gauche vu de face).
 *
 * Il place le repère du [WallCanvas] puis laisse [ThemePainter] dessiner le thème du mur.
 */
class ScreenWallRenderer(context: BlockEntityRendererProvider.Context) : BlockEntityRenderer<ScreenBlockEntity> {

    override fun render(
        blockEntity: ScreenBlockEntity,
        partialTick: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int,
    ) {
        val wall = blockEntity.wall ?: return
        val board = blockEntity.board
        if (board.phase == Phase.OFF) return

        poseStack.pushPose()
        // Tourne le bloc pour que +z sorte de la dalle et +x aille vers la droite du spectateur,
        // puis va au coin bas gauche de la face avant.
        poseStack.translate(0.5, 0.0, 0.5)
        poseStack.mulPose(Axis.YP.rotationDegrees(-wall.facing.toYRot()))
        poseStack.translate(-0.5, 0.0, 0.5)
        val theme = ClientThemes.forWall(blockEntity.theme)
        ThemePainter.paint(poseStack, bufferSource, wall.width.toFloat(), wall.height.toFloat(), board, theme, partialTick)
        poseStack.popPose()
    }

    /** Le mur dépasse son bloc de contrôle : il doit être dessiné même quand ce bloc est hors champ. */
    override fun shouldRenderOffScreen(blockEntity: ScreenBlockEntity): Boolean = true

    override fun getViewDistance(): Int = 96
}
