package matheo1712.leaguescreen.client.render

import com.mojang.math.Axis
import matheo1712.leaguescreen.screen.Frame
import net.minecraft.client.Minecraft
import net.minecraft.client.model.PlayerModel
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import kotlin.math.min

/**
 * Un dresseur debout, en 3D : le modèle de joueur de Minecraft habillé de son skin, comme les
 * figures de l'intro de Cobblemon Trainers.
 *
 * On ne passe pas par l'entité : celui qui regarde le mur peut être loin du combat, et l'entité
 * du dresseur n'est alors pas chargée chez lui. Le skin suffit (celui d'un joueur, ou celui
 * qu'un dresseur de Cobblemon Trainers affiche). Le modèle reste dans sa pose de repos, bras le
 * long du corps : sur le mur, tout est figé de toute façon (voir [BakedWalls]).
 */
object FigurePainter {

    /** La hauteur du modèle, chapeau compris, en blocs : 32 pixels de skin, plus la seconde couche. */
    private const val HEIGHT = 2.05f

    /** Sa largeur, bras et manches compris, en blocs. */
    private const val WIDTH = 1.1f

    /** La part de sa profondeur gardée : un dresseur en relief, pas une statue qui sort du mur. */
    private const val DEPTH = 0.15f

    /** Dans l'espace du modèle, les pieds sont à 1,5 bloc sous son origine (comme dans le rendu des entités). */
    private const val FEET = 1.501f

    private val wide by lazy { adult(PlayerModel(Minecraft.getInstance().entityModels.bakeLayer(ModelLayers.PLAYER), false)) }
    private val slim by lazy { adult(PlayerModel(Minecraft.getInstance().entityModels.bakeLayer(ModelLayers.PLAYER_SLIM), true)) }

    /**
     * Un modèle tout neuf se croit celui d'un bébé (`young` vaut vrai par défaut ; c'est le rendu
     * des entités qui le remet à jour à chaque image). Il dessinerait alors un corps réduit de
     * moitié sous une grosse tête : le dresseur paraîtrait tout petit dans son cadre.
     */
    private fun adult(model: PlayerModel<LivingEntity>): PlayerModel<LivingEntity> = model.also { it.young = false }

    /**
     * Dessine le dresseur dans [box] : centré, les pieds sur le bas du cadre, aussi grand que le
     * cadre le permet. [yaw] le tourne sur lui-même, en degrés ; [pitch] incline sa tête
     * (positif : il baisse les yeux), pour que deux dresseurs placés à des hauteurs différentes
     * se regardent.
     */
    fun draw(canvas: WallCanvas, skin: ResourceLocation, slimArms: Boolean, box: Frame, yaw: Float, pitch: Float, z: Float) {
        val scale = min(box.height / HEIGHT, box.width / WIDTH)
        if (scale <= 0f) return
        val model = if (slimArms) slim else wide

        val pose = canvas.pose
        pose.pushPose()
        // Les pieds au bas du cadre ; le modèle entier devant [z], malgré sa profondeur.
        pose.translate(box.x + box.width / 2, box.y, z + 0.5f * scale * DEPTH)
        pose.scale(scale, scale, scale * DEPTH)
        pose.mulPose(Axis.YP.rotationDegrees(yaw))
        // Le passage au repère des modèles d'entités : leur `y` va vers le bas et leur visage vers
        // -z. Le jeu fait un demi-tour puis retourne x et y ; ensemble, cela revient à retourner
        // y et z. Deux retournements : les faces gardent leur orientation.
        pose.scale(1f, -1f, -1f)
        pose.translate(0f, -FEET, 0f)
        val buffer = canvas.buffers.getBuffer(RenderType.entityTranslucent(skin))
        // Le modèle est partagé : la tête est inclinée le temps de ce dessin, puis remise droite.
        val tilt = Math.toRadians(pitch.toDouble()).toFloat()
        model.head.xRot = tilt
        model.hat.xRot = tilt
        try {
            model.renderToBuffer(pose, buffer, WallCanvas.LIGHT, OverlayTexture.NO_OVERLAY, Colors.WHITE)
        } finally {
            model.head.xRot = 0f
            model.hat.xRot = 0f
        }
        pose.popPose()
    }
}
