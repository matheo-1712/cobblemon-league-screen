package matheo1712.leaguescreen.theme

import C_REACH
import matheo1712.leaguescreen.LeagueScreen

/**
 * Ce que le serveur répond quand un joueur choisit un thème dans le menu de la borne.
 *
 * Le choix arrive du client : rien n'y est cru sur parole. Le serveur refait chaque vérification
 * avec ce qu'il sait lui-même (où est le joueur, où est la borne, quels thèmes existent).
 */
object ThemeRules {

    enum class Answer(key: String) {
        APPLY("theme_applied"),
        TOO_FAR("theme_too_far"),
        NO_WALL("no_wall"),
        UNKNOWN_THEME("theme_unknown"),
        ;

        val messageKey: String = "message.${LeagueScreen.MOD_ID}.$key"
    }

    /** Les contrôles s'appliquent dans cet ordre : le premier qui échoue donne la réponse. */
    fun decide(distanceToTerminal: Double, wallValid: Boolean, themeExists: Boolean): Answer = when {
        distanceToTerminal > C_REACH -> Answer.TOO_FAR
        !wallValid -> Answer.NO_WALL
        !themeExists -> Answer.UNKNOWN_THEME
        else -> Answer.APPLY
    }
}
