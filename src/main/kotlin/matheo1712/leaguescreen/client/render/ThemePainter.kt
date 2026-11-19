package matheo1712.leaguescreen.client.render

import com.mojang.blaze3d.vertex.PoseStack
import matheo1712.leaguescreen.board.BoardState
import matheo1712.leaguescreen.board.DisplayName
import matheo1712.leaguescreen.board.Phase
import matheo1712.leaguescreen.board.SideView
import matheo1712.leaguescreen.board.SlotState
import matheo1712.leaguescreen.screen.Frame
import matheo1712.leaguescreen.theme.FillLayer
import matheo1712.leaguescreen.theme.ImageLayer
import matheo1712.leaguescreen.theme.Layer
import matheo1712.leaguescreen.theme.NameLayer
import matheo1712.leaguescreen.theme.PortraitLayer
import matheo1712.leaguescreen.theme.SideRef
import matheo1712.leaguescreen.theme.SlotLayer
import matheo1712.leaguescreen.theme.TextLayer
import matheo1712.leaguescreen.theme.Theme
import matheo1712.leaguescreen.theme.ThemeGeometry
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import kotlin.math.min

/**
 * Dessine un [BoardState] selon un [Theme] : le fond, puis les calques de la scène, dans l'ordre.
 *
 * Le repère est celui de [WallCanvas] : origine en bas à gauche, `y` vers le haut. [width] et
 * [height] sont la taille de la surface dans ce repère : des blocs sur un mur, des pixels dans
 * l'aperçu du menu.
 */
object ThemePainter {

    fun paint(
        pose: PoseStack, buffers: MultiBufferSource,
        width: Float, height: Float,
        board: BoardState, theme: Theme, partialTick: Float,
    ) {
        if (board.phase == Phase.OFF || board.sides.size != 2) return
        val canvas = WallCanvas(pose, buffers)
        val frame = ThemeGeometry.frame(width, height, theme.ratio)

        val victory = board.phase == Phase.VICTORY
        val winner = when (board.winner) {
            0 -> SideRef.LEFT
            1 -> SideRef.RIGHT
            else -> null
        }
        // Une victoire sans vainqueur ne devrait pas arriver ; on ne dessine alors que le fond.
        val background = if (victory) theme.resolve(theme.victoryBackground, winner) else theme.resolve(theme.background, null)
        canvas.fill(0f, 0f, width, height, background, ThemeGeometry.BACKGROUND_Z)
        if (victory && winner == null) return

        val scene = Scene(canvas, theme, board, frame, winner, partialTick)
        (if (victory) theme.victory else theme.live).forEachIndexed { index, layer ->
            scene.draw(layer, ThemeGeometry.layerZ(index))
        }
    }

    private class Scene(
        val canvas: WallCanvas,
        val theme: Theme,
        val board: BoardState,
        val frame: Frame,
        val winner: SideRef?,
        val partialTick: Float,
    ) {
        /** Unité du repère par pixel de maquette. */
        val unit = frame.width / theme.canvasWidth

        fun draw(layer: Layer, z: Float) {
            val box = ThemeGeometry.toWall(layer.box, frame, theme.canvasWidth, theme.canvasHeight)
            val cx = box.x + box.width / 2
            val cy = box.y + box.height / 2
            fun color(ref: matheo1712.leaguescreen.theme.ColorRef, side: SideRef?) = Colors.fade(theme.resolve(ref, side), layer.alpha)

            when (layer) {
                is FillLayer -> canvas.fill(box.x, box.y, box.x + box.width, box.y + box.height, color(layer.color, null), z)
                is ImageLayer -> if (ThemeTextures.exists(layer.image)) {
                    canvas.texture(layer.image, cx, cy, box.width, box.height, color(layer.color, null), z)
                }
                is TextLayer -> canvas.text(
                    Component.translatable(layer.value), box.x, box.width, cy, box.height,
                    color(layer.color, null), layer.align, z,
                )
                is NameLayer -> {
                    val (side, view) = sideOf(layer.side) ?: return
                    canvas.text(nameOf(view.name), box.x, box.width, cy, box.height, color(layer.color, side), layer.align, z)
                }
                is PortraitLayer -> {
                    val (side, view) = sideOf(layer.side) ?: return
                    val border = layer.border?.let { it * unit } ?: (min(box.width, box.height) * 0.06f)
                    PortraitPainter.paint(
                        canvas, view.portrait, box, border,
                        color(layer.frame, side), color(layer.fill, side), z,
                    )
                }
                is SlotLayer -> slot(layer, box, z)
            }
        }

        /** Le camp désigné par un calque, `winner` ramené à gauche ou droite. */
        fun sideOf(ref: SideRef): Pair<SideRef, SideView>? {
            val side = if (ref == SideRef.WINNER) winner ?: return null else ref
            return side to board.sides[if (side == SideRef.LEFT) 0 else 1]
        }

        fun slot(layer: SlotLayer, box: Frame, z: Float) {
            val (side, view) = sideOf(layer.side) ?: return
            val slot = view.slots.getOrNull(layer.index - 1) ?: return
            val cx = box.x + box.width / 2
            val cy = box.y + box.height / 2
            val d = min(box.width, box.height)

            // Le Pokémon d'abord : s'il ne peut pas être dessiné, la case se dessine comme cachée.
            val species = slot.species?.takeIf { slot.state.revealed }?.let(ResourceLocation::tryParse)
            val pokemon = theme.pokemon
            val drawn = species != null && PokemonModels.draw(
                canvas.pose, canvas.buffers, species, slot.aspects,
                cx, cy, z + 4 * ThemeGeometry.SUB_STEP, d * pokemon.fit,
                // Les deux camps se tournent vers le centre.
                if (side == SideRef.LEFT) pokemon.yaw else -pokemon.yaw,
                if (slot.state == SlotState.FAINTED) Colors.fade(theme.resolve(pokemon.fainted, side), layer.alpha) else Colors.fade(Colors.WHITE, layer.alpha),
                pokemon.depth, partialTick, WallCanvas.LIGHT,
            )
            val state = if (slot.state.revealed && !drawn) SlotState.HIDDEN else slot.state
            val images = (layer.style ?: theme.slots)[state] ?: emptyList()
            images.forEachIndexed { i, image ->
                if (!ThemeTextures.exists(image.image)) return@forEachIndexed
                canvas.texture(
                    image.image, cx, cy, box.width * image.scale, box.height * image.scale,
                    Colors.fade(theme.resolve(image.color, side), image.alpha * layer.alpha),
                    z + minOf(i, 3) * ThemeGeometry.SUB_STEP,
                )
            }
        }

        fun nameOf(name: DisplayName): Component =
            if (name.translate) Component.translatable(name.text) else Component.literal(name.text)
    }
}
