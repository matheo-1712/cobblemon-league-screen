package matheo1712.leaguescreen.client.render

import matheo1712.leaguescreen.LeagueScreen
import matheo1712.leaguescreen.board.Portrait
import matheo1712.leaguescreen.board.PortraitKind
import matheo1712.leaguescreen.screen.Frame
import matheo1712.leaguescreen.theme.ThemeGeometry
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.resources.ResourceLocation
import java.util.UUID
import kotlin.math.min

/** Le cadre d'un dresseur : un rectangle de couleur, un fond, et son skin à plat dedans. */
object PortraitPainter {

    private val SILHOUETTE = LeagueScreen.id("textures/board/silhouette.png")

    /**
     * Un skin à dessiner : sa texture 64 × 64. Fourni par Cobblemon Trainers pour un dresseur
     * dont le skin vient du serveur, `null` s'il n'est pas (encore) connu.
     */
    var trainerSkin: (String) -> ResourceLocation? = { null }

    /**
     * @param box le cadre, en unités du repère
     * @param border l'épaisseur du cadre, en unités du repère
     * @param frame la couleur du cadre ; la silhouette par défaut la reprend
     * @param fill la couleur du fond
     */
    fun paint(canvas: WallCanvas, portrait: Portrait, box: Frame, border: Float, frame: Int, fill: Int, z: Float) {
        val inner = Frame(box.x + border, box.y + border, box.width - 2 * border, box.height - 2 * border)
        if (inner.width <= 0f || inner.height <= 0f) {
            canvas.fill(box.x, box.y, box.x + box.width, box.y + box.height, frame, z)
            return
        }
        // Le cadre en quatre bandes autour du fond : rien ne se superpose, rien ne scintille.
        val right = box.x + box.width
        val top = box.y + box.height
        canvas.fill(box.x, box.y, right, inner.y, frame, z)
        canvas.fill(box.x, inner.y + inner.height, right, top, frame, z)
        canvas.fill(box.x, inner.y, inner.x, inner.y + inner.height, frame, z)
        canvas.fill(inner.x + inner.width, inner.y, right, inner.y + inner.height, frame, z)
        canvas.fill(inner.x, inner.y, inner.x + inner.width, inner.y + inner.height, fill, z)

        val cx = inner.x + inner.width / 2
        val cy = inner.y + inner.height / 2
        val side = min(inner.width, inner.height)
        val skin = skinOf(portrait)
        if (skin == null) {
            canvas.texture(SILHOUETTE, cx, cy, side * 0.9f, side * 0.9f, Colors.withAlpha(frame, 0.85f), z + ThemeGeometry.SUB_STEP)
        } else {
            bust(canvas, skin, cx, cy, side, z + ThemeGeometry.SUB_STEP)
        }
    }

    /**
     * Le skin à dessiner dans ce cadre, ou `null` pour la silhouette. Il peut changer avec le temps
     * (un skin de joueur qui finit de se télécharger) : [BakedWalls] s'en sert pour savoir quand
     * redessiner le tableau.
     */
    fun skinOf(portrait: Portrait): ResourceLocation? = when (portrait.kind) {
        PortraitKind.PLAYER -> playerSkin(portrait.id)
        PortraitKind.TEXTURE -> ResourceLocation.tryParse(portrait.id)?.takeIf(ThemeTextures::exists)
        PortraitKind.TRAINER -> trainerSkin(portrait.id)
        PortraitKind.NONE -> null
    }

    /** Le skin d'un joueur que le client connaît ; le skin par défaut sinon. */
    private fun playerSkin(id: String): ResourceLocation? {
        val uuid = runCatching { UUID.fromString(id) }.getOrNull() ?: return null
        val info = Minecraft.getInstance().connection?.getPlayerInfo(uuid)
        return (info?.skin ?: DefaultPlayerSkin.get(uuid)).texture()
    }

    /**
     * Tête et buste vus de face, dans un carré de 16 × 16 « pixels de skin » : la tête en haut,
     * le torse et le haut des bras en dessous, coupés au bord du cadre.
     */
    private fun bust(canvas: WallCanvas, skin: ResourceLocation, cx: Float, cy: Float, side: Float, z: Float) {
        val px = side / 16f
        val left = cx - side / 2
        val top = cy + side / 2

        /** Dessine la zone [u]..[u]+[w], [v]..[v]+[h] du skin (en pixels de 64) au rectangle (gx, gy) de la grille. */
        fun part(gx: Float, gy: Float, w: Float, h: Float, u: Float, v: Float, layer: Int) {
            canvas.texture(
                skin,
                left + (gx + w / 2) * px, top - (gy + h / 2) * px, w * px, h * px,
                // Quatre couches (torse, veste, tête, cheveux) : 4 × SUB_STEP reste sous le calque suivant.
                Colors.WHITE, z + layer * ThemeGeometry.SUB_STEP,
                u / 64f, v / 64f, (u + w) / 64f, (v + h) / 64f,
            )
        }

        part(4f, 9f, 8f, 7f, 20f, 20f, 0)   // torse
        part(0f, 9f, 4f, 7f, 44f, 20f, 0)   // bras droit (à gauche vu de face)
        part(12f, 9f, 4f, 7f, 36f, 52f, 0)  // bras gauche
        part(4f, 9f, 8f, 7f, 20f, 36f, 1)   // veste
        part(4f, 1f, 8f, 8f, 8f, 8f, 2)     // tête
        part(4f, 1f, 8f, 8f, 40f, 8f, 3)    // chapeau, cheveux
    }
}
