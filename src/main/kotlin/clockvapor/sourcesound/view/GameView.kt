package clockvapor.sourcesound.view

import clockvapor.sourcesound.*
import clockvapor.sourcesound.model.GameModel
import javafx.collections.ObservableList
import javafx.geometry.HPos
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.util.Callback
import tornadofx.*
import java.io.File
import java.nio.file.Paths

class GameView(allGames: ObservableList<Game>) : View() {
    val model: GameModel = GameModel(allGames)

    private var okButton: Button by singleAssign()
    private var cancelButton: Button by singleAssign()

    override val root = vbox(8.0) {
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
                label(messages["preset"]) {
                    GridPane.setColumnIndex(this, 0)
                }
                combobox(model.presetProperty, GamePreset.all) {
                    GridPane.setColumnIndex(this, 1)
                    maxWidth = Double.MAX_VALUE
                    cellFactory = Callback { stringListCell { it.name } }
                    buttonCell = stringListCell { it.name }
                }
                button(messages["apply"]) {
                    GridPane.setColumnIndex(this, 2)
                    action {
                        model.name = model.preset.name
                        model.id = model.preset.id.toString()
                        model.useUserData = model.preset.useUserdata
                        model.soundsRate = model.preset.soundsRate
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
                label(messages["appId"]) {
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
                label(messages["path"]) {
                    GridPane.setColumnIndex(this, 0)
                }
                textfield(model.pathProperty) {
                    GridPane.setColumnIndex(this, 1)
                    textProperty().addListener { _, _, _ ->
                        updateOkButton()
                    }
                }
                button(messages["browse..."]) {
                    GridPane.setColumnIndex(this, 2)
                    action {
                        browseForDirectory(messages["path"], model.path)?.let {
                            model.path = it
                        }
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
                        browseForDirectory(messages["cfgPath"], model.cfgPath)?.let {
                            model.cfgPath = it
                        }
                    }
                }
            }
            row {
                label(messages["soundsRate"]) {
                    GridPane.setColumnIndex(this, 0)
                }
                combobox(model.soundsRateProperty, Sound.rates) {
                    GridPane.setColumnIndex(this, 1)
                    hgrow = Priority.ALWAYS
                    maxWidth = Double.MAX_VALUE
                    selectionModel.selectedItemProperty().addListener { _, _, _ -> updateOkButton() }
                }
            }
            row {
                checkbox(messages["useUserdata"], model.useUserdataProperty) {
                    GridPane.setColumnIndex(this, 0)
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
        super.onDock()
        model.success = false
        updateOkButton()
    }

    fun populate(game: Game) {
        model.editing = game
        model.name = game.name
        model.id = game.id.toString()
        model.path = game.path
        model.cfgPath = game.cfgPath
        model.useUserData = game.useUserdata
        if (game.soundsRate in Sound.rates) {
            model.soundsRate = game.soundsRate
        } else {
            model.soundsRate = Sound.rates[0]
        }
    }

    fun clear() {
        model.editing = null
        model.name = ""
        model.id = ""
        model.path = ""
        model.cfgPath = ""
        model.useUserData = false
        model.soundsRate = Sound.rates[0]
    }

    private fun updateOkButton() {
        okButton.isDisable = model.name.isBlank() || model.id.isBlank() || model.path.isBlank() ||
            model.cfgPath.isBlank()
    }

    private fun validate(): Boolean {
        if (model.allGames.filter { it !== model.editing }.any { it.name == model.name }) {
            error(owner = primaryStage, header = messages["nameTakenHeader"],
                content = messages["nameTakenContent"])
            return false
        }
        try {
            Paths.get(model.path)
        } catch (e: Exception) {
            error(owner = primaryStage, header = messages["invalidPathHeader"],
                content = messages["invalidPathContent"])
            return false
        }
        if (!File(model.path).exists()) {
            error(owner = primaryStage, header = messages["pathDoesntExistHeader"],
                content = messages["pathDoesntExistContent"])
            return false
        }
        try {
            Paths.get(model.cfgPath)
        } catch (e: Exception) {
            error(owner = primaryStage, header = messages["invalidCfgPathHeader"],
                content = messages["invalidCfgPathContent"])
            return false
        }
        if (!File(model.cfgPath).exists()) {
            error(owner = primaryStage, header = messages["cfgPathDoesntExistHeader"],
                content = messages["cfgPathDoesntExistContent"])
            return false
        }
        return true
    }
}
