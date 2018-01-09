package clockvapor.sourcesound.model

import clockvapor.sourcesound.Library
import clockvapor.sourcesound.Sound
import javafx.beans.property.Property
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import tornadofx.*

class LibraryModel(val allLibraries: ObservableList<Library>) {
    var success: Boolean = false
    val nameProperty: Property<String> = SimpleStringProperty("")
    var name: String by nameProperty
    val rateProperty: Property<Number> = SimpleIntegerProperty(Sound.rates[0])
    var rate: Number by rateProperty
    var editing: Library? = null
}
