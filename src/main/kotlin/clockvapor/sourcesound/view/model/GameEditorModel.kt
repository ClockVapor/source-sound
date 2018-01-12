package clockvapor.sourcesound.view.model

import clockvapor.sourcesound.model.Game
import clockvapor.sourcesound.model.GamePreset
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableList
import tornadofx.*

class GameEditorModel(val allGames: ObservableList<Game>) : AbstractEditorModel<Game>(Game()) {
    var nameProperty = bind { focus.nameProperty }
    var name: String by nameProperty

    var idProperty: BindingAwareSimpleIntegerProperty = bind { focus.idProperty }
    var id: Int by idProperty

    var pathProperty = bind { focus.pathProperty }
    var path: String by pathProperty

    var cfgPathProperty = bind { focus.cfgPathProperty }
    var cfgPath: String by cfgPathProperty

    var useUserdataProperty = bind { focus.useUserdataProperty }
    var useUserdata: Boolean by useUserdataProperty

    var soundsRateProperty = bind { focus.soundsRateProperty }
    var soundsRate: Number by soundsRateProperty

    val presetProperty: ObjectProperty<GamePreset?> = SimpleObjectProperty()
    var preset: GamePreset? by presetProperty
}
