package matheo1712.leaguescreen.block

import C_ARENA
import C_LEGACY_WALL
import C_THEME
import C_WALLS
import matheo1712.leaguescreen.arena.ArenaSettings
import matheo1712.leaguescreen.arena.ArenaTerminals
import matheo1712.leaguescreen.registry.ModBlocks
import matheo1712.leaguescreen.registry.ModComponents
import matheo1712.leaguescreen.theme.Couronne
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.GlobalPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponentMap
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.IntArrayTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

/**
 * L'entité de bloc de la borne : **tout ce qu'on y configure** — ses écrans, son thème, son arène.
 *
 * - Les écrans sont retenus **dans le repère de la borne** ([TerminalLinks]) : une arène copiée ou
 *   tournée avec un bloc de structure garde ses liens.
 * - Cassée, la borne emporte tout dans son objet ([collectImplicitComponents], recopié par la table
 *   de butin) ; reposée, elle le retrouve ([applyImplicitComponents]). Les écrans voyagent alors en
 *   positions absolues, puisque l'objet n'a pas de position à lui.
 */
class TerminalBlockEntity(pos: BlockPos, state: BlockState) : BlockEntity(ModBlocks.TERMINAL_ENTITY, pos, state) {

    /** Les écrans, dans le repère de la borne (droite, haut, devant). */
    private var localWalls: List<BlockPos> = emptyList()

    var arena: ArenaSettings = ArenaSettings.DEFAULT
        private set

    /** Le thème que la borne donne à ses écrans. */
    var theme: ResourceLocation = Couronne.id
        private set

    val facing: Direction
        get() = blockState.getOptionalValue(TerminalBlock.FACING).orElse(Direction.NORTH)

    /** Les écrans dans le monde. Ils sont dans la dimension de la borne. */
    fun walls(): List<GlobalPos> {
        val dimension = level?.dimension() ?: return emptyList()
        return localWalls.map { GlobalPos.of(dimension, TerminalLinks.toWorld(blockPos, facing, it)) }
    }

    fun removeWall(world: BlockPos): Boolean {
        val local = TerminalLinks.toLocal(blockPos, facing, world)
        if (local !in localWalls) return false
        localWalls = localWalls - local
        setChanged()
        return true
    }

    fun applyArena(settings: ArenaSettings) {
        arena = settings
        setChanged()
        announce()
    }

    fun applyTheme(id: ResourceLocation) {
        theme = id
        setChanged()
    }

    /** Tient [ArenaTerminals] à jour : seules les bornes chargées et actives y figurent. */
    private fun announce() {
        val level = level ?: return
        if (level.isClientSide) return
        val here = GlobalPos.of(level.dimension(), blockPos)
        if (arena.auto && !isRemoved) ArenaTerminals.add(here) else ArenaTerminals.remove(here)
    }

    override fun setLevel(level: Level) {
        super.setLevel(level)
        announce()
    }

    override fun setRemoved() {
        super.setRemoved()
        announce()
    }

    override fun clearRemoved() {
        super.clearRemoved()
        announce()
    }

    // ---- Objet → bloc, bloc → objet ----

    override fun applyImplicitComponents(componentInput: DataComponentInput) {
        super.applyImplicitComponents(componentInput)
        val absolute = (componentInput.get(ModComponents.LINKED_WALLS) ?: emptyList()) +
                listOfNotNull(componentInput.get(ModComponents.LINKED_WALL))
        if (absolute.isNotEmpty()) {
            localWalls = absolute.map { TerminalLinks.toLocal(blockPos, facing, it.pos()) }.distinct().take(TerminalLinks.MAX_WALLS)
        }
        componentInput.get(ModComponents.ARENA)?.let { arena = it }
        componentInput.get(ModComponents.THEME)?.let { theme = it }
        announce()
    }

    override fun collectImplicitComponents(components: DataComponentMap.Builder) {
        super.collectImplicitComponents(components)
        val walls = walls()
        if (walls.isNotEmpty()) components.set(ModComponents.LINKED_WALLS, walls)
        if (arena != ArenaSettings.DEFAULT) components.set(ModComponents.ARENA, arena)
        if (theme != Couronne.id) components.set(ModComponents.THEME, theme)
    }

    /** Ce que portent les composants de l'objet n'a pas à être recopié dans ses données de bloc. */
    @Suppress("OVERRIDE_DEPRECATION")
    override fun removeComponentsFromTag(tag: CompoundTag) {
        tag.remove(C_WALLS)
        tag.remove(C_ARENA)
        tag.remove(C_THEME)
        tag.remove(C_LEGACY_WALL)
    }

    // ---- Sauvegarde (monde et structures) ----

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        val list = ListTag()
        localWalls.forEach { list.add(IntArrayTag(intArrayOf(it.x, it.y, it.z))) }
        tag.put(C_WALLS, list)
        tag.put(C_ARENA, arena.toTag())
        tag.putString(C_THEME, theme.toString())
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        localWalls = tag.getList(C_WALLS, Tag.TAG_INT_ARRAY.toInt())
            .mapNotNull { (it as? IntArrayTag)?.asIntArray?.takeIf { a -> a.size == 3 }?.let { a -> BlockPos(a[0], a[1], a[2]) } }
            .take(TerminalLinks.MAX_WALLS)
        // L'ancien format : un seul écran, en position absolue.
        tag.get(C_LEGACY_WALL)?.let { GlobalPos.CODEC.parse(NbtOps.INSTANCE, it).result().orElse(null) }?.let { legacy ->
            val local = TerminalLinks.toLocal(blockPos, facing, legacy.pos())
            if (local !in localWalls) localWalls = localWalls + local
        }
        arena = if (tag.contains(C_ARENA)) ArenaSettings.fromTag(tag.getCompound(C_ARENA)) else ArenaSettings.DEFAULT
        theme = ResourceLocation.tryParse(tag.getString(C_THEME)).takeIf { tag.contains(C_THEME) } ?: Couronne.id
        announce()
    }
}
