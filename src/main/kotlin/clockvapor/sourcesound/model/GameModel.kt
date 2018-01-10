package clockvapor.sourcesound.model

import clockvapor.sourcesound.Game
import clockvapor.sourcesound.GamePreset
import clockvapor.sourcesound.Sound
import javafx.beans.property.*
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
    val useUserdataProperty: Property<Boolean> = SimpleBooleanProperty(false)
    var useUserData: Boolean by useUserdataProperty
    val soundsRateProperty: Property<Number> = SimpleIntegerProperty(Sound.rates[0])
    var soundsRate: Number by soundsRateProperty
    var editing: Game? = null
}
