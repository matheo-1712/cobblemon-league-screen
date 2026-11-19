package matheo1712.leaguescreen.client.gui

import matheo1712.leaguescreen.LeagueScreen
import matheo1712.leaguescreen.arena.ArenaSettings
import matheo1712.leaguescreen.arena.ArenaZone
import matheo1712.leaguescreen.board.DemoBoards
import matheo1712.leaguescreen.board.Phase
import matheo1712.leaguescreen.client.arena.ZoneParticles
import matheo1712.leaguescreen.client.arena.ZonePreview
import matheo1712.leaguescreen.client.render.ThemePainter
import matheo1712.leaguescreen.client.theme.ClientThemes
import matheo1712.leaguescreen.network.SetArenaPayload
import matheo1712.leaguescreen.network.SetThemePayload
import matheo1712.leaguescreen.network.UnlinkWallPayload
import matheo1712.leaguescreen.network.WallEntry
import matheo1712.leaguescreen.network.WallStatus
import matheo1712.leaguescreen.theme.Theme
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractSliderButton
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Checkbox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import kotlin.math.max
import kotlin.math.min

/**
 * Le menu de la borne, ouvert par accroupi + clic droit, mains vides. Deux onglets :
 *
 * - **Thème** : la liste des thèmes à gauche, l'aperçu du thème choisi à droite (le tableau de
 *   démonstration, dessiné par le vrai rendu, Pokémon compris), et le bouton qui l'applique au mur.
 * - **Arène** : l'interrupteur « lancer l'écran tout seul » et la taille de la zone, orientée comme
 *   la borne (largeur de gauche à droite, profondeur d'avant en arrière). Le menu laisse
 *   alors voir le monde, où les bords de la zone se dessinent en particules ; « Afficher la zone »
 *   les laisse visibles deux minutes après la fermeture du menu.
 *
 * - **Écrans** : les écrans reliés à la borne, leur état, et un bouton pour en retirer un.
 *
 * Le serveur revérifie tout à la réception : ce menu ne fait que proposer.
 */
class ThemeMenuScreen(
    private val terminal: BlockPos,
    private val current: ResourceLocation,
    arena: ArenaSettings,
    private val facing: Direction,
    walls: List<WallEntry>,
) : Screen(Component.translatable("screen.${LeagueScreen.MOD_ID}.themes")) {

    private enum class Tab { THEME, ARENA, WALLS }

    private var tab = Tab.THEME

    // ---- Onglet Thème ----
    private val themes: List<Theme> = ClientThemes.list()
    private var selected: ResourceLocation = themes.firstOrNull { it.id == current }?.id ?: themes.first().id
    private var phase = Phase.LIVE
    private var page = 0

    // ---- Onglet Arène ----
    private var auto = arena.auto
    // ---- Onglet Écrans ----
    private val walls = walls.toMutableList()

    private var zoneWidth = arena.zone.width
    private var zoneLength = arena.zone.length
    private var zoneHeight = arena.zone.height
    private var ticks = 0

    private val left = 16
    private val listWidth = 150
    private val top = 36
    private val rowHeight = 22
    private val panelWidth = 220

    private val rowsPerPage: Int get() = max(1, (height - top - 60) / rowHeight)
    private val pageCount: Int get() = max(1, (themes.size + rowsPerPage - 1) / rowsPerPage)

    private fun key(name: String) = "screen.${LeagueScreen.MOD_ID}.$name"

    override fun init() {
        addRenderableWidget(Button.builder(Component.translatable(key("tab_theme"))) { tab = Tab.THEME; rebuildWidgets() }
            .bounds(left, 8, 80, 20).build()).active = tab != Tab.THEME
        addRenderableWidget(Button.builder(Component.translatable(key("tab_arena"))) { tab = Tab.ARENA; rebuildWidgets() }
            .bounds(left + 84, 8, 80, 20).build()).active = tab != Tab.ARENA
        addRenderableWidget(Button.builder(Component.translatable(key("tab_walls"))) { tab = Tab.WALLS; rebuildWidgets() }
            .bounds(left + 168, 8, 80, 20).build()).active = tab != Tab.WALLS
        when (tab) {
            Tab.THEME -> initTheme()
            Tab.ARENA -> initArena()
            Tab.WALLS -> initWalls()
        }
    }

    // ------------------------------------------------------------------------------------------
    // Thème
    // ------------------------------------------------------------------------------------------

    private fun initTheme() {
        page = page.coerceIn(0, pageCount - 1)
        val start = page * rowsPerPage
        themes.subList(start, min(themes.size, start + rowsPerPage)).forEachIndexed { row, theme ->
            addRenderableWidget(
                Button.builder(label(theme)) {
                    selected = theme.id
                    rebuildWidgets()
                }.bounds(left, top + row * rowHeight, listWidth, 20).build(),
            )
        }
        if (pageCount > 1) {
            val y = top + rowsPerPage * rowHeight + 4
            addRenderableWidget(Button.builder(Component.literal("◀")) { page--; rebuildWidgets() }
                .bounds(left, y, 36, 20).build()).active = page > 0
            addRenderableWidget(Button.builder(Component.literal("▶")) { page++; rebuildWidgets() }
                .bounds(left + listWidth - 36, y, 36, 20).build()).active = page < pageCount - 1
        }

        val preview = previewBox()
        val buttonsY = preview.y + preview.height + 8
        addRenderableWidget(
            Button.builder(Component.translatable(if (phase == Phase.LIVE) key("live") else key("victory"))) {
                phase = if (phase == Phase.LIVE) Phase.VICTORY else Phase.LIVE
                rebuildWidgets()
            }.bounds(preview.x, buttonsY, 120, 20).build(),
        )
        addRenderableWidget(
            Button.builder(Component.translatable(key("apply"))) {
                ClientPlayNetworking.send(SetThemePayload(terminal, selected.toString()))
                onClose()
            }.bounds(preview.x + preview.width - 120, buttonsY, 120, 20).build(),
        )
    }

    private fun label(theme: Theme): Component {
        val name = Component.translatable(theme.name)
        val text = if (theme.id == selected) Component.literal("› ").append(name) else name
        return if (theme.id == current) text.copy().append(" ").append(Component.translatable(key("current"))) else text
    }

    private data class Box(val x: Int, val y: Int, val width: Int, val height: Int)

    /** L'aperçu à la proportion du thème choisi, dans la place laissée à droite de la liste. */
    private fun previewBox(): Box {
        val theme = ClientThemes.forWall(selected)
        val x0 = left + listWidth + 16
        val maxWidth = width - x0 - 16
        val maxHeight = height - top - 44
        var w = maxWidth
        var h = (w / theme.ratio).toInt()
        if (h > maxHeight) {
            h = maxHeight
            w = (h * theme.ratio).toInt()
        }
        return Box(x0 + (maxWidth - w) / 2, top, max(w, 1), max(h, 1))
    }

    // ------------------------------------------------------------------------------------------
    // Arène
    // ------------------------------------------------------------------------------------------

    private fun initArena() {
        addRenderableWidget(
            Checkbox.builder(Component.translatable(key("arena_auto")), font)
                .pos(left + 6, top + 4)
                .selected(auto)
                .onValueChange { _, value -> auto = value; previewChanged() }
                .build(),
        )
        addRenderableWidget(SizeSlider(top + 30, ArenaZone.MIN_SIDE, ArenaZone.MAX_SIDE, zoneWidth, "arena_width") { zoneWidth = it; previewChanged() })
        addRenderableWidget(SizeSlider(top + 56, ArenaZone.MIN_SIDE, ArenaZone.MAX_SIDE, zoneLength, "arena_length") { zoneLength = it; previewChanged() })
        addRenderableWidget(SizeSlider(top + 82, ArenaZone.MIN_HEIGHT, ArenaZone.MAX_HEIGHT, zoneHeight, "arena_height") { zoneHeight = it; previewChanged() })
        // Les particules restent deux minutes après la fermeture du menu : on juge la zone en marchant.
        val showing = ZonePreview.isShowing(terminal, now())
        addRenderableWidget(
            Button.builder(Component.translatable(key(if (showing) "arena_hide" else "arena_show"))) {
                if (ZonePreview.isShowing(terminal, now())) {
                    ZonePreview.hide()
                } else {
                    ZonePreview.show(terminal, facing, zone(), auto, now())
                }
                rebuildWidgets()
            }.bounds(left + 6, top + 108, panelWidth - 12, 20).build(),
        )
        addRenderableWidget(
            Button.builder(Component.translatable(key("apply"))) {
                ClientPlayNetworking.send(SetArenaPayload(terminal, ArenaSettings(auto, zone())))
                onClose()
            }.bounds(left + 6, top + 132, panelWidth - 12, 20).build(),
        )
    }

    private fun zone() = ArenaZone.clamped(zoneWidth, zoneLength, zoneHeight)

    /** L'heure du monde, en ticks : la minuterie de l'affichage de la zone. */
    private fun now(): Long = minecraft?.level?.gameTime ?: 0L

    private fun previewChanged() = ZonePreview.update(terminal, zone(), auto)

    // ------------------------------------------------------------------------------------------
    // Écrans
    // ------------------------------------------------------------------------------------------

    private val wallsWidth: Int get() = min(width - 2 * left, 420)

    private fun initWalls() {
        walls.forEachIndexed { index, wall ->
            addRenderableWidget(
                Button.builder(Component.translatable(key("unlink"))) {
                    ClientPlayNetworking.send(UnlinkWallPayload(terminal, wall.pos))
                    walls.removeAt(index)
                    rebuildWidgets()
                }.bounds(left + wallsWidth - 70, top + index * rowHeight, 70, 20).build(),
            )
        }
    }

    private fun renderWalls(graphics: GuiGraphics) {
        if (walls.isEmpty()) {
            graphics.drawString(font, Component.translatable(key("walls_empty")), left + 4, top + 6, 0xFFFFFFFF.toInt())
        }
        walls.forEachIndexed { index, wall ->
            val y = top + index * rowHeight + 6
            val number = Component.literal("${index + 1}. ")
            val text = if (wall.status == WallStatus.READY) {
                Component.translatable(key("wall_row"), wall.width, wall.height, wall.pos.x, wall.pos.y, wall.pos.z)
            } else {
                Component.literal("${wall.pos.x}, ${wall.pos.y}, ${wall.pos.z}")
            }
            graphics.drawString(font, number.copy().append(text), left + 4, y, 0xFFFFFFFF.toInt())
            val (statusKey, color) = when (wall.status) {
                WallStatus.READY -> "wall_ready" to 0xFF5FD3A6.toInt()
                WallStatus.MISSING -> "wall_missing" to 0xFFFF7A80.toInt()
                WallStatus.TOO_FAR -> "wall_too_far" to 0xFFFFD84A.toInt()
                WallStatus.UNLOADED -> "wall_unloaded" to 0xFF98A3C4.toInt()
            }
            val status = Component.translatable(key(statusKey))
            graphics.drawString(font, status, left + wallsWidth - 78 - font.width(status), y, color)
        }
        val hintTop = top + max(1, walls.size) * rowHeight + 10
        font.split(Component.translatable(key("walls_hint")), wallsWidth).forEachIndexed { i, line ->
            graphics.drawString(font, line, left + 4, hintTop + i * 10, 0xFFC8D0E8.toInt())
        }
    }

    /** Un curseur de taille en blocs, par pas de 2 pour que la boîte reste centrée sur un bloc. */
    private inner class SizeSlider(
        y: Int,
        private val minValue: Int,
        private val maxValue: Int,
        initial: Int,
        private val labelKey: String,
        private val onChange: (Int) -> Unit,
    ) : AbstractSliderButton(left + 6, y, panelWidth - 12, 20, Component.empty(), fraction(initial, minValue, maxValue)) {

        init {
            updateMessage()
        }

        private fun current(): Int {
            val raw = minValue + value * (maxValue - minValue)
            return (Mth.floor(raw / 2.0 + 0.5) * 2).coerceIn(minValue, maxValue)
        }

        override fun updateMessage() {
            message = Component.translatable(key(labelKey), current())
        }

        override fun applyValue() = onChange(current())
    }

    override fun tick() {
        super.tick()
        if (tab != Tab.ARENA) return
        // Pendant le réglage, les bords de la zone, deux fois par seconde. Si « Afficher la zone » est
        // allumé, c'est LeagueScreenClient qui les dessine déjà : on ne les dessine pas deux fois.
        if (ticks++ % 10 != 0 || ZonePreview.isShowing(terminal, now())) return
        val level = minecraft?.level ?: return
        ZoneParticles.draw(level, terminal, facing, zone(), auto)
    }

    // ------------------------------------------------------------------------------------------
    // Dessin
    // ------------------------------------------------------------------------------------------

    /** Dans l'onglet Arène, pas de flou : on doit voir le monde et les particules derrière le menu. */
    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        if (tab == Tab.ARENA) {
            graphics.fill(left, top - 4, left + panelWidth, top + 214, 0xB0101828.toInt())
        } else {
            super.renderBackground(graphics, mouseX, mouseY, partialTick)
        }
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(graphics, mouseX, mouseY, partialTick)
        if (tab == Tab.WALLS) {
            renderWalls(graphics)
            return
        }
        if (tab == Tab.ARENA) {
            val hint = font.split(Component.translatable(key("arena_hint")), panelWidth - 12)
            hint.forEachIndexed { i, line -> graphics.drawString(font, line, left + 6, top + 160 + i * 10, 0xFFC8D0E8.toInt()) }
            return
        }
        graphics.drawCenteredString(font, title, width / 2 + 60, 14, 0xFFFFFFFF.toInt())

        val box = previewBox()
        graphics.fill(box.x - 1, box.y - 1, box.x + box.width + 1, box.y + box.height + 1, 0xFF3A4670.toInt())
        val player = minecraft?.player ?: return
        val board = DemoBoards.board(player.gameProfile.name, player.uuid.toString(), phase)

        val pose = graphics.pose()
        pose.pushPose()
        // Le peintre travaille `y` vers le haut ; l'écran, `y` vers le bas. L'axe `z` est grossi
        // pour que les quelques millimètres entre calques restent distincts à l'échelle du pixel.
        pose.translate(box.x.toFloat(), (box.y + box.height).toFloat(), 50f)
        pose.scale(1f, -1f, 10f)
        ThemePainter.paint(pose, graphics.bufferSource(), box.width.toFloat(), box.height.toFloat(), board, ClientThemes.forWall(selected), partialTick)
        graphics.flush()
        pose.popPose()
    }

    override fun isPauseScreen(): Boolean = false

    private companion object {
        fun fraction(value: Int, min: Int, max: Int): Double = (value - min).toDouble() / (max - min)
    }
}
