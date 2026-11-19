/**
 * ------------------
 * General rules
 * ------------------
 */
const val C_FOLDER = "leaguescreen/themes"

/** Distance maximale entre le joueur et le centre de la borne, en blocs. */
const val C_REACH = 8.0

/**
 * ------------------
 * Link rules
 * ------------------
 */
const val C_MAX_DISTANCE = 64

/**
 * ------------------
 * Battle tracker rules
 * ------------------
 */
const val C_POLL_INTERVAL = 5

/**
 * ------------------
 * Follow book rules
 * ------------------
 */
/** Dix secondes. */
const val C_VICTORY_TICKS = 200L

/**
 * ------------------
 * Side rules
 * ------------------
 */
// En dessous de cet écart, en blocs, les deux dresseurs sont alignés face au mur : ni gauche ni droite.
const val C_MIN_GAP = 1.0

/**
 * ------------------
 * Screen block entity data keys
 * ------------------
 */
const val C_WALL = "wall"
const val C_BOARD = "board"
const val C_THEME = "theme"

/**
 * ------------------
 * Terminal block entity data keys
 * ------------------
 */
const val C_WALLS = "walls"
const val C_ARENA = "arena"
const val C_LEGACY_WALL = "linked_wall"

/**
 * ------------------
 * Default theme rules
 * ------------------
 */
const val C_COURONNE_ID = "cobblemon-league-screen:couronne"

/**
 * ------------------
 * Color rules
 * ------------------
 */
const val COLOR_WHITE = 0xFFFFFFFF.toInt()