package matheo1712.leaguescreen.screen

/** Un rectangle sur le mur, depuis son coin bas gauche, dans les unités du repère (blocs ou pixels). */
data class Frame(val x: Float, val y: Float, val width: Float, val height: Float)

/** Un point du tableau, en fractions du cadre : `x` depuis la gauche, `y` depuis le **haut**. */
data class Spot(val x: Float, val y: Float)

/** Ce que tout tableau partage, quel que soit son thème. Les positions vivent dans les thèmes. */
object BoardLayout {
    /** Le nombre de cases par camp : une équipe compte six Pokémon au plus. */
    const val SLOT_COUNT = 6
}
