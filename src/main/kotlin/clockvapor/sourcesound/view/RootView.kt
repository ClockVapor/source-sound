package clockvapor.sourcesound.view

import clockvapor.sourcesound.*
import clockvapor.sourcesound.model.RootModel
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.TableView
import javafx.scene.layout.Priority
import javafx.stage.Modality
import javafx.util.Callback
import tornadofx.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class RootView : View(SourceSound.TITLE) {
    private val model: RootModel by lazy { loadModel() }
    private val libraryView: LibraryView = LibraryView(model.libraries)
    private val gameView: GameView = GameView(model.games)
    private var gamesComboBox: ComboBox<Game?> by singleAssign()
    private var librariesComboBox: ComboBox<Library?> by singleAssign()
    private var startButton: Button by singleAssign()
    private var stopButton: Button by singleAssign()
    private var newLibraryButton: Button by singleAssign()
    private var editLibraryButton: Button by singleAssign()
    private var deleteLibraryButton: Button by singleAssign()
    private var newGameButton: Button by singleAssign()
    private var editGameButton: Button by singleAssign()
    private var deleteGameButton: Button by singleAssign()
    private var refreshSoundsButton: Button by singleAssign()

    override val root = vbox(8.0) {
        paddingAll = 8.0
        hbox(8.0) {
            alignment = Pos.CENTER_LEFT
            label(messages["game"])
            gamesComboBox = combobox(model.currentGameProperty, model.games) {
                maxWidth = Double.MAX_VALUE
                hgrow = Priority.ALWAYS
                cellFactory = Callback { stringListCell(Game::name) }
                buttonCell = stringListCell(Game::name)
            }
            newGameButton = button(messages["new"]) {
                action {
                    createNewGame()
                }
            }
            editGameButton = button(messages["edit"]) {
                action {
                    editGame()
                }
            }
            deleteGameButton = button(messages["delete"]) {
                action {
                    deleteGame()
                }
            }
        }
        hbox(8.0) {
            alignment = Pos.CENTER_LEFT
            label(messages["library"])
            librariesComboBox = combobox(model.currentLibraryProperty, model.libraries) {
                maxWidth = Double.MAX_VALUE
                hgrow = Priority.ALWAYS
                cellFactory = Callback { stringListCell { it.name } }
                buttonCell = stringListCell { it.name }
            }
            newLibraryButton = button(messages["new"]) {
                action {
                    createNewLibrary()
                }
            }
            editLibraryButton = button(messages["edit"]) {
                action {
                    editLibrary()
                }
            }
            deleteLibraryButton = button(messages["delete"]) {
                action {
                    deleteLibrary()
                }
            }
        }
        tableview(model.currentLibrarySounds) {
            columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
            column(messages["path"], Sound::relativePath)
        }
        hbox(8.0) {
            alignment = Pos.CENTER_RIGHT
            refreshSoundsButton = button(messages["refresh"]) {
                tooltip(messages["refreshTooltip"])
                action {
                    model.currentLibrary?.loadSounds()
                }
            }
        }
        hbox(16.0) {
            hbox(8.0) {
                alignment = Pos.CENTER_LEFT
                label(messages["togglePlayKey"]) {
                    tooltip(messages["togglePlayKeyTooltip"])
                }
                textfield(model.togglePlayKeyProperty) {
                    tooltip(messages["togglePlayKeyTooltip"])
                }
            }
            hbox(8.0) {
                alignment = Pos.CENTER_LEFT
                label(messages["relayKey"]) {
                    tooltip(messages["relayKeyTooltip"])
                }
                textfield(model.relayKeyProperty) {
                    tooltip(messages["relayKeyTooltip"])
                }
            }
        }
        hbox(8.0) {
            alignment = Pos.CENTER_RIGHT
            startButton = button(messages["start"]) {
                action {
                    model.apply {
                        currentLibrary!!.start(model.currentGame!!, togglePlayKey, relayKey)
                        isStarted = true
                    }
                }
            }
            stopButton = button(messages["stop"]) {
                action {
                    model.apply {
                        currentLibrary!!.stop()
                        isStarted = false
                    }
                }
            }
        }
    }

    init {
        model.apply {
            isStartedProperty.addListener { _, _, _ ->
                updateGameAndLibraryControls()
                updateStartButton()
                updateStopButton()
                updateRefreshSoundsButton()
                updateEditGameButton()
                updateDeleteGameButton()
                updateEditLibraryButton()
                updateDeleteLibraryButton()
            }
            libraries.addListener { _: ListChangeListener.Change<out Library> ->
                saveModel()
            }
            games.addListener { _: ListChangeListener.Change<out Game> ->
                saveModel()
            }
            currentLibraryProperty.addListener { _, oldValue, newValue ->
                oldValue?.unloadSounds()
                newValue?.let {
                    it.createSoundsDirectory()
                    it.loadSounds()
                }
                currentLibrarySounds.value = newValue?.sounds ?: FXCollections.emptyObservableList()
                updateStartButton()
                updateStopButton()
                updateRefreshSoundsButton()
                updateEditLibraryButton()
                updateDeleteLibraryButton()
            }
            currentGameProperty.addListener { _, _, _ ->
                updateEditGameButton()
                updateDeleteGameButton()
            }
            togglePlayKeyProperty.addListener { _, _, _ ->
                updateStartButton()
            }
            relayKeyProperty.addListener { _, _, _ ->
                updateStartButton()
            }
        }
    }

    override fun onDock() {
        super.onDock()
        updateStartButton()
        updateStopButton()
        updateRefreshSoundsButton()
        updateEditGameButton()
        updateDeleteGameButton()
        updateEditLibraryButton()
        updateDeleteLibraryButton()
        gamesComboBox.selectionModel.selectFirst()
        librariesComboBox.selectionModel.selectFirst()
    }

    override fun onUndock() {
        super.onUndock()
        saveModel()
        model.currentLibrary?.stop()
    }

    private fun loadModel(): RootModel = try {
        FileInputStream(File(MODEL_CONFIG_PATH)).use { stream ->
            objectMapper.readValue(stream, RootModel::class.java)
        }
    } catch (e: Exception) {
        RootModel()
    }

    private fun saveModel() {
        FileOutputStream(File(MODEL_CONFIG_PATH)).use { stream ->
            objectMapper.writeValue(stream, model)
        }
    }

    private fun createNewLibrary() {
        libraryView.clear()
        libraryView.openModal(modality = Modality.WINDOW_MODAL, owner = currentStage, block = true)
        if (libraryView.model.success) {
            val library = Library(
                libraryView.model.name,
                libraryView.model.rate.toInt()
            )
            model.libraries += library
            model.currentLibrary = library
        }
    }

    private fun createNewGame() {
        gameView.clear()
        gameView.openModal(modality = Modality.WINDOW_MODAL, owner = currentStage, block = true)
        if (gameView.model.success) {
            val game = Game(gameView.model.name, gameView.model.id.toInt(), gameView.model.cfgPath)
            model.games += game
            model.currentGame = game
        }
    }

    private fun editGame() {
        model.currentGame?.let { game ->
            gameView.populate(game)
            gameView.openModal(modality = Modality.WINDOW_MODAL, owner = currentStage, block = true)
            game.name = gameView.model.name
            game.id = gameView.model.id.toInt()
            game.cfgPath = gameView.model.cfgPath
            saveModel()
        }
    }

    private fun deleteGame() {
        model.currentGame?.let { game ->
            model.games -= game
            gamesComboBox.selectionModel.selectFirst()
        }
    }

    private fun editLibrary() {
        model.currentLibrary?.let { library ->
            libraryView.populate(library)
            libraryView.openModal(modality = Modality.WINDOW_MODAL, owner = currentStage, block = true)
            library.name = libraryView.model.name
            library.rate = libraryView.model.rate.toInt()
            saveModel()
        }
    }

    private fun deleteLibrary() {
        model.currentLibrary?.let { library ->
            model.libraries -= library
            librariesComboBox.selectionModel.selectFirst()
        }
    }

    private fun updateGameAndLibraryControls() {
        gamesComboBox.isDisable = model.isStarted
        librariesComboBox.isDisable = model.isStarted
        newGameButton.isDisable = model.isStarted
        newLibraryButton.isDisable = model.isStarted
    }

    private fun updateStartButton() {
        startButton.isDisable = model.currentLibrary == null || model.isStarted || model.togglePlayKey.isBlank() ||
            model.relayKey.isBlank()
    }

    private fun updateStopButton() {
        stopButton.isDisable = model.currentLibrary == null || !model.isStarted
    }

    private fun updateRefreshSoundsButton() {
        refreshSoundsButton.isDisable = model.currentLibrary == null || model.isStarted
    }

    private fun updateEditGameButton() {
        editGameButton.isDisable = model.currentGame == null || model.isStarted
    }

    private fun updateDeleteGameButton() {
        deleteGameButton.isDisable = model.currentGame == null || model.isStarted
    }

    private fun updateEditLibraryButton() {
        editLibraryButton.isDisable = model.currentLibrary == null || model.isStarted
    }

    private fun updateDeleteLibraryButton() {
        deleteLibraryButton.isDisable = model.currentLibrary == null || model.isStarted
    }

    companion object {
        const val MODEL_CONFIG_PATH = "settings.yml"

        private val objectMapper: ObjectMapper = ObjectMapper(YAMLFactory())
    }
}
