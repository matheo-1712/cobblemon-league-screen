package matheo1712.leaguescreen.theme

import COLOR_WHITE
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import matheo1712.leaguescreen.board.SlotState
import net.minecraft.resources.ResourceLocation

/**
 * Lit un thème. Ne connaît ni le monde ni le client : on lui donne le texte du fichier.
 *
 * Un champ invalide rejette **tout** le thème, avec un message qui nomme le champ (`live[3].x`) :
 * un écran à moitié faux est pire qu'un thème absent. Ce qui peut être ignoré sans fausser
 * l'écran (un type de calque inconnu) l'est, avec un avertissement.
 */
object ThemeParser {

    private val DEFAULT_POKEMON = PokemonStyle(0.68f, 35f, 0.15f, ColorRef.Fixed(0xFF595959.toInt()))

    /**
     * @param fallback le thème qui fournit `colors`, `pokemon` et `slots` absents (la Couronne) ;
     *   `null` quand on lit la Couronne elle-même
     */
    fun parse(id: ResourceLocation, json: String, fallback: Theme?): Result<Theme> = runCatching {
        val root = try {
            JsonParser.parseString(json)
        } catch (e: Exception) {
            throw IllegalArgumentException("JSON illisible : ${e.message}")
        }
        if (!root.isJsonObject) throw IllegalArgumentException("le thème doit être un objet JSON")
        Reader(id, root.asJsonObject, fallback).theme()
    }

    private class Reader(val id: ResourceLocation, val root: JsonObject, val fallback: Theme?) {

        val warnings = mutableListOf<String>()

        fun theme(): Theme {
            val canvas = root.get("canvas")?.let { array(it, "canvas") }
            val (width, height) = if (canvas == null) 1500f to 900f else {
                if (canvas.size() != 2) fail("canvas", "deux nombres attendus, [largeur, hauteur]")
                number(canvas[0], "canvas[0]") to number(canvas[1], "canvas[1]")
            }
            if (width <= 0f || height <= 0f) fail("canvas", "la largeur et la hauteur doivent être positives")

            val colors = (fallback?.colors ?: emptyMap()).toMutableMap()
            root.get("colors")?.let { element ->
                for ((name, value) in obj(element, "colors").entrySet()) {
                    val color = color(value, "colors.$name")
                    if (color !is ColorRef.Fixed) fail("colors.$name", "une couleur de la palette doit être fixe (#RRGGBB)")
                    colors[name] = color.argb
                }
            }

            val slots = (fallback?.slots ?: emptyMap()).toMutableMap()
            root.get("slots")?.let { slots.putAll(slotStyles(it, "slots")) }

            val background = root.get("background")?.let { color(it, "background") } ?: ColorRef.Fixed(0xFF000000.toInt())
            return Theme(
                id = id,
                name = root.get("name")?.let { string(it, "name") } ?: id.toString(),
                canvasWidth = width,
                canvasHeight = height,
                background = background,
                victoryBackground = root.get("victoryBackground")?.let { color(it, "victoryBackground") } ?: background,
                colors = colors,
                pokemon = pokemon(root.get("pokemon"), fallback?.pokemon ?: DEFAULT_POKEMON),
                slots = slots,
                live = layers(root.get("live"), "live", victory = false),
                victory = layers(root.get("victory"), "victory", victory = true),
                warnings = warnings,
            )
        }

        fun pokemon(element: JsonElement?, base: PokemonStyle): PokemonStyle {
            if (element == null) return base
            val o = obj(element, "pokemon")
            return PokemonStyle(
                fit = o.get("fit")?.let { number(it, "pokemon.fit") } ?: base.fit,
                yaw = o.get("yaw")?.let { number(it, "pokemon.yaw") } ?: base.yaw,
                depth = o.get("depth")?.let { number(it, "pokemon.depth") } ?: base.depth,
                fainted = o.get("fainted")?.let { color(it, "pokemon.fainted") } ?: base.fainted,
            )
        }

        fun slotStyles(element: JsonElement, path: String): Map<SlotState, List<SlotImage>> {
            val result = mutableMapOf<SlotState, List<SlotImage>>()
            for ((key, value) in obj(element, path).entrySet()) {
                val state = SlotState.entries.firstOrNull { it.name.equals(key, ignoreCase = true) }
                    ?: fail("$path.$key", "état inconnu (empty, hidden, bench, active, fainted)")
                val images = array(value, "$path.$key")
                result[state] = images.mapIndexed { i, image -> slotImage(image, "$path.$key[$i]") }
            }
            return result
        }

        fun slotImage(element: JsonElement, path: String): SlotImage {
            val o = obj(element, path)
            return SlotImage(
                image = location(o.get("image") ?: fail("$path.image", "champ obligatoire"), "$path.image"),
                color = o.get("color")?.let { color(it, "$path.color") } ?: ColorRef.Fixed(COLOR_WHITE),
                alpha = alpha(o.get("alpha"), "$path.alpha"),
                scale = o.get("scale")?.let { number(it, "$path.scale") } ?: 1f,
            )
        }

        fun layers(element: JsonElement?, path: String, victory: Boolean): List<Layer> {
            if (element == null) return emptyList()
            return array(element, path).mapIndexedNotNull { i, layer -> layer(layer, "$path[$i]", victory) }
        }

        fun layer(element: JsonElement, path: String, victory: Boolean): Layer? {
            val o = obj(element, path)
            val type = string(o.get("type") ?: fail("$path.type", "champ obligatoire"), "$path.type")
            val box = Box(
                o.get("x")?.let { number(it, "$path.x") } ?: 0f,
                o.get("y")?.let { number(it, "$path.y") } ?: 0f,
                number(o.get("w") ?: fail("$path.w", "champ obligatoire"), "$path.w"),
                number(o.get("h") ?: fail("$path.h", "champ obligatoire"), "$path.h"),
            )
            val alpha = alpha(o.get("alpha"), "$path.alpha")
            val white = ColorRef.Fixed(COLOR_WHITE)
            fun colorOr(name: String, default: ColorRef) = o.get(name)?.let { color(it, "$path.$name") } ?: default
            fun align() = o.get("align")?.let { value ->
                val text = string(value, "$path.align")
                Align.entries.firstOrNull { it.name.equals(text, ignoreCase = true) }
                    ?: fail("$path.align", "left, center ou right attendu")
            } ?: Align.CENTER
            fun side(): SideRef? {
                val text = string(o.get("side") ?: fail("$path.side", "champ obligatoire"), "$path.side")
                val side = SideRef.entries.firstOrNull { it.name.equals(text, ignoreCase = true) }
                    ?: fail("$path.side", "left, right ou winner attendu")
                if (side == SideRef.WINNER && !victory) {
                    warnings += "$path : le camp « winner » n'existe que dans la scène de victoire, calque ignoré"
                    return null
                }
                return side
            }

            return when (type) {
                "fill" -> FillLayer(box, alpha, colorOr("color", white))
                "image" -> ImageLayer(box, alpha, location(o.get("image") ?: fail("$path.image", "champ obligatoire"), "$path.image"), colorOr("color", white))
                "text" -> TextLayer(box, alpha, string(o.get("value") ?: fail("$path.value", "champ obligatoire"), "$path.value"), colorOr("color", white), align())
                "name" -> side()?.let { NameLayer(box, alpha, it, colorOr("color", white), align()) }
                "portrait" -> side()?.let {
                    PortraitLayer(
                        box, alpha, it, colorOr("frame", ColorRef.Named("side")),
                        o.get("border")?.let { b -> number(b, "$path.border") },
                        o.get("fill")?.let { f -> color(f, "$path.fill") } ?: ColorRef.Named("side", 0.25f),
                    )
                }
                "slot" -> side()?.let {
                    if (it == SideRef.WINNER) fail("$path.side", "une case appartient à left ou right")
                    val index = number(o.get("index") ?: fail("$path.index", "champ obligatoire"), "$path.index").toInt()
                    if (index !in 1..6) fail("$path.index", "de 1 à 6")
                    SlotLayer(box, alpha, it, index, o.get("style")?.let { s -> slotStyles(s, "$path.style") })
                }
                else -> {
                    warnings += "$path : type de calque inconnu « $type », calque ignoré"
                    null
                }
            }
        }

        // ---- Lecture typée, avec le chemin du champ dans l'erreur ----

        fun fail(path: String, why: String): Nothing = throw IllegalArgumentException("$path : $why")

        fun obj(e: JsonElement, path: String): JsonObject = if (e.isJsonObject) e.asJsonObject else fail(path, "objet attendu")

        fun array(e: JsonElement, path: String): JsonArray = if (e.isJsonArray) e.asJsonArray else fail(path, "liste attendue")

        fun string(e: JsonElement, path: String): String =
            if (e.isJsonPrimitive && e.asJsonPrimitive.isString) e.asString else fail(path, "texte attendu")

        fun number(e: JsonElement, path: String): Float {
            if (!e.isJsonPrimitive || !e.asJsonPrimitive.isNumber) fail(path, "nombre attendu")
            val value = e.asFloat
            if (!value.isFinite()) fail(path, "nombre fini attendu")
            return value
        }

        fun alpha(e: JsonElement?, path: String): Float = e?.let { number(it, path).coerceIn(0f, 1f) } ?: 1f

        fun location(e: JsonElement, path: String): ResourceLocation =
            ResourceLocation.tryParse(string(e, path)) ?: fail(path, "identifiant de ressource attendu (pack:textures/…)")

        fun color(e: JsonElement, path: String): ColorRef {
            val text = string(e, path)
            if (text.startsWith("@") && text.length > 1) {
                val name = text.substring(1).substringBefore(':')
                val shade = text.substringAfter(':', "").takeIf { it.isNotEmpty() }?.let {
                    it.toFloatOrNull()?.takeIf { f -> f.isFinite() && f >= 0f }
                        ?: fail(path, "assombrissement attendu après « : », un nombre comme 0.5 (reçu « $text »)")
                } ?: 1f
                if (name.isEmpty()) fail(path, "nom de couleur attendu après @")
                return ColorRef.Named(name, shade)
            }
            val hex = text.removePrefix("#")
            if (!text.startsWith("#") || (hex.length != 6 && hex.length != 8) || !hex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
                fail(path, "couleur attendue (#RRGGBB, #AARRGGBB ou @nom), reçu « $text »")
            }
            val value = hex.toLong(16)
            return ColorRef.Fixed((if (hex.length == 6) 0xFF000000L or value else value).toInt())
        }
    }
}
