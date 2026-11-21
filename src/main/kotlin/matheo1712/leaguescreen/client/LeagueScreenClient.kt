package matheo1712.leaguescreen.client

import matheo1712.leaguescreen.LeagueScreen
import matheo1712.leaguescreen.client.render.BakedWalls
import matheo1712.leaguescreen.client.render.PokemonModels
import matheo1712.leaguescreen.client.render.PortraitPainter
import matheo1712.leaguescreen.client.theme.ClientThemes
import matheo1712.leaguescreen.network.ThemesPayload
import matheo1712.leaguescreen.network.OpenThemeMenuPayload
import matheo1712.leaguescreen.client.gui.ThemeMenuScreen
import matheo1712.leaguescreen.theme.Couronne
import matheo1712.leaguescreen.client.render.ScreenWallRenderer
import matheo1712.leaguescreen.client.render.ThemeTextures
import matheo1712.leaguescreen.registry.ModBlocks
import matheo1712.leaguescreen.client.arena.ZoneParticles
import matheo1712.leaguescreen.client.arena.ZonePreview
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers
import net.minecraft.resources.ResourceLocation

/** Point d'entrée du client : tout ce qui dessine vit sous ce paquet. */
object LeagueScreenClient : ClientModInitializer {

    override fun onInitializeClient() {
        BlockEntityRenderers.register(ModBlocks.SCREEN_ENTITY, ::ScreenWallRenderer)
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            BakedWalls.clear()
            PokemonModels.clear()
            ClientThemes.clear()
            ThemeTextures.clear()
            ZonePreview.hide()
        }
        ClientPlayNetworking.registerGlobalReceiver(ThemesPayload.TYPE) { payload, _ -> ClientThemes.accept(payload.themes) }
        ClientPlayNetworking.registerGlobalReceiver(OpenThemeMenuPayload.TYPE) { payload, context ->
            val current = ResourceLocation.tryParse(payload.current) ?: Couronne.id
            context.client().setScreen(ThemeMenuScreen(payload.terminal, current, payload.arena, payload.facing, payload.walls))
        }
        linkTrainerSkins()
        ClientTickEvents.END_CLIENT_TICK.register { BakedWalls.flush() }
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val level = client.level ?: return@register
            if (level.gameTime % 10L != 0L) return@register
            val shown = ZonePreview.shown(level.gameTime) ?: return@register
            ZoneParticles.draw(level, shown.terminal, shown.facing, shown.zone, shown.auto)
        }
        LeagueScreen.LOGGER.info("Cobblemon League Screen : client initialized")
    }

    /**
     * Branche les skins de Cobblemon Trainers sur les portraits, si ce mod est installé. Passe par
     * la réflexion pour qu'aucune classe chargée sans lui ne le mentionne.
     */
    private fun linkTrainerSkins() {
        if (!FabricLoader.getInstance().isModLoaded("cobblemon-trainers")) return
        try {
            val find = Class.forName("matheo1712.leaguescreen.client.compat.TrainerSkins")
                .getMethod("find", String::class.java)
            PortraitPainter.trainerSkin = { id -> find.invoke(null, id) as ResourceLocation? }
        } catch (e: Throwable) {
            LeagueScreen.LOGGER.warn("Cobblemon Trainers skins unavailable, using default silhouettes", e)
        }
    }
}
