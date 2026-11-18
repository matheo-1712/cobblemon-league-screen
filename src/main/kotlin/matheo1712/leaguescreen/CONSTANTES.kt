/**
 * ------------------
 * Link rules
 * ------------------
 */
const val MAX_DISTANCE = 64

/**
 * ------------------
 * Battle tracker rules
 * ------------------
 */
const val POLL_INTERVAL = 5

/**
 * ------------------
 * Follow book rules
 * ------------------
 */
/** Dix secondes. */
const val VICTORY_TICKS = 200L

/**
 * ------------------
 * Side rules
 * ------------------
 */
// En dessous de cet écart, en blocs, les deux dresseurs sont alignés face au mur : ni gauche ni droite.
const val MIN_GAP = 1.0

/**
 * ------------------
 * Screen block entity data keys
 * ------------------
 */
const val WALL = "wall"
const val BOARD = "board"
const val THEME = "theme"

/**
 * ------------------
 * Terminal block entity data keys
 * ------------------
 */
const val WALLS = "walls"
const val ARENA = "arena"
const val LEGACY_WALL = "linked_wall"