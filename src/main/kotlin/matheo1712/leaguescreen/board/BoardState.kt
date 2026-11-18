package matheo1712.leaguescreen.board

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag

/** Ce que fait un mur : rien, suivre un combat, ou fêter son vainqueur. */
enum class Phase { OFF, LIVE, VICTORY }

/** L'état d'une case de la Couronne. [revealed] dit si le spectateur a le droit de savoir quel Pokémon c'est. */
enum class SlotState(val revealed: Boolean) {
    /** Pas de Pokémon à cette place : l'équipe en compte moins de six. */
    EMPTY(false),
    /** Un Pokémon pas encore envoyé. */
    HIDDEN(false),
    /** Déjà vu, actuellement dans sa Poké Ball. */
    BENCH(true),
    /** Sur le terrain. */
    ACTIVE(true),
    /** K.O. */
    FAINTED(true),
}

/**
 * Une case telle que le client la reçoit.
 *
 * Le constructeur est privé : on passe par [of], qui **efface l'espèce et les aspects d'une case non
 * révélée**. C'est ce qui garantit qu'un Pokémon pas encore envoyé ne quitte jamais le serveur, même
 * caché dans les données.
 */
class SlotView private constructor(val state: SlotState, val species: String?, val aspects: Set<String>) {

    override fun equals(other: Any?): Boolean =
        other is SlotView && state == other.state && species == other.species && aspects == other.aspects

    override fun hashCode(): Int = 31 * (31 * state.hashCode() + (species?.hashCode() ?: 0)) + aspects.hashCode()

    override fun toString(): String = "SlotView($state, $species, $aspects)"

    companion object {
        val EMPTY = SlotView(SlotState.EMPTY, null, emptySet())
        val HIDDEN = SlotView(SlotState.HIDDEN, null, emptySet())

        fun of(state: SlotState, species: String?, aspects: Set<String>): SlotView = when {
            state == SlotState.EMPTY -> EMPTY
            !state.revealed -> HIDDEN
            else -> SlotView(state, species, aspects.toSet())
        }
    }
}

/** Un nom à afficher : soit un texte tel quel, soit une clé de traduction que le client résout. */
data class DisplayName(val text: String, val translate: Boolean)

/**
 * Comment trouver le visage d'un dresseur : [PLAYER] par l'UUID d'un joueur, [TEXTURE] par le
 * chemin d'une texture de skin que le client possède, [TRAINER] par l'identifiant d'un dresseur
 * de Cobblemon Trainers dont le skin s'obtient auprès du serveur.
 */
enum class PortraitKind { NONE, PLAYER, TEXTURE, TRAINER }

/** Qui dessiner dans le cadre : [id] est l'UUID, le chemin de texture ou l'identifiant selon [kind], vide pour [PortraitKind.NONE]. */
data class Portrait(val kind: PortraitKind, val id: String) {
    companion object {
        val NONE = Portrait(PortraitKind.NONE, "")
    }
}

/** Un camp : son nom, son portrait et ses six cases. */
data class SideView(val name: DisplayName, val portrait: Portrait, val slots: List<SlotView>)

/**
 * Tout ce qu'un mur affiche. [winner] est l'indice du camp vainqueur (0 ou 1), ou -1.
 *
 * C'est cet objet, et lui seul, qui voyage du serveur au client.
 */
data class BoardState(val phase: Phase, val sides: List<SideView>, val winner: Int) {

    fun toTag(): CompoundTag {
        val tag = CompoundTag()
        tag.putString(PHASE, phase.name)
        tag.putInt(WINNER, winner)
        val sidesTag = ListTag()
        for (side in sides) {
            val sideTag = CompoundTag()
            sideTag.putString(NAME, side.name.text)
            sideTag.putBoolean(TRANSLATE, side.name.translate)
            sideTag.putString(PORTRAIT_KIND, side.portrait.kind.name)
            sideTag.putString(PORTRAIT_ID, side.portrait.id)
            val slotsTag = ListTag()
            for (slot in side.slots) {
                val slotTag = CompoundTag()
                slotTag.putString(STATE, slot.state.name)
                // Rien d'autre que l'état pour une case non révélée.
                if (slot.state.revealed && slot.species != null) {
                    slotTag.putString(SPECIES, slot.species)
                    val aspectsTag = ListTag()
                    slot.aspects.sorted().forEach { aspectsTag.add(StringTag.valueOf(it)) }
                    slotTag.put(ASPECTS, aspectsTag)
                }
                slotsTag.add(slotTag)
            }
            sideTag.put(SLOTS, slotsTag)
            sidesTag.add(sideTag)
        }
        tag.put(SIDES, sidesTag)
        return tag
    }

    companion object {
        val OFF = BoardState(Phase.OFF, emptyList(), -1)

        private const val PHASE = "phase"
        private const val WINNER = "winner"
        private const val SIDES = "sides"
        private const val NAME = "name"
        private const val TRANSLATE = "translate"
        private const val PORTRAIT_KIND = "portraitKind"
        private const val PORTRAIT_ID = "portraitId"
        private const val SLOTS = "slots"
        private const val STATE = "state"
        private const val SPECIES = "species"
        private const val ASPECTS = "aspects"

        /** Lit un tableau ; un tag vide ou illisible donne un mur éteint plutôt qu'une erreur. */
        fun fromTag(tag: CompoundTag): BoardState {
            val phase = Phase.entries.firstOrNull { it.name == tag.getString(PHASE) } ?: return OFF
            if (phase == Phase.OFF) return OFF

            val sidesTag = tag.getList(SIDES, Tag.TAG_COMPOUND.toInt())
            val sides = (0 until sidesTag.size).map { readSide(sidesTag.getCompound(it)) }
            return BoardState(phase, sides, if (tag.contains(WINNER)) tag.getInt(WINNER) else -1)
        }

        private fun readSide(tag: CompoundTag): SideView {
            val kind = PortraitKind.entries.firstOrNull { it.name == tag.getString(PORTRAIT_KIND) } ?: PortraitKind.NONE
            val portrait = if (kind == PortraitKind.NONE) Portrait.NONE else Portrait(kind, tag.getString(PORTRAIT_ID))
            val slotsTag = tag.getList(SLOTS, Tag.TAG_COMPOUND.toInt())
            val slots = (0 until slotsTag.size).map { readSlot(slotsTag.getCompound(it)) }
            return SideView(DisplayName(tag.getString(NAME), tag.getBoolean(TRANSLATE)), portrait, slots)
        }

        private fun readSlot(tag: CompoundTag): SlotView {
            // Un état que cette version ne connaît pas ne doit rien révéler.
            val state = SlotState.entries.firstOrNull { it.name == tag.getString(STATE) } ?: SlotState.HIDDEN
            if (!state.revealed || !tag.contains(SPECIES)) return SlotView.of(state, null, emptySet())
            val aspectsTag = tag.getList(ASPECTS, Tag.TAG_STRING.toInt())
            val aspects = (0 until aspectsTag.size).map { aspectsTag.getString(it) }.toSet()
            return SlotView.of(state, tag.getString(SPECIES), aspects)
        }
    }
}
