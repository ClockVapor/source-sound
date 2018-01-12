package clockvapor.sourcesound.model

import javafx.collections.FXCollections
import javafx.collections.ObservableList

class GamePreset private constructor(val id: Int, val name: String, val soundsRate: Int, val useUserdata: Boolean) {
    companion object {
        val all: ObservableList<GamePreset> = FXCollections.observableArrayList(
            GamePreset(730, "Counter-Strike: Global Offensive", 22050, true),
            GamePreset(550, "Left 4 Dead 2", 11025, false)
        )
    }
}
