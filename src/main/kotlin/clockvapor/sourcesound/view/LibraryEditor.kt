package clockvapor.sourcesound.view

import clockvapor.sourcesound.model.Library
import clockvapor.sourcesound.model.Sound
import clockvapor.sourcesound.view.model.LibraryEditorModel
import javafx.collections.ObservableList
import tornadofx.*
import java.nio.file.Paths

class LibraryEditor(allLibraries: ObservableList<Library>)
    : AbstractEditor<Library, LibraryEditorModel>(LibraryEditorModel(allLibraries)) {

    override fun Form.fieldSets() {
        fieldset {
            field(messages["name"]) {
                textfield(model.nameProperty) {
                    validator {
                        if (it == null) error(messages["nameNull"])
                        else if (it.isBlank()) error(messages["nameBlank"])
                        else if (it in model.allLibraries.filter { it !== model.focus }.map(Library::name)) error(
                            messages["nameTaken"])
                        else {
                            val okay =
                                try {
                                    Paths.get(it)
                                    true
                                } catch (e: Exception) {
                                    false
                                }
                            if (okay) {
                                success()
                            } else {
                                error(messages["invalidLibraryName"])
                            }
                        }
                    }
                }
            }
            field(messages["rate"]) {
                combobox(model.rateProperty, Sound.rates)
            }
        }
    }
}
