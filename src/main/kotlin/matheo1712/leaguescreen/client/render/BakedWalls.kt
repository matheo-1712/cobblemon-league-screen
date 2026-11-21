package matheo1712.leaguescreen.client.render

import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexSorting
import matheo1712.leaguescreen.LeagueScreen
import matheo1712.leaguescreen.block.ScreenBlockEntity
import matheo1712.leaguescreen.board.BoardState
import matheo1712.leaguescreen.board.Portrait
import matheo1712.leaguescreen.theme.Theme
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30
import java.util.IdentityHashMap

/**
 * Le tableau d'un mur, dessiné **une fois** dans une texture, puis posé sur le mur à chaque image.
 *
 * Avant, chaque image redessinait tout : fond, calques, textes, skins, et surtout les modèles 3D
 * des Pokémon, des milliers de sommets par mur. Maintenant, tout cela n'est dessiné que quand ce
 * que le mur montre change (une case révélée, un K.O., un thème, un skin enfin chargé) ; le reste
 * du temps, le mur ne coûte qu'un rectangle texturé.
 *
 * Le dessin se fait avec le même [ThemePainter], dans le même repère en blocs que sur le mur :
 * seule la projection change (orthogonale, sur la face du mur). Le résultat est donc le même
 * tableau, à plat, et les Pokémon y sont figés dans leur pose de profil.
 */
object BakedWalls {

    /** La résolution de la texture : des pixels par bloc de mur. */
    private const val PIXELS_PER_BLOCK = 128

    /** Le plus grand côté d'une texture : un mur de 16 blocs donne 2048 pixels. */
    private const val MAX_SIZE = 2048

    /** Un mur qu'on n'a plus dessiné depuis ce temps (hors de vue, déchargé, cassé) libère sa texture. */
    private const val IDLE_NANOS = 10_000_000_000L

    /**
     * Tout ce dont dépend l'image : si rien de cela n'a changé, la texture est encore juste.
     * Le thème est comparé par identité : [matheo1712.leaguescreen.client.theme.ClientThemes] en
     * garde la même instance tant qu'il ne change pas.
     */
    private class Key(val board: BoardState, val theme: Theme, val width: Int, val height: Int, val skins: List<ResourceLocation?>) {
        fun same(other: Key?): Boolean = other != null && other.theme === theme && other.board == board &&
            other.width == width && other.height == height && other.skins == skins
    }

    private class Baked(val target: TextureTarget, val location: ResourceLocation) {
        /** Ce que la texture montre ; nul tant qu'elle n'a jamais été dessinée. */
        var key: Key? = null
        /** Ce qu'elle doit montrer au prochain [flush], si ce n'est pas déjà [key]. */
        var pending: Key? = null
        var lastUsed = 0L
    }

    /** Fait voir la texture d'un [TextureTarget] au gestionnaire de textures, sans lui en laisser la charge. */
    private class TargetTexture(private val target: TextureTarget) : AbstractTexture() {
        override fun getId(): Int = target.colorTextureId
        override fun load(resourceManager: ResourceManager) {}
        // La texture appartient à la cible : c'est elle qu'on détruit, pas ce prête-nom.
        override fun releaseId() {}
        override fun close() {}
    }

    private val baked = IdentityHashMap<ScreenBlockEntity, Baked>()
    private val buffer by lazy { ByteBufferBuilder(1 shl 18) }
    private var counter = 0
    private var lastSweep = 0L

    /**
     * La texture du tableau de ce mur, ou nul si elle n'a pas encore été dessinée. À appeler
     * pendant le rendu du monde, sur le fil de rendu.
     *
     * Si le tableau a changé, il n'est pas redessiné ici mais au prochain [flush], hors du rendu du
     * monde : pendant ce rendu, un pack de shaders (Iris) remplace les shaders du jeu par les siens,
     * qui n'écrivent pas une image ordinaire, et repasse même une fois pour les ombres. En
     * attendant, le mur garde l'image d'avant.
     */
    fun textureFor(entity: ScreenBlockEntity, width: Int, height: Int, board: BoardState, theme: Theme): ResourceLocation? {
        val now = System.nanoTime()
        sweep(now)

        val key = Key(board, theme, width, height, board.sides.map { PortraitPainter.skinOf(it.portrait) })
        val (pixelsWide, pixelsHigh) = pixels(width, height)

        var entry = baked[entity]
        if (entry != null && (entry.target.width != pixelsWide || entry.target.height != pixelsHigh)) {
            release(entry)
            baked.remove(entity)
            entry = null
        }
        if (entry == null) {
            val target = TextureTarget(pixelsWide, pixelsHigh, true, Minecraft.ON_OSX)
            target.setFilterMode(GL11.GL_LINEAR)
            val location = LeagueScreen.id("baked_wall/${counter++}")
            Minecraft.getInstance().textureManager.register(location, TargetTexture(target))
            entry = Baked(target, location)
            baked[entity] = entry
        }
        entry.lastUsed = now
        entry.pending = if (key.same(entry.key)) null else key
        return if (entry.key != null) entry.location else null
    }

    /**
     * Redessine les tableaux qui ont changé. À appeler sur le fil de rendu, hors du rendu du monde
     * (à chaque tick du client) : les shaders du jeu y sont ceux d'origine, même avec Iris.
     */
    fun flush() {
        for (entry in baked.values) {
            val key = entry.pending ?: continue
            entry.pending = null
            bake(entry.target, key.width.toFloat(), key.height.toFloat(), key.board, key.theme)
            entry.key = key
        }
    }

    /** Le mur s'est éteint : sa texture n'a plus lieu d'être. */
    fun forget(entity: ScreenBlockEntity) {
        baked.remove(entity)?.let(::release)
    }

    /** En quittant un monde : tout est libéré. */
    fun clear() {
        baked.values.forEach(::release)
        baked.clear()
    }

    private fun pixels(width: Int, height: Int): Pair<Int, Int> {
        val scale = minOf(PIXELS_PER_BLOCK.toFloat(), MAX_SIZE.toFloat() / maxOf(width, height))
        return (width * scale).toInt().coerceAtLeast(1) to (height * scale).toInt().coerceAtLeast(1)
    }

    /** Dessine le tableau dans [target], puis rend la main au monde comme on l'a trouvé. */
    private fun bake(target: TextureTarget, width: Float, height: Float, board: BoardState, theme: Theme) {
        val buffers = MultiBufferSource.immediate(buffer)
        // Les tampons d'image liés, et la zone d'affichage, pour les remettre ensuite.
        val previousDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)
        val previousRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING)
        val previousViewport = IntArray(4).also { GL11.glGetIntegerv(GL11.GL_VIEWPORT, it) }

        // Deux précautions pour Iris (pack de shaders), sans quoi le tableau partait sur l'écran du
        // jeu au lieu de sa texture (mur vide ou abîmé) :
        // - `bindWrite` lui dit que l'écran principal n'est plus la cible : il ne remplace les
        //   shaders du jeu par ceux du pack que quand il croit dessiner à l'écran. Il le croit de
        //   nouveau au prochain rendu du monde.
        // - Puis on lie notre tampon directement : Iris garde en mémoire le tampon qu'il croit lié
        //   et saute les liaisons qu'il juge inutiles ; or lui et le jeu en lient aussi sans le
        //   prévenir, et sa mémoire peut dire « déjà lié » à tort.
        // Le tableau n'utilise aussi que des rendus qui restent sur ce tampon (voir
        // [WallCanvas.fill] et [FigurePainter]).
        target.bindWrite(true)
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, target.frameBufferId)
        GlStateManager._viewport(0, 0, target.width, target.height)
        RenderSystem.clearColor(0f, 0f, 0f, 0f)
        RenderSystem.clearDepth(1.0)
        RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX)

        RenderSystem.backupProjectionMatrix()
        // Le repère du mur : x vers la droite, y vers le haut, z vers le spectateur, en blocs.
        RenderSystem.setProjectionMatrix(Matrix4f().setOrtho(0f, width, 0f, height, -16f, 16f), VertexSorting.ORTHOGRAPHIC_Z)
        val modelView = RenderSystem.getModelViewStack()
        modelView.pushMatrix()
        modelView.identity()
        RenderSystem.applyModelViewMatrix()
        // Pas de brouillard dans la texture : il s'applique déjà au mur, là où il est dans le monde.
        val fogStart = RenderSystem.getShaderFogStart()
        RenderSystem.setShaderFogStart(Float.MAX_VALUE)

        try {
            ThemePainter.paint(PoseStack(), buffers, width, height, board, theme, 0f)
            buffers.endBatch()
        } finally {
            RenderSystem.setShaderFogStart(fogStart)
            modelView.popMatrix()
            RenderSystem.applyModelViewMatrix()
            RenderSystem.restoreProjectionMatrix()
            // Remis comme trouvés, toujours sans passer par la mémoire d'Iris : elle redevient
            // juste (ou aussi fausse qu'avant, mais pas par notre faute).
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDraw)
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousRead)
            GlStateManager._viewport(previousViewport[0], previousViewport[1], previousViewport[2], previousViewport[3])
        }
    }

    /** Libère les murs qu'on ne dessine plus. Vérifié une fois par seconde au plus. */
    private fun sweep(now: Long) {
        if (now - lastSweep < 1_000_000_000L) return
        lastSweep = now
        val idle = baked.entries.filter { now - it.value.lastUsed > IDLE_NANOS || it.key.isRemoved }
        idle.forEach {
            release(it.value)
            baked.remove(it.key)
        }
    }

    private fun release(entry: Baked) {
        Minecraft.getInstance().textureManager.release(entry.location)
        entry.target.destroyBuffers()
    }
}
