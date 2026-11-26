package matheo1712.leaguescreen.client.render

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.client.render.SpriteType
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState
import com.cobblemon.mod.common.client.render.models.blockbench.repository.RenderContext
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository
import com.cobblemon.mod.common.entity.PoseType
import com.cobblemon.mod.common.util.math.fromEulerXYZDegrees
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import matheo1712.leaguescreen.LeagueScreen
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import org.joml.Quaternionf
import org.joml.Vector3f

/**
 * Dessine le modèle 3D d'un Pokémon sur le mur, dans sa pose de profil.
 *
 * On ne passe ni par l'objet « modèle de Pokémon » (son rendu en cadre suppose le demi-tour d'un
 * vrai cadre, et l'aplatit) ni par `drawProfilePokemon` de Cobblemon (faite pour les menus : elle
 * change l'éclairage global et vide le tampon de dessin). On refait ses étapes, mais dans le
 * tampon du mur :
 *
 * 1. mesurer le modèle une fois tourné (une fois par espèce, aspects et angle) ;
 * 2. le mettre à l'échelle pour qu'il tienne dans la case, et le centrer ;
 * 3. le dessiner, aplati en profondeur pour qu'il reste collé au mur.
 */
object PokemonModels {

    /** L'inclinaison des profils de Cobblemon : on voit le Pokémon un peu d'en haut. */
    private const val TILT = 13f

    private data class Key(val species: ResourceLocation, val aspects: Set<String>, val yaw: Float)

    /** La boîte du modèle tourné, dans l'espace où il est mesuré (`y` vers le bas, comme dans un menu). */
    private class Measure(
        val extent: Float, val centerX: Float, val centerY: Float, val minZ: Float,
        val width: Float, val height: Float, val bottom: Float,
    )

    /** Où et à quelle échelle poser un modèle mesuré : l'échelle, puis le point d'origine du modèle. */
    private class Placement(val scale: Float, val x: Float, val y: Float)

    private val states = hashMapOf<Key, FloatingState>()
    private val measures = hashMapOf<Key, Measure>()
    private val failed = hashSetOf<Key>()

    /**
     * Dessine le Pokémon centré en ([cx], [cy]), tenant dans un carré de [size] blocs, entièrement
     * devant [z]. [yaw] le tourne sur lui-même ; [argb] le teinte (gris pour un K.O.) ; [depth] est la
     * part de sa profondeur gardée (un Pokémon en relief, pas une statue qui sort du mur).
     *
     * @return `false` si le modèle n'a pas pu être dessiné : la case se dessine alors comme cachée
     */
    fun draw(
        pose: PoseStack, buffers: MultiBufferSource,
        species: ResourceLocation, aspects: Set<String>,
        cx: Float, cy: Float, z: Float, size: Float, yaw: Float, argb: Int,
        depth: Float, partialTick: Float, light: Int,
    ): Boolean = render(
        pose, buffers, species, aspects, z, yaw, argb, depth, partialTick, light,
        spriteBox = { _ -> SpriteBox(cx, cy, size) },
    ) { m, _ ->
        val scale = size / m.extent
        Placement(scale, cx - m.centerX * scale, cy + m.centerY * scale)
    }

    /**
     * Dessine le Pokémon debout : les pieds sur [bottom], centré sur [cx], sans dépasser [maxWidth]
     * ni [maxHeight]. Avec [unitsPerBlock] (unités du repère par bloc), il prend sa taille du
     * Pokédex (1 m = 1 bloc), pour se tenir à l'échelle d'un dresseur dessiné avec la même valeur ;
     * sinon il remplit la place. Les modèles de Cobblemon ne sont pas à la même échelle d'une
     * espèce à l'autre : c'est la taille du Pokédex qui les compare justement.
     */
    fun drawStanding(
        pose: PoseStack, buffers: MultiBufferSource,
        species: ResourceLocation, aspects: Set<String>,
        cx: Float, bottom: Float, z: Float, maxWidth: Float, maxHeight: Float, unitsPerBlock: Float?,
        yaw: Float, argb: Int, depth: Float, partialTick: Float, light: Int,
    ): Boolean = render(
        pose, buffers, species, aspects, z, yaw, argb, depth, partialTick, light,
        spriteBox = { meters ->
            val side = minOf(maxWidth, maxHeight, unitsPerBlock?.let { it * meters } ?: Float.MAX_VALUE)
            SpriteBox(cx, bottom + side / 2, side)
        },
    ) { m, meters ->
        var scale = minOf(maxWidth / m.width, maxHeight / m.height)
        if (unitsPerBlock != null) scale = minOf(scale, unitsPerBlock * meters / m.height)
        // L'espace mesuré a `y` vers le bas : son bas ([Measure.bottom]) se pose sur [bottom].
        Placement(scale, cx - m.centerX * scale, bottom + m.bottom * scale)
    }

    private class SpriteBox(val cx: Float, val cy: Float, val size: Float)

    private fun render(
        pose: PoseStack, buffers: MultiBufferSource,
        species: ResourceLocation, aspects: Set<String>,
        z: Float, yaw: Float, argb: Int, depth: Float, partialTick: Float, light: Int,
        spriteBox: (meters: Float) -> SpriteBox,
        place: (Measure, meters: Float) -> Placement,
    ): Boolean {
        val key = Key(species, aspects, yaw)
        if (key in failed) return false
        val form = PokemonSpecies.getByIdentifier(species)?.getForm(aspects) ?: return false.also { failed += key }

        // La taille du Pokédex, en décimètres ; jamais nulle, pour ne pas faire disparaître le modèle.
        val meters = (form.height / 10f).takeIf { it > 0f } ?: 1f
        val state = states.getOrPut(key) { FloatingState().also { it.currentAspects = aspects } }
        val sprite = VaryingModelRepository.getSprite(species, state, SpriteType.PROFILE)
        if (sprite != null) {
            val box = spriteBox(meters)
            sprite(pose, buffers, sprite, box.cx, box.cy, z, box.size, argb, light)
            return true
        }

        return try {
            val model = VaryingModelRepository.getPoser(species, state)
            val texture = VaryingModelRepository.getTexture(species, state)
            val context = RenderContext().apply {
                put(RenderContext.TEXTURE, VaryingModelRepository.getTextureNoSubstitute(species, state))
                put(RenderContext.SCALE, form.baseScale)
                put(RenderContext.SPECIES, species)
                put(RenderContext.ASPECTS, aspects)
                put(RenderContext.RENDER_STATE, RenderContext.RenderState.PROFILE)
                put(RenderContext.POSABLE_STATE, state)
                put(RenderContext.DO_QUIRKS, false)
            }
            model.context = context
            state.currentModel = model
            state.setPoseToFirstSuitable(PoseType.PROFILE)
            state.updatePartialTicks(partialTick)
            val rotation = Quaternionf().fromEulerXYZDegrees(Vector3f(TILT, yaw, 0f))

            try {
                model.applyAnimations(null, state, 0f, 0f, 0f, 0f, 0f)
                val measure = measures.getOrPut(key) {
                    val bounds = Bounds()
                    val measuring = PoseStack()
                    measuring.scale(1f, 1f, -1f)
                    measuring.mulPose(rotation)
                    model.rootPart.render(context, measuring, bounds, 0, 0, -1)
                    if (!bounds.extent.isFinite() || bounds.extent <= 0f) {
                        failed += key
                        return false
                    }
                    Measure(bounds.extent, bounds.centerX, bounds.centerY, bounds.minZ, bounds.width, bounds.height, bounds.maxY)
                }
                val placement = place(measure, meters)
                val scale = placement.scale

                pose.pushPose()
                // L'espace mesuré a `y` vers le bas : le retourner revient à mettre (s, -s, -s·D),
                // ce qui garde l'orientation des faces (déterminant positif).
                pose.translate(placement.x, placement.y, z - measure.minZ * scale * depth)
                pose.scale(scale, -scale, -scale * depth)
                pose.mulPose(rotation)
                val buffer = buffers.getBuffer(RenderType.entityCutout(texture))
                model.withLayerContext(buffers, state, VaryingModelRepository.getLayers(species, state)) {
                    model.render(context, pose, buffer, light, OverlayTexture.NO_OVERLAY, argb)
                }
                pose.popPose()
            } finally {
                model.setDefault()
            }
            true
        } catch (e: Exception) {
            failed += key
            LeagueScreen.LOGGER.warn("Modèle de {} impossible à dessiner sur l'écran de ligue", species, e)
            false
        }
    }

    /** Certaines espèces n'ont qu'une image de profil : on la pose à plat, comme une texture. */
    private fun sprite(
        pose: PoseStack, buffers: MultiBufferSource, texture: ResourceLocation,
        cx: Float, cy: Float, z: Float, size: Float, argb: Int, light: Int,
    ) {
        val matrix = pose.last().pose()
        val consumer = buffers.getBuffer(RenderType.text(texture))
        val half = size / 2
        consumer.addVertex(matrix, cx - half, cy - half, z).setColor(argb).setUv(0f, 1f).setLight(light)
        consumer.addVertex(matrix, cx + half, cy - half, z).setColor(argb).setUv(1f, 1f).setLight(light)
        consumer.addVertex(matrix, cx + half, cy + half, z).setColor(argb).setUv(1f, 0f).setLight(light)
        consumer.addVertex(matrix, cx - half, cy + half, z).setColor(argb).setUv(0f, 0f).setLight(light)
    }

    /** À vider quand on change de monde : les modèles et les espèces peuvent changer. */
    fun clear() {
        states.clear()
        measures.clear()
        failed.clear()
    }

    /** Un faux tampon qui ne dessine rien : il retient seulement la boîte des sommets qu'on lui passe. */
    private class Bounds : VertexConsumer {
        private var minX = Float.POSITIVE_INFINITY
        private var minY = Float.POSITIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
            private set
        private var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
            private set

        val centerX get() = (minX + maxX) / 2f
        val centerY get() = (minY + maxY) / 2f
        val extent get() = maxOf(maxX - minX, maxY - minY)
        val width get() = maxX - minX
        val height get() = maxY - minY

        override fun addVertex(x: Float, y: Float, z: Float): VertexConsumer {
            minX = minOf(minX, x)
            minY = minOf(minY, y)
            minZ = minOf(minZ, z)
            maxX = maxOf(maxX, x)
            maxY = maxOf(maxY, y)
            return this
        }

        override fun setColor(red: Int, green: Int, blue: Int, alpha: Int): VertexConsumer = this
        override fun setUv(u: Float, v: Float): VertexConsumer = this
        override fun setUv1(u: Int, v: Int): VertexConsumer = this
        override fun setUv2(u: Int, v: Int): VertexConsumer = this
        override fun setNormal(x: Float, y: Float, z: Float): VertexConsumer = this
    }
}
