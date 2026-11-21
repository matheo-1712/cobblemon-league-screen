package matheo1712.leaguescreen.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import matheo1712.leaguescreen.block.ScreenBlockEntity
import matheo1712.leaguescreen.board.Phase
import matheo1712.leaguescreen.client.theme.ClientThemes
import matheo1712.leaguescreen.theme.ThemeGeometry
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider

/**
 * Le rendu d'un mur entier, attaché à son bloc de contrôle (le coin bas gauche vu de face).
 *
 * Le tableau est dessiné une fois dans une texture par [BakedWalls], seulement quand il change ;
 * ici, on ne pose plus que cette texture sur la face du mur.
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
        if (board.phase == Phase.OFF) {
            BakedWalls.forget(blockEntity)
            return
        }
        val theme = ClientThemes.forWall(blockEntity.theme)
        val texture = BakedWalls.textureFor(blockEntity, wall.width, wall.height, board, theme) ?: return

        poseStack.pushPose()
        // Tourne le bloc pour que +z sorte de la dalle et +x aille vers la droite du spectateur,
        // puis va au coin bas gauche de la face avant.
        poseStack.translate(0.5, 0.0, 0.5)
        poseStack.mulPose(Axis.YP.rotationDegrees(-wall.facing.toYRot()))
        poseStack.translate(-0.5, 0.0, 0.5)
        val matrix = poseStack.last().pose()
        val consumer = bufferSource.getBuffer(RenderType.text(texture))
        val w = wall.width.toFloat()
        val h = wall.height.toFloat()
        val z = ThemeGeometry.BACKGROUND_Z
        // La texture a son origine en bas à gauche, comme le repère du mur.
        consumer.addVertex(matrix, 0f, 0f, z).setColor(Colors.WHITE).setUv(0f, 0f).setLight(WallCanvas.LIGHT)
        consumer.addVertex(matrix, w, 0f, z).setColor(Colors.WHITE).setUv(1f, 0f).setLight(WallCanvas.LIGHT)
        consumer.addVertex(matrix, w, h, z).setColor(Colors.WHITE).setUv(1f, 1f).setLight(WallCanvas.LIGHT)
        consumer.addVertex(matrix, 0f, h, z).setColor(Colors.WHITE).setUv(0f, 1f).setLight(WallCanvas.LIGHT)
        poseStack.popPose()
    }

    /** Le mur dépasse son bloc de contrôle : il doit être dessiné même quand ce bloc est hors champ. */
    override fun shouldRenderOffScreen(blockEntity: ScreenBlockEntity): Boolean = true

    override fun getViewDistance(): Int = 96
}
