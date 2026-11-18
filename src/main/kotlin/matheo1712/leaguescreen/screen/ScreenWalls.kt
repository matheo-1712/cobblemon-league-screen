package matheo1712.leaguescreen.screen

import matheo1712.leaguescreen.battle.BattleTracker
import matheo1712.leaguescreen.block.ScreenBlock
import matheo1712.leaguescreen.block.LinkRules
import matheo1712.leaguescreen.block.ScreenBlockEntity
import matheo1712.leaguescreen.block.TerminalBlockEntity
import matheo1712.leaguescreen.network.WallStatus
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.GlobalPos
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level

/** Le lien entre [WallScanner], qui ne connaît que des positions, et le monde. */
object ScreenWalls {

    /** Jusqu'où l'on suit un ensemble de blocs pour y retirer d'anciens blocs de contrôle. */
    private const val CLEAR_LIMIT = 1024

    /** L'orientation du bloc d'écran à cette position, ou `null` s'il n'y en a pas (ou si le tronçon n'est pas chargé). */
    fun facingAt(level: Level, pos: BlockPos): Direction? {
        if (!level.isLoaded(pos)) return null
        val state = level.getBlockState(pos)
        return if (state.block is ScreenBlock) state.getValue(ScreenBlock.FACING) else null
    }

    /** Le mur auquel appartient le bloc à cette position, s'il en forme un valide. */
    fun wallAt(level: Level, pos: BlockPos): WallRect? {
        val facing = facingAt(level, pos) ?: return null
        return WallScanner.scan(pos, facing) { facingAt(level, it) == facing }
    }

    /** L'entité de bloc qui porte l'état du mur passant par cette position. */
    fun controllerAt(level: Level, pos: BlockPos): ScreenBlockEntity? {
        val rect = wallAt(level, pos) ?: return null
        return (level.getBlockEntity(rect.controller) as? ScreenBlockEntity)?.takeIf { it.wall == rect }
    }

    /** Un écran relié à une borne : le lien retenu, le bloc de contrôle s'il est prêt, et son état. */
    data class LinkedWall(val link: GlobalPos, val controller: ScreenBlockEntity?, val status: WallStatus) {
        /** Le mur, s'il est prêt à afficher. */
        val rect: WallRect? get() = controller?.wall
    }

    /** Les écrans de la borne, dans l'ordre où ils ont été reliés, avec leur état. */
    fun linkedWalls(server: MinecraftServer, terminal: TerminalBlockEntity): List<LinkedWall> {
        val here = GlobalPos.of(terminal.level?.dimension() ?: return emptyList(), terminal.blockPos)
        return terminal.walls().map { link ->
            if (!LinkRules.inRange(here, link)) return@map LinkedWall(link, null, WallStatus.TOO_FAR)
            val level = server.getLevel(link.dimension())
            if (level == null || !level.isLoaded(link.pos())) return@map LinkedWall(link, null, WallStatus.UNLOADED)
            val controller = controllerAt(level, link.pos())
            LinkedWall(link, controller, if (controller != null) WallStatus.READY else WallStatus.MISSING)
        }
    }

    /**
     * À appeler quand un bloc d'écran vient d'être posé ou retiré en [pos] : recalcule les murs
     * touchés. Dans chaque ensemble de blocs reliés, un seul garde un rectangle (le bloc de
     * contrôle d'un mur valide) ; tous les autres le perdent.
     */
    fun refresh(level: ServerLevel, pos: BlockPos) {
        // Le bloc posé ou retiré a pu être un bloc de contrôle : son combat est oublié.
        BattleTracker.wallChanged(level, GlobalPos.of(level.dimension(), pos.immutable()))
        val seen = hashSetOf<BlockPos>()
        for (candidate in listOf(pos) + Direction.entries.map { pos.relative(it) }) {
            if (candidate in seen) continue
            val facing = facingAt(level, candidate) ?: continue
            val isScreen = { other: BlockPos -> facingAt(level, other) == facing }

            val component = WallScanner.connected(candidate, facing, CLEAR_LIMIT, isScreen)
            seen += component
            val rect = WallScanner.scan(candidate, facing, isScreen)
            // Le thème suit le mur : si le bloc de contrôle change (mur agrandi), le nouveau le reprend.
            val inherited = component.firstNotNullOfOrNull { block ->
                (level.getBlockEntity(block) as? ScreenBlockEntity)?.takeIf { it.wall != null }?.theme
            }
            for (block in component) {
                val entity = level.getBlockEntity(block) as? ScreenBlockEntity ?: continue
                val next = rect?.takeIf { it.controller == block }
                if (entity.wall != null && entity.wall != next) {
                    BattleTracker.wallChanged(level, GlobalPos.of(level.dimension(), block))
                }
                entity.assign(next)
                if (next != null && inherited != null) entity.applyTheme(inherited)
            }
        }
    }
}
