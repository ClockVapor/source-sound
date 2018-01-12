package clockvapor.sourcesound.view.model

import clockvapor.sourcesound.model.Library
import javafx.collections.ObservableList
import tornadofx.*

class LibraryEditorModel(val allLibraries: ObservableList<Library>) : AbstractEditorModel<Library>(Library()) {
    var nameProperty = bind { focus.nameProperty }
    var name: String by nameProperty

    var rateProperty: BindingAwareSimpleIntegerProperty = bind { focus.rateProperty }
    var rate: Int by rateProperty
}
