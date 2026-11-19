package matheo1712.leaguescreen.client.render

/** Petits calculs de couleurs ARGB. Les couleurs elles-mêmes viennent des thèmes. */
object Colors {
    const val WHITE = 0xFFFFFFFF.toInt()

    /** Remplace l'opacité par [alpha] (0 à 1). */
    fun withAlpha(argb: Int, alpha: Float): Int = ((alpha.coerceIn(0f, 1f) * 255).toInt() shl 24) or (argb and 0x00FFFFFF)

    /** Multiplie l'opacité par [alpha] (0 à 1) : l'opacité d'un calque s'ajoute à celle de sa couleur. */
    fun fade(argb: Int, alpha: Float): Int {
        if (alpha >= 1f) return argb
        val a = ((argb ushr 24) * alpha.coerceIn(0f, 1f)).toInt()
        return (a shl 24) or (argb and 0x00FFFFFF)
    }
}
