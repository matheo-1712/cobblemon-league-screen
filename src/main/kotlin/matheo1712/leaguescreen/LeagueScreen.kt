package matheo1712.leaguescreen

import matheo1712.leaguescreen.battle.BattleTracker
import matheo1712.leaguescreen.registry.ModBlocks
import matheo1712.leaguescreen.registry.ModComponents
import matheo1712.leaguescreen.theme.ThemeRegistry
import matheo1712.leaguescreen.theme.ThemeService
import matheo1712.leaguescreen.debug.DemoCommand
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.resources.ResourceLocation
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** Point d'entrée commun au client et au serveur. */
object LeagueScreen : ModInitializer {

    const val MOD_ID = "cobblemon-league-screen"

    val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

    /** Un identifiant dans l'espace de noms du mod : `id("screen")` donne `cobblemon-league-screen:screen`. */
    fun id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)

    override fun onInitialize() {
        ModComponents.register()
        ModBlocks.register()
        BattleTracker.register()
        ThemeRegistry.register()
        ThemeService.register()
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ -> DemoCommand.register(dispatcher) }
        LOGGER.info("Cobblemon League Screen initialized")
    }
}
