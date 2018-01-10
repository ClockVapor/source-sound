package clockvapor.sourcesound.view

import clockvapor.sourcesound.Library
import clockvapor.sourcesound.Sound
import clockvapor.sourcesound.model.LibraryModel
import javafx.collections.ObservableList
import javafx.geometry.HPos
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import tornadofx.*
import java.nio.file.Paths

class LibraryView(allLibraries: ObservableList<Library>) : View() {
    val model: LibraryModel = LibraryModel(allLibraries)
    private var okButton: Button by singleAssign()
    private var cancelButton: Button by singleAssign()

    override val root = vbox(8.0) {
        prefWidth = 300.0
        paddingAll = 8.0
        gridpane {
            hgap = 8.0
            vgap = 8.0
            vgrow = Priority.ALWAYS
            columnConstraints +=
                ColumnConstraints(0.0, GridPane.USE_COMPUTED_SIZE, Double.MAX_VALUE, Priority.NEVER, HPos.LEFT, true)
            columnConstraints +=
                ColumnConstraints(0.0, GridPane.USE_COMPUTED_SIZE, Double.MAX_VALUE, Priority.ALWAYS, HPos.RIGHT, true)
            row {
                label(messages["name"]) {
                    GridPane.setColumnIndex(this, 0)
                }
                textfield(model.nameProperty) {
                    GridPane.setColumnIndex(this, 1)
                    textProperty().addListener { _, _, _ -> updateOkButton() }
                }
            }
            row {
                label(messages["rate"]) {
                    GridPane.setColumnIndex(this, 0)
                }
                combobox(model.rateProperty, Sound.rates) {
                    GridPane.setColumnIndex(this, 1)
                    hgrow = Priority.ALWAYS
                    maxWidth = Double.MAX_VALUE
                    selectionModel.selectedItemProperty().addListener { _, _, _ -> updateOkButton() }
                }
            }
        }
        hbox(8.0) {
            alignment = Pos.CENTER_RIGHT
            okButton = button(messages["ok"]) {
                action {
                    if (validate()) {
                        model.success = true
                        close()
                    }
                }
            }
            cancelButton = button(messages["cancel"]) {
                action {
                    model.success = false
                    close()
                }
            }
        }
    }

    init {
        title = messages["title"]
    }

    override fun onDock() {
        model.success = false
        super.onDock()
        updateOkButton()
    }

    fun populate(library: Library) {
        model.editing = library
        model.name = library.name
        if (library.rate in Sound.rates) {
            model.rate = library.rate
        } else {
            model.rate = Sound.rates[0]
        }
    }

    fun clear() {
        model.editing = null
        model.name = ""
        model.rate = Sound.rates[0]
    }

    private fun updateOkButton() {
        okButton.isDisable = model.name.isBlank() || model.rate !in Sound.rates
    }

    private fun validate(): Boolean {
        if (model.allLibraries.filter { it !== model.editing }.any { it.name == model.name }) {
            error(owner = primaryStage, header = messages["libraryNameTakenHeader"],
                content = messages["libraryNameTakenContent"])
            return false
        }
        try {
            Paths.get(model.name)
        } catch (e: Exception) {
            error(owner = primaryStage, header = messages["invalidLibraryNameHeader"],
                content = messages["invalidLibraryNameContent"])
            return false
        }
        return true
    }
}
