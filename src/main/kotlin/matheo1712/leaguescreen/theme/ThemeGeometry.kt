package matheo1712.leaguescreen.theme

import matheo1712.leaguescreen.screen.Frame

/**
 * Passer de la maquette au mur. Pur : des nombres en entrée, des nombres en sortie.
 *
 * Sur le mur, l'origine est le coin bas gauche vu de face et `y` monte ; dans la maquette,
 * l'origine est le coin haut gauche et `y` descend, comme dans Canva.
 */
object ThemeGeometry {

    /** La profondeur du fond, devant la dalle et derrière tous les calques. */
    const val BACKGROUND_Z = 0.005f

    /** L'écart entre deux calques : assez pour que deux couches ne scintillent pas vues de loin. */
    const val LAYER_STEP = 0.01f

    /** L'écart entre deux éléments d'un même calque (cadre et skin d'un portrait, images d'une case). */
    const val SUB_STEP = 0.002f

    /** Le plus grand cadre de proportion [ratio] (largeur / hauteur) qui tient dans le mur, centré. */
    fun frame(wallWidth: Float, wallHeight: Float, ratio: Float): Frame {
        val heightLimited = wallWidth / wallHeight > ratio
        return if (heightLimited) {
            val width = wallHeight * ratio
            Frame((wallWidth - width) / 2f, 0f, width, wallHeight)
        } else {
            val height = wallWidth / ratio
            Frame(0f, (wallHeight - height) / 2f, wallWidth, height)
        }
    }

    /** Une boîte de la maquette ([canvasWidth] × [canvasHeight] pixels), placée dans le cadre du mur. */
    fun toWall(box: Box, frame: Frame, canvasWidth: Float, canvasHeight: Float): Frame {
        val sx = frame.width / canvasWidth
        val sy = frame.height / canvasHeight
        return Frame(
            x = frame.x + box.x * sx,
            y = frame.y + (canvasHeight - box.y - box.h) * sy,
            width = box.w * sx,
            height = box.h * sy,
        )
    }

    /** La profondeur du calque n° [index] : l'ordre écrit dans le thème est l'ordre de profondeur. */
    fun layerZ(index: Int): Float = 2 * BACKGROUND_Z + index * LAYER_STEP
}
