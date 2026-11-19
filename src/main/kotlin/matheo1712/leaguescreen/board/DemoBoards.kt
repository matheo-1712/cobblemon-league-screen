package matheo1712.leaguescreen.board

/**
 * Le tableau de démonstration : un joueur (6 Pokémon) contre le Maître (4 Pokémon), avec tous les
 * états de case. Montré par `/leaguescreen demo` et par l'aperçu du menu des thèmes.
 */
object DemoBoards {

    fun board(playerName: String, playerUuid: String, phase: Phase): BoardState {
        if (phase == Phase.OFF) return BoardState.OFF
        fun slot(state: SlotState, species: String) = SlotView.of(state, "cobblemon:$species", emptySet())
        val left = SideView(
            DisplayName(playerName, false),
            Portrait(PortraitKind.PLAYER, playerUuid),
            listOf(
                slot(SlotState.FAINTED, "lucario"),
                slot(SlotState.BENCH, "dragonite"),
                slot(SlotState.ACTIVE, "garchomp"),
                SlotView.HIDDEN, SlotView.HIDDEN, SlotView.HIDDEN,
            ),
        )
        val right = SideView(
            DisplayName("Maître", false),
            Portrait.NONE,
            listOf(
                slot(SlotState.FAINTED, "togekiss"),
                slot(SlotState.BENCH, "lapras"),
                slot(SlotState.ACTIVE, "milotic"),
                SlotView.HIDDEN, SlotView.EMPTY, SlotView.EMPTY,
            ),
        )
        return BoardState(phase, listOf(left, right), if (phase == Phase.VICTORY) 0 else -1)
    }
}
