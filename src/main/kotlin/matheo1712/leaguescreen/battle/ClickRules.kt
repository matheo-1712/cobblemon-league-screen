package matheo1712.leaguescreen.battle

import matheo1712.leaguescreen.LeagueScreen
import java.util.UUID

/** Ce que la borne répond à un clic. Chaque réponse a son message dans la barre d'action. */
enum class ClickResult {
    FOLLOW,
    ALREADY_FOLLOWED,
    NOT_IN_BATTLE,
    WILD_BATTLE,
    TOO_MANY_TRAINERS,
    WALL_BUSY,
    NO_WALL,
    ;

    val messageKey: String get() = "message.${LeagueScreen.MOD_ID}.${name.lowercase()}"
}

/**
 * Tout ce qu'il faut savoir pour répondre à un clic, déjà extrait du monde et de Cobblemon.
 *
 * @param wallValid la borne est reliée à un mur qui existe encore, est chargé et à portée
 * @param playerBattle le combat du joueur qui clique, s'il en a un
 * @param hasWildActor ce combat oppose le joueur à un Pokémon sauvage
 * @param actorCount le nombre de dresseurs dans ce combat
 * @param wallBattle le combat que le mur suit déjà en direct, s'il en suit un
 */
data class ClickContext(
    val wallValid: Boolean,
    val playerBattle: UUID?,
    val hasWildActor: Boolean,
    val actorCount: Int,
    val wallBattle: UUID?,
)

object ClickRules {

    /** Les contrôles s'appliquent dans cet ordre : le premier qui échoue donne la réponse. */
    fun decide(context: ClickContext): ClickResult = when {
        !context.wallValid -> ClickResult.NO_WALL
        context.playerBattle == null -> ClickResult.NOT_IN_BATTLE
        context.hasWildActor -> ClickResult.WILD_BATTLE
        context.actorCount != 2 -> ClickResult.TOO_MANY_TRAINERS
        context.wallBattle == context.playerBattle -> ClickResult.ALREADY_FOLLOWED
        context.wallBattle != null -> ClickResult.WALL_BUSY
        else -> ClickResult.FOLLOW
    }

    /**
     * La réponse à montrer quand une borne a plusieurs écrans : « suit » si au moins un écran suit,
     * sinon la raison la plus utile au joueur (déjà suivi, puis occupé, puis la première venue).
     */
    fun summary(results: List<ClickResult>): ClickResult = when {
        results.isEmpty() -> ClickResult.NO_WALL
        ClickResult.FOLLOW in results -> ClickResult.FOLLOW
        ClickResult.ALREADY_FOLLOWED in results -> ClickResult.ALREADY_FOLLOWED
        ClickResult.WALL_BUSY in results -> ClickResult.WALL_BUSY
        else -> results.first()
    }
}
