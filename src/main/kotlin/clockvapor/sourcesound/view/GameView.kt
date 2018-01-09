package clockvapor.sourcesound.view

import clockvapor.sourcesound.Game
import clockvapor.sourcesound.GamePreset
import clockvapor.sourcesound.browseForDirectory
import clockvapor.sourcesound.model.GameModel
import clockvapor.sourcesound.stringListCell
import javafx.collections.ObservableList
import javafx.geometry.HPos
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.util.Callback
import tornadofx.*

class GameView(allGames: ObservableList<Game>) : View() {
    val model: GameModel = GameModel(allGames)

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
                label(messages["preset"]) {
                    GridPane.setColumnIndex(this, 0)
                }
                combobox(model.presetProperty, GamePreset.all) {
                    GridPane.setColumnIndex(this, 1)
                    cellFactory = Callback { stringListCell { it.name } }
                    buttonCell = stringListCell { it.name }
                }
                button(messages["apply"]) {
                    GridPane.setColumnIndex(this, 2)
                    action {
                        model.name = model.preset.name
                        model.id = model.preset.id.toString()
                    }
                }
            }
            row {
                label(messages["name"]) {
                    GridPane.setColumnIndex(this, 0)
                }
                textfield(model.nameProperty) {
                    GridPane.setColumnIndex(this, 1)
                    textProperty().addListener { _, _, _ ->
                        updateOkButton()
                    }
                }
            }
            row {
                label(messages["gameId"]) {
                    GridPane.setColumnIndex(this, 0)
                }
                textfield(model.idProperty) {
                    GridPane.setColumnIndex(this, 1)
                    textProperty().addListener { _, oldValue, newValue ->
                        if (!newValue.all(Char::isDigit)) {
                            text = oldValue
                        }
                        updateOkButton()
                    }
                }
            }
            row {
                label(messages["cfgPath"]) {
                    GridPane.setColumnIndex(this, 0)
                }
                textfield(model.cfgPathProperty) {
                    GridPane.setColumnIndex(this, 1)
                    textProperty().addListener { _, _, _ ->
                        updateOkButton()
                    }
                }
                button(messages["browse..."]) {
                    GridPane.setColumnIndex(this, 2)
                    action {
                        browseForDirectory(primaryStage, messages["cfgPath"], model.cfgPath)?.let {
                            model.cfgPath = it
                        }
                    }
                }
            }
        }
        hbox(8.0) {
            alignment = Pos.CENTER_RIGHT
            okButton = button(messages["ok"]) {
                action {
                    if (model.allGames.filter { it !== model.editing }.any { it.name == model.name }) {
                        error(owner = primaryStage, header = messages["nameTakenHeader"],
                            content = messages["nameTakenContent"])
                    } else {
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
        super.onDock()
        model.success = false
        updateOkButton()
    }

    fun populate(game: Game) {
        model.editing = game
        model.name = game.name
        model.id = game.id.toString()
        model.cfgPath = game.cfgPath
    }

    fun clear() {
        model.editing = null
        model.name = ""
        model.id = ""
        model.cfgPath = ""
    }

    private fun updateOkButton() {
        okButton.isDisable = model.name.isBlank() || model.id.isBlank() || model.cfgPath.isBlank()
    }
}
