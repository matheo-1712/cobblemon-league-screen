package matheo1712.leaguescreen.client.render

import matheo1712.leaguescreen.LeagueScreen
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation

/**
 * Les textures nommées par les thèmes existent-elles chez ce client ?
 *
 * Une image d'un thème vit dans un pack que ce joueur n'a peut-être pas. Minecraft dessinerait
 * alors son damier violet et noir ; on préfère ne rien dessiner, et le dire une fois dans le journal.
 */
object ThemeTextures {

    private val known = hashMapOf<ResourceLocation, Boolean>()

    fun exists(texture: ResourceLocation): Boolean = known.getOrPut(texture) {
        val present = Minecraft.getInstance().resourceManager.getResource(texture).isPresent
        if (!present) LeagueScreen.LOGGER.warn("Image de thème introuvable : {} (le calque ne sera pas dessiné)", texture)
        present
    }

    /** À vider quand les ressources changent (changement de monde, F3+T). */
    fun clear() = known.clear()
}
