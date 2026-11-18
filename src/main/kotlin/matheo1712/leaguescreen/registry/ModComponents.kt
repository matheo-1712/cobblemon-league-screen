package matheo1712.leaguescreen.registry

import matheo1712.leaguescreen.LeagueScreen
import matheo1712.leaguescreen.arena.ArenaSettings
import net.minecraft.core.GlobalPos
import net.minecraft.core.Registry
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.resources.ResourceLocation

/**
 * Ce que l'objet de la borne emporte quand on la casse, pour le retrouver à la pose : ses écrans,
 * son thème, son arène.
 *
 * `persistent` l'écrit dans la sauvegarde, `networkSynchronized` l'envoie au client (l'infobulle).
 */
object ModComponents {

    /** Les écrans reliés, en positions absolues : l'objet n'a pas de position à lui. */
    val LINKED_WALLS: DataComponentType<List<GlobalPos>> = DataComponentType.builder<List<GlobalPos>>()
        .persistent(GlobalPos.CODEC.listOf())
        .networkSynchronized(GlobalPos.STREAM_CODEC.apply(ByteBufCodecs.list()))
        .build()

    val ARENA: DataComponentType<ArenaSettings> = DataComponentType.builder<ArenaSettings>()
        .persistent(ArenaSettings.CODEC)
        .networkSynchronized(ArenaSettings.STREAM_CODEC)
        .build()

    val THEME: DataComponentType<ResourceLocation> = DataComponentType.builder<ResourceLocation>()
        .persistent(ResourceLocation.CODEC)
        .networkSynchronized(ResourceLocation.STREAM_CODEC)
        .build()

    val LINKED_WALL: DataComponentType<GlobalPos> = DataComponentType.builder<GlobalPos>()
        .persistent(GlobalPos.CODEC)
        .networkSynchronized(GlobalPos.STREAM_CODEC)
        .build()

    fun register() {
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, LeagueScreen.id("linked_walls"), LINKED_WALLS)
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, LeagueScreen.id("arena"), ARENA)
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, LeagueScreen.id("theme"), THEME)
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, LeagueScreen.id("linked_wall"), LINKED_WALL)
    }
}
