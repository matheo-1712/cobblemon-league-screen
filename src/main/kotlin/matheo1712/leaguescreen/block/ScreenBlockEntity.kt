package matheo1712.leaguescreen.block

import C_BOARD
import C_THEME
import C_WALL
import matheo1712.leaguescreen.board.BoardState
import matheo1712.leaguescreen.registry.ModBlocks
import matheo1712.leaguescreen.screen.WallRect
import matheo1712.leaguescreen.theme.ThemeRegistry
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

/**
 * L'entité de bloc d'un bloc d'écran.
 *
 * Chaque bloc d'écran en a une, mais une seule par mur porte quelque chose : celle du **bloc de
 * contrôle** (le coin bas gauche), dont [wall] n'est pas nul. C'est elle qui garde l'état affiché
 * ([board]) et c'est son rendu qui dessine tout le mur.
 */
class ScreenBlockEntity(pos: BlockPos, state: BlockState) : BlockEntity(ModBlocks.SCREEN_ENTITY, pos, state) {

    /** Le rectangle du mur si ce bloc en est le bloc de contrôle, `null` sinon. */
    var wall: WallRect? = null
        private set

    var board: BoardState = BoardState.OFF
        private set

    /**
     * Le thème du mur. Sauvegardé, contrairement au tableau : c'est un réglage du mur, pas l'état
     * d'un combat. Un thème qui n'existe plus est dessiné avec le thème par défaut, sans effacer ce choix.
     */
    var theme: ResourceLocation = ThemeRegistry.defaultId
        private set

    /** Change le thème et prévient les clients : le mur se redessine aussitôt, combat en cours compris. */
    fun applyTheme(id: ResourceLocation) {
        if (id == theme) return
        theme = id
        sync()
    }

    /** Fait de ce bloc le bloc de contrôle de [rect], ou lui retire ce rôle. Un mur qui change s'éteint. */
    fun assign(rect: WallRect?) {
        if (wall == rect) return
        wall = rect
        board = BoardState.OFF
        sync()
    }

    /** Change ce que le mur affiche et prévient les clients ; ne fait rien si rien ne change. */
    fun show(next: BoardState) {
        if (next == board) return
        board = next
        sync()
    }

    private fun sync() {
        setChanged()
        level?.sendBlockUpdated(blockPos, blockState, blockState, Block.UPDATE_CLIENTS)
    }

    /** Sur le disque : le mur et son thème. Un combat ne survit pas à un redémarrage, le tableau non plus. */
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        wall?.let { tag.put(C_WALL, it.toSizeTag()) }
        tag.putString(C_THEME, theme.toString())
    }

    /** Lit aussi bien la sauvegarde (sans tableau) que le paquet de mise à jour (avec). */
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        // L'origine du mur est ce bloc, son orientation celle du bloc : un mur copié ou tourné reste juste.
        val facing = blockState.getOptionalValue(ScreenBlock.FACING).orElse(null)
        wall = if (tag.contains(C_WALL) && facing != null) WallRect.fromSizeTag(tag.getCompound(C_WALL), blockPos, facing) else null
        board = if (tag.contains(C_BOARD)) BoardState.fromTag(tag.getCompound(C_BOARD)) else BoardState.OFF
        theme = ResourceLocation.tryParse(tag.getString(C_THEME)).takeIf { tag.contains(C_THEME) } ?: ThemeRegistry.defaultId
    }

    /** Vers le client : le mur **et** le tableau. Le tag n'est jamais vide, sans quoi le client l'ignorerait. */
    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = CompoundTag()
        wall?.let { tag.put(C_WALL, it.toSizeTag()) }
        tag.put(C_BOARD, board.toTag())
        tag.putString(C_THEME, theme.toString())
        return tag
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener> = ClientboundBlockEntityDataPacket.create(this)

}
