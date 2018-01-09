package clockvapor.sourcesound.model

import clockvapor.sourcesound.Game
import clockvapor.sourcesound.GamePreset
import javafx.beans.property.Property
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import tornadofx.*

class GameModel(val allGames: ObservableList<Game>) {
    var success: Boolean = false
    val presetProperty: Property<GamePreset> = SimpleObjectProperty(GamePreset.all[0])
    var preset: GamePreset by presetProperty
    val nameProperty: Property<String> = SimpleStringProperty("")
    var name: String by nameProperty
    val idProperty: Property<String> = SimpleStringProperty("")
    var id: String by idProperty
    val pathProperty: Property<String> = SimpleStringProperty("")
    var path: String by pathProperty
    val cfgPathProperty: Property<String> = SimpleStringProperty("")
    var cfgPath: String by cfgPathProperty
    var editing: Game? = null
}
