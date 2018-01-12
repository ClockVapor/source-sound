package clockvapor.sourcesound.view.model

import tornadofx.*

abstract class AbstractEditorModel<T>(var focus: T) : ViewModel() {
    var success: Boolean = false
}
