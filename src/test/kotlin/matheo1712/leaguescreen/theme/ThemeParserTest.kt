package matheo1712.leaguescreen.theme

import matheo1712.leaguescreen.board.SlotState
import net.minecraft.resources.ResourceLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ThemeParserTest {

    private val id = ResourceLocation.fromNamespaceAndPath("mon_pack", "indigo")

    private fun parse(json: String, fallback: Theme? = null): Theme =
        ThemeParser.parse(id, json, fallback).getOrThrow()

    private fun failure(json: String): String {
        val result = ThemeParser.parse(id, json, null)
        assertTrue(result.isFailure, "le thème aurait dû être refusé")
        return result.exceptionOrNull()!!.message ?: ""
    }

    @Test
    fun `un theme minimal prend les valeurs par defaut`() {
        val theme = parse("{}")
        assertEquals(id, theme.id)
        assertEquals("mon_pack:indigo", theme.name)
        assertEquals(1500f, theme.canvasWidth)
        assertEquals(900f, theme.canvasHeight)
        assertEquals(ColorRef.Fixed(0xFF000000.toInt()), theme.background)
        assertTrue(theme.live.isEmpty())
        assertTrue(theme.victory.isEmpty())
        assertTrue(theme.warnings.isEmpty())
    }

    @Test
    fun `les couleurs fixes se lisent en RGB et ARGB`() {
        val theme = parse("""{ "background": "#3B82F6", "colors": { "x": "#803B82F6" } }""")
        assertEquals(ColorRef.Fixed(0xFF3B82F6.toInt()), theme.background)
        assertEquals(0x803B82F6.toInt(), theme.colors["x"])
    }

    @Test
    fun `les references de couleur se resolvent`() {
        val theme = parse("""{ "colors": { "left": "#111111", "right": "#222222", "accent": "#333333" } }""")
        assertEquals(0xFF111111.toInt(), theme.resolve(ColorRef.Named("left"), null))
        assertEquals(0xFF333333.toInt(), theme.resolve(ColorRef.Named("accent"), null))
        assertEquals(0xFF111111.toInt(), theme.resolve(ColorRef.Named("side"), SideRef.LEFT))
        assertEquals(0xFF222222.toInt(), theme.resolve(ColorRef.Named("side"), SideRef.RIGHT))
        // Sans camp, ou pour un nom inconnu : blanc, plutôt qu'une erreur au milieu du dessin.
        assertEquals(0xFFFFFFFF.toInt(), theme.resolve(ColorRef.Named("side"), null))
        assertEquals(0xFFFFFFFF.toInt(), theme.resolve(ColorRef.Named("inconnu"), null))
    }

    @Test
    fun `une couleur peut etre assombrie`() {
        val theme = parse("""{ "colors": { "left": "#80FF40" }, "victoryBackground": "@side:0.5" }""")
        assertEquals(ColorRef.Named("side", 0.5f), theme.victoryBackground)
        // Le rouge, le vert et le bleu sont multipliés par 0,5 ; l'opacité reste.
        assertEquals(0xFF407F20.toInt(), theme.resolve(ColorRef.Named("side", 0.5f), SideRef.LEFT))
        assertEquals(0xFF407F20.toInt(), theme.resolve(ColorRef.Named("left", 0.5f), null))
    }

    @Test
    fun `le fond de victoire reprend le fond par defaut`() {
        val theme = parse("""{ "background": "#0A0F1F" }""")
        assertEquals(ColorRef.Fixed(0xFF0A0F1F.toInt()), theme.victoryBackground)
    }

    @Test
    fun `un portrait sans fond prend la couleur du camp assombrie au quart`() {
        val theme = parse("""{ "live": [ { "type": "portrait", "side": "left", "x": 0, "y": 0, "w": 1, "h": 1 } ] }""")
        assertEquals(ColorRef.Named("side", 0.25f), assertIs<PortraitLayer>(theme.live.single()).fill)
    }

    @Test
    fun `un assombrissement illisible rejette le theme`() {
        assertTrue(failure("""{ "background": "@side:sombre" }""").contains("background"))
    }

    @Test
    fun `une couleur inconnue rejette le theme`() {
        assertTrue(failure("""{ "background": "bleu" }""").contains("background"))
    }

    @Test
    fun `un champ du mauvais type rejette le theme`() {
        val message = failure("""{ "live": [ { "type": "fill", "x": "abc", "y": 0, "w": 10, "h": 10 } ] }""")
        assertTrue(message.contains("live[0].x"), message)
        assertTrue(failure("""{ "live": {} }""").contains("live"))
        assertTrue(failure("""{ "canvas": [1500] }""").contains("canvas"))
    }

    @Test
    fun `une taille de maquette nulle rejette le theme`() {
        assertTrue(failure("""{ "canvas": [0, 900] }""").contains("canvas"))
    }

    @Test
    fun `les six types de calque se lisent`() {
        val theme = parse(
            """{
              "live": [
                { "type": "fill", "color": "#102030", "x": 0, "y": 0, "w": 1500, "h": 900 },
                { "type": "image", "image": "mon_pack:textures/screen/fond.png", "x": 1.5, "y": 2, "w": 3, "h": 4, "alpha": 0.5 },
                { "type": "text", "value": "VS", "align": "left", "color": "@accent", "x": 650, "y": 370, "w": 200, "h": 105 },
                { "type": "name", "side": "right", "x": 870, "y": 588, "w": 330, "h": 48 },
                { "type": "portrait", "side": "left", "border": 15, "fill": "#000000", "x": 338, "y": 296, "w": 255, "h": 255 },
                { "type": "slot", "side": "left", "index": 6, "x": 348, "y": 713, "w": 129, "h": 129 }
              ]
            }""",
        )
        assertEquals(6, theme.live.size)
        val fill = assertIs<FillLayer>(theme.live[0])
        assertEquals(ColorRef.Fixed(0xFF102030.toInt()), fill.color)
        val image = assertIs<ImageLayer>(theme.live[1])
        assertEquals(ResourceLocation.parse("mon_pack:textures/screen/fond.png"), image.image)
        assertEquals(Box(1.5f, 2f, 3f, 4f), image.box)
        assertEquals(0.5f, image.alpha)
        val text = assertIs<TextLayer>(theme.live[2])
        assertEquals(Align.LEFT, text.align)
        assertEquals(ColorRef.Named("accent"), text.color)
        assertEquals(SideRef.RIGHT, assertIs<NameLayer>(theme.live[3]).side)
        val portrait = assertIs<PortraitLayer>(theme.live[4])
        assertEquals(ColorRef.Named("side"), portrait.frame)
        assertEquals(15f, portrait.border)
        val slot = assertIs<SlotLayer>(theme.live[5])
        assertEquals(6, slot.index)
        assertNull(slot.style)
    }

    @Test
    fun `un type inconnu est ignore avec un avertissement`() {
        val theme = parse("""{ "live": [ { "type": "hologramme", "x": 0, "y": 0, "w": 1, "h": 1 } ] }""")
        assertTrue(theme.live.isEmpty())
        assertTrue(theme.warnings.single().contains("hologramme"))
    }

    @Test
    fun `winner dans la scene en direct est ignore`() {
        val theme = parse(
            """{
              "live": [ { "type": "name", "side": "winner", "x": 0, "y": 0, "w": 1, "h": 1 } ],
              "victory": [ { "type": "name", "side": "winner", "x": 0, "y": 0, "w": 1, "h": 1 } ]
            }""",
        )
        assertTrue(theme.live.isEmpty())
        assertEquals(1, theme.warnings.size)
        assertEquals(SideRef.WINNER, assertIs<NameLayer>(theme.victory.single()).side)
    }

    @Test
    fun `index hors de 1 a 6 rejette`() {
        assertTrue(failure("""{ "live": [ { "type": "slot", "side": "left", "index": 7, "x": 0, "y": 0, "w": 1, "h": 1 } ] }""").contains("index"))
    }

    @Test
    fun `l'opacite est bornee entre 0 et 1`() {
        val theme = parse("""{ "live": [ { "type": "fill", "alpha": 4, "x": 0, "y": 0, "w": 1, "h": 1 } ] }""")
        assertEquals(1f, theme.live.single().alpha)
    }

    @Test
    fun `les styles de case se lisent`() {
        val theme = parse(
            """{ "slots": { "active": [
                 { "image": "a:textures/halo.png", "color": "@accent", "scale": 1.7 },
                 { "image": "a:textures/circle.png", "alpha": 0.4 } ] } }""",
        )
        val active = theme.slots.getValue(SlotState.ACTIVE)
        assertEquals(2, active.size)
        assertEquals(1.7f, active[0].scale)
        assertEquals(ColorRef.Named("accent"), active[0].color)
        assertEquals(ColorRef.Fixed(0xFFFFFFFF.toInt()), active[1].color)
        assertEquals(0.4f, active[1].alpha)
        assertEquals(1f, active[1].scale)
    }

    @Test
    fun `colors pokemon et slots absents viennent du fallback`() {
        val fallback = parse(
            """{
              "colors": { "left": "#111111", "right": "#222222", "accent": "#333333" },
              "pokemon": { "fit": 0.5, "yaw": 20, "depth": 0.2, "fainted": "#444444" },
              "slots": { "hidden": [ { "image": "a:textures/circle.png" } ] }
            }""",
        )
        val theme = parse("""{ "colors": { "accent": "#999999" } }""", fallback)
        assertEquals(0xFF111111.toInt(), theme.colors["left"])
        assertEquals(0xFF999999.toInt(), theme.colors["accent"])
        assertEquals(PokemonStyle(0.5f, 20f, 0.2f, ColorRef.Fixed(0xFF444444.toInt())), theme.pokemon)
        assertEquals(1, theme.slots.getValue(SlotState.HIDDEN).size)
    }

    @Test
    fun `un JSON illisible rejette sans exception`() {
        assertTrue(ThemeParser.parse(id, "{ pas du json", null).isFailure)
        assertTrue(ThemeParser.parse(id, "[]", null).isFailure)
    }

    @Test
    fun `le calque active lit son camp, son rang et sa taille`() {
        val theme = parse(
            """{ "live": [ { "type": "active", "side": "left", "w": 200, "h": 200 },
                           { "type": "active", "side": "right", "index": 2, "fit": 0.8, "block": 120, "yaw": 20, "w": 200, "h": 200 } ],
                 "victory": [ { "type": "active", "side": "winner", "w": 200, "h": 200 } ] }""",
        )
        val left = assertIs<ActiveLayer>(theme.live[0])
        assertEquals(SideRef.LEFT, left.side)
        assertEquals(1, left.index)
        assertEquals(1f, left.fit)
        assertNull(left.yaw)
        assertNull(left.block)
        val right = assertIs<ActiveLayer>(theme.live[1])
        assertEquals(2, right.index)
        assertEquals(0.8f, right.fit)
        assertEquals(20f, right.yaw)
        assertEquals(120f, right.block)
        assertEquals(SideRef.WINNER, assertIs<ActiveLayer>(theme.victory[0]).side)
        assertTrue(failure("""{ "live": [ { "type": "active", "side": "left", "index": 4, "w": 1, "h": 1 } ] }""").startsWith("live[0].index"))
    }
}
