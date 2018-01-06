package clockvapor.sourcesound.view

import clockvapor.sourcesound.browseForDirectory
import javafx.beans.property.Property
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.geometry.HPos
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import tornadofx.*

class NewLibraryView : View() {
    var success: Boolean = false
    val soundsPath: Property<String> = SimpleStringProperty("")
    val soundsRate: Property<Number> = SimpleIntegerProperty(rates[0])
    val cfgPath: Property<String> = SimpleStringProperty("")
    private var okButton: Button by singleAssign()
    private var cancelButton: Button by singleAssign()

    override val root = vbox(8.0) {
        paddingAll = 8.0
        gridpane {
            hgap = 8.0
            vgap = 8.0
            columnConstraints +=
                ColumnConstraints(0.0, GridPane.USE_COMPUTED_SIZE, Double.MAX_VALUE, Priority.NEVER, HPos.LEFT, true)
            columnConstraints +=
                ColumnConstraints(0.0, GridPane.USE_COMPUTED_SIZE, Double.MAX_VALUE, Priority.ALWAYS, HPos.RIGHT, true)
            row {
                label(messages["soundsPath"]) {
                    GridPane.setColumnIndex(this, 0)
                }
                textfield(soundsPath) {
                    GridPane.setColumnIndex(this, 1)
                    textProperty().addListener { _, _, _ -> updateOkButton() }
                }
                button(messages["browse"]) {
                    GridPane.setColumnIndex(this, 2)
                    action {
                        browseForDirectory(primaryStage, messages["soundsPath"], soundsPath.value)?.let {
                            soundsPath.value = it
                        }
                    }
                }
            }
            row {
                label(messages["cfgPath"]) {
                    GridPane.setColumnIndex(this, 0)
                }
                textfield(cfgPath) {
                    GridPane.setColumnIndex(this, 1)
                    textProperty().addListener { _, _, _ -> updateOkButton() }
                }
                button(messages["browse"]) {
                    GridPane.setColumnIndex(this, 2)
                    action {
                        browseForDirectory(primaryStage, messages["cfgPath"], cfgPath.value)?.let {
                            cfgPath.value = it
                        }
                    }
                }
            }
            row {
                label(messages["soundsRate"]) {
                    GridPane.setColumnIndex(this, 0)
                }
                combobox(soundsRate, rates) {
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
                    success = true
                    close()
                }
            }
            cancelButton = button(messages["cancel"]) {
                action {
                    success = false
                    close()
                }
            }
        }
    }

    init {
        title = messages["title"]
    }

    override fun onDock() {
        super.onDock()
        updateOkButton()
    }

    private fun updateOkButton() {
        okButton.isDisable = soundsPath.value.isNullOrBlank() ||
            cfgPath.value.isNullOrBlank() ||
            soundsRate.value !in rates
    }

    companion object {
        private val rates: ObservableList<Int> = FXCollections.observableArrayList(22050, 44100)
    }
}
