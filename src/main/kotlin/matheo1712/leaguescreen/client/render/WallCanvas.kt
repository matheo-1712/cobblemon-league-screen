package matheo1712.leaguescreen.client.render

import com.mojang.blaze3d.vertex.PoseStack
import matheo1712.leaguescreen.LeagueScreen
import matheo1712.leaguescreen.theme.Align
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

/**
 * Une surface de dessin posée sur la face d'un mur.
 *
 * Le repère de [pose] est déjà placé par l'appelant : origine au coin bas gauche, `x` vers la
 * droite, `y` vers le haut, `z` vers le spectateur. Sur un mur, l'unité est le bloc ; dans
 * l'aperçu du menu, c'est le pixel d'écran. Tout est dessiné à pleine luminosité : l'écran se
 * lit de nuit.
 */
class WallCanvas(val pose: PoseStack, val buffers: MultiBufferSource) {

    /**
     * Un rectangle de couleur unie : une texture blanche teintée, dessinée comme les images.
     * Pas `RenderType.textBackground()` : avec Iris (pack de shaders), ce rendu-là quittait la
     * texture du mur pour l'écran du jeu, et le fond manquait.
     */
    fun fill(x0: Float, y0: Float, x1: Float, y1: Float, argb: Int, z: Float) {
        val matrix = pose.last().pose()
        val consumer = buffers.getBuffer(RenderType.text(WHITE))
        consumer.addVertex(matrix, x0, y0, z).setColor(argb).setUv(0f, 0f).setLight(LIGHT)
        consumer.addVertex(matrix, x1, y0, z).setColor(argb).setUv(1f, 0f).setLight(LIGHT)
        consumer.addVertex(matrix, x1, y1, z).setColor(argb).setUv(1f, 1f).setLight(LIGHT)
        consumer.addVertex(matrix, x0, y1, z).setColor(argb).setUv(0f, 1f).setLight(LIGHT)
    }

    /**
     * Une texture (ou une partie : [u0]..[u1], [v0]..[v1] en fractions) centrée en ([cx], [cy]),
     * teintée par [argb]. Les images nommées par un thème sont vérifiées avant (voir [ThemeTextures]) ;
     * les skins, eux, sont des textures créées à la volée, pas des ressources : on ne les vérifie pas.
     */
    fun texture(
        texture: ResourceLocation,
        cx: Float, cy: Float, width: Float, height: Float,
        argb: Int, z: Float,
        u0: Float = 0f, v0: Float = 0f, u1: Float = 1f, v1: Float = 1f,
    ) {
        val matrix = pose.last().pose()
        val consumer = buffers.getBuffer(RenderType.text(texture))
        val x0 = cx - width / 2
        val x1 = cx + width / 2
        val y0 = cy - height / 2
        val y1 = cy + height / 2
        // Le haut de l'image (v0) va en haut du quad.
        consumer.addVertex(matrix, x0, y0, z).setColor(argb).setUv(u0, v1).setLight(LIGHT)
        consumer.addVertex(matrix, x1, y0, z).setColor(argb).setUv(u1, v1).setLight(LIGHT)
        consumer.addVertex(matrix, x1, y1, z).setColor(argb).setUv(u1, v0).setLight(LIGHT)
        consumer.addVertex(matrix, x0, y1, z).setColor(argb).setUv(u0, v0).setLight(LIGHT)
    }

    /**
     * Un texte haut de [height], dans la boîte ([x], [y]) – ([x] + [width]), centré verticalement en
     * [cy], aligné selon [align], réduit s'il dépasse [width].
     */
    fun text(text: Component, x: Float, width: Float, cy: Float, height: Float, argb: Int, align: Align, z: Float) {
        val font = Minecraft.getInstance().font
        val textWidth = font.width(text).coerceAtLeast(1)
        var scale = height / font.lineHeight
        if (textWidth * scale > width) scale = width / textWidth
        val drawn = textWidth * scale
        val left = when (align) {
            Align.LEFT -> x
            Align.CENTER -> x + (width - drawn) / 2
            Align.RIGHT -> x + width - drawn
        }
        pose.pushPose()
        pose.translate(left, cy, z)
        // La police dessine `y` vers le bas : on retourne l'axe.
        pose.scale(scale, -scale, scale)
        font.drawInBatch(
            text, 0f, -font.lineHeight / 2f + 1f, argb, false,
            pose.last().pose(), buffers, Font.DisplayMode.POLYGON_OFFSET, 0, LIGHT,
        )
        pose.popPose()
    }

    companion object {
        val LIGHT: Int = LightTexture.FULL_BRIGHT
        private val WHITE: ResourceLocation = LeagueScreen.id("textures/misc/white.png")
    }
}
