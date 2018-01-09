package clockvapor.sourcesound

import javafx.collections.FXCollections
import javafx.collections.ObservableList

class GamePreset private constructor(val id: Int, val name: String, val soundsRate: Int) {
    companion object {
        val all: ObservableList<GamePreset> = FXCollections.observableArrayList(
            GamePreset(730, "Counter-Strike: Global Offensive", 44100),
            GamePreset(550, "Left 4 Dead 2", 22050)
        )

        operator fun get(id: Int): GamePreset = all.first { it.id == id }
    }
}
