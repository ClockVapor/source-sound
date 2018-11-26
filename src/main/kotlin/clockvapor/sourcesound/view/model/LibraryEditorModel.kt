package clockvapor.sourcesound.view.model

import clockvapor.sourcesound.model.Library
import javafx.collections.ObservableList
import tornadofx.getValue
import tornadofx.setValue

class LibraryEditorModel(val allLibraries: ObservableList<Library>) : AbstractEditorModel<Library>(Library()) {
    var nameProperty = bind { focus.nameProperty }
    var name: String by nameProperty
}
