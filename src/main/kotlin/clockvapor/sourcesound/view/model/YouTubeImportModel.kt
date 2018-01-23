package clockvapor.sourcesound.view.model

import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import tornadofx.*

class YouTubeImportModel {
    var success: Boolean = false

    val urlProperty: StringProperty = SimpleStringProperty("")
    var url: String by urlProperty
}
