package matheo1712.leaguescreen.theme

import COLOR_WHITE
import matheo1712.leaguescreen.board.SlotState
import net.minecraft.resources.ResourceLocation

/** Une boîte en pixels de la maquette : coin haut gauche, largeur, hauteur. */
data class Box(val x: Float, val y: Float, val w: Float, val h: Float)

/**
 * Une couleur : fixe (ARGB), ou un nom de la palette du thème (`@left`, `@accent`, `@side`…),
 * éventuellement assombri : `@side:0.5` multiplie le rouge, le vert et le bleu par 0,5.
 */
sealed interface ColorRef {
    data class Fixed(val argb: Int) : ColorRef
    data class Named(val name: String, val shade: Float = 1f) : ColorRef
}

/** Le camp d'un calque. [WINNER] n'existe que dans la scène de victoire. */
enum class SideRef { LEFT, RIGHT, WINNER }

enum class Align { LEFT, CENTER, RIGHT }

/** Une image d'une case, centrée sur la case et agrandie de [scale]. */
data class SlotImage(val image: ResourceLocation, val color: ColorRef, val alpha: Float, val scale: Float)

/** Comment un Pokémon se place dans sa case. */
data class PokemonStyle(val fit: Float, val yaw: Float, val depth: Float, val fainted: ColorRef)

/** Un élément d'une scène. Les calques sont dessinés dans l'ordre : le premier est derrière. */
sealed interface Layer {
    val box: Box
    val alpha: Float
}

data class FillLayer(override val box: Box, override val alpha: Float, val color: ColorRef) : Layer

data class ImageLayer(override val box: Box, override val alpha: Float, val image: ResourceLocation, val color: ColorRef) : Layer

data class TextLayer(override val box: Box, override val alpha: Float, val value: String, val color: ColorRef, val align: Align) : Layer

data class NameLayer(override val box: Box, override val alpha: Float, val side: SideRef, val color: ColorRef, val align: Align) : Layer

/**
 * [border] en pixels de maquette, `null` pour 6 % du côté.
 *
 * [figure] dessine le dresseur en entier, en 3D, debout sur le bas du cadre et tourné de [yaw]
 * degrés (vers le centre pour le camp de gauche, en miroir pour celui de droite), la tête
 * inclinée de [pitch] degrés (positif : il baisse les yeux) ; sinon, son buste à plat.
 */
data class PortraitLayer(
    override val box: Box, override val alpha: Float,
    val side: SideRef, val frame: ColorRef, val border: Float?, val fill: ColorRef,
    val figure: Boolean = false, val yaw: Float = 0f, val pitch: Float = 0f,
) : Layer

/** La case n° [index] (1 à 6) d'un camp. [style] remplace, pour cette case seulement, les styles du thème. */
data class SlotLayer(
    override val box: Box, override val alpha: Float,
    val side: SideRef, val index: Int, val style: Map<SlotState, List<SlotImage>>?,
) : Layer

/**
 * Un thème d'écran, tel que lu dans `data/<ns>/leaguescreen/themes/<chemin>.json`.
 *
 * [warnings] liste ce qui a été ignoré sans rendre le thème inutilisable (un type de calque
 * inconnu, un camp `winner` dans la scène en direct).
 */
data class Theme(
    val id: ResourceLocation,
    val name: String,
    val canvasWidth: Float,
    val canvasHeight: Float,
    val background: ColorRef,
    /** Le fond de tout le mur pendant l'écran de victoire ; `@side` y désigne le vainqueur. */
    val victoryBackground: ColorRef,
    val colors: Map<String, Int>,
    val pokemon: PokemonStyle,
    val slots: Map<SlotState, List<SlotImage>>,
    val live: List<Layer>,
    val victory: List<Layer>,
    val warnings: List<String> = emptyList(),
) {
    val ratio: Float get() = canvasWidth / canvasHeight

    /**
     * La couleur ARGB d'une référence. `@side` prend la couleur de [side] ([SideRef.WINNER] doit
     * avoir été ramené à `LEFT` ou `RIGHT` par l'appelant). Un nom inconnu donne du blanc : un
     * thème mal écrit se voit, il ne plante pas.
     */
    fun resolve(color: ColorRef, side: SideRef?): Int = when (color) {
        is ColorRef.Fixed -> color.argb
        is ColorRef.Named -> shade(
            when (color.name) {
                "side" -> when (side) {
                    SideRef.LEFT -> colors["left"]
                    SideRef.RIGHT -> colors["right"]
                    else -> null
                }
                else -> colors[color.name]
            } ?: COLOR_WHITE,
            color.shade,
        )
    }

    companion object {
        /** Garde l'opacité, multiplie le rouge, le vert et le bleu par [factor]. */
        fun shade(argb: Int, factor: Float): Int {
            if (factor == 1f) return argb
            val r = ((argb shr 16 and 0xFF) * factor).toInt().coerceIn(0, 255)
            val g = ((argb shr 8 and 0xFF) * factor).toInt().coerceIn(0, 255)
            val b = ((argb and 0xFF) * factor).toInt().coerceIn(0, 255)
            return (argb and 0xFF000000.toInt()) or (r shl 16) or (g shl 8) or b
        }
    }
}
