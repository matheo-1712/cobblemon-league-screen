package matheo1712.leaguescreen.registry

import matheo1712.leaguescreen.LeagueScreen
import matheo1712.leaguescreen.block.ScreenBlock
import matheo1712.leaguescreen.block.ScreenBlockEntity
import matheo1712.leaguescreen.block.TerminalBlock
import matheo1712.leaguescreen.block.TerminalBlockEntity
import matheo1712.leaguescreen.block.TerminalItem
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.BlockItem
import net.minecraft.network.chat.Component
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.MapColor

/**
 * Les blocs du mod, leurs objets et leurs entités de bloc.
 *
 * Tout est créé à l'initialisation de cet objet ; [register] les inscrit dans les registres du
 * jeu et doit être appelé depuis le point d'entrée, avant que les registres ne se figent.
 */
object ModBlocks {

    val SCREEN = ScreenBlock(
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK)
            .strength(1.5f, 6.0f)
            .sound(SoundType.GLASS)
    )

    val TERMINAL = TerminalBlock(
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(2.0f, 6.0f)
            .sound(SoundType.METAL)
    )

    val SCREEN_ITEM = BlockItem(SCREEN, Item.Properties())

    /** La borne ne s'empile pas : chaque objet porte son propre lien. */
    val TERMINAL_ITEM = TerminalItem(TERMINAL, Item.Properties().stacksTo(1))

    // `build` attend le type du correcteur de données, que le jeu laisse à null pour ses propres
    // entités de bloc. Kotlin lit ce paramètre Java comme non nul, d'où un avertissement sans gravité.
    val SCREEN_ENTITY: BlockEntityType<ScreenBlockEntity> =
        BlockEntityType.Builder.of({ pos, state -> ScreenBlockEntity(pos, state) }, SCREEN).build(null)

    val TERMINAL_ENTITY: BlockEntityType<TerminalBlockEntity> =
        BlockEntityType.Builder.of({ pos, state -> TerminalBlockEntity(pos, state) }, TERMINAL).build(null)

    /** L'onglet du mod dans le menu créatif : la borne en icône, puis l'écran et la borne. */
    val TAB: CreativeModeTab = FabricItemGroup.builder()
        .title(Component.translatable("itemGroup.${LeagueScreen.MOD_ID}.main"))
        .icon { ItemStack(TERMINAL_ITEM) }
        .displayItems { _, output ->
            output.accept(SCREEN_ITEM)
            output.accept(TERMINAL_ITEM)
        }
        .build()

    fun register() {
        val screen = LeagueScreen.id("screen")
        val terminal = LeagueScreen.id("terminal")

        Registry.register(BuiltInRegistries.BLOCK, screen, SCREEN)
        Registry.register(BuiltInRegistries.BLOCK, terminal, TERMINAL)
        Registry.register(BuiltInRegistries.ITEM, screen, SCREEN_ITEM)
        Registry.register(BuiltInRegistries.ITEM, terminal, TERMINAL_ITEM)
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, screen, SCREEN_ENTITY)
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, terminal, TERMINAL_ENTITY)

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, LeagueScreen.id("main"), TAB)
    }
}
