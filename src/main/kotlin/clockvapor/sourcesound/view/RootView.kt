package clockvapor.sourcesound.view

import clockvapor.sourcesound.*
import clockvapor.sourcesound.controller.RootController
import clockvapor.sourcesound.model.RootModel
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.geometry.HPos
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.util.Callback
import org.apache.commons.io.FileUtils
import tornadofx.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.MessageFormat

class RootView : View(SourceSound.TITLE) {
    private val controller: RootController by lazy { RootController(model) }
    private val model: RootModel by lazy { loadModel() }
    private val libraryView: LibraryView = LibraryView(model.libraries)
    private val gameView: GameView = GameView(model.games)
    private val editSoundView: EditSoundView = EditSoundView()
    private val aboutView: AboutView = AboutView()
    private var gamesComboBox: ComboBox<Game?> by singleAssign()
    private var librariesComboBox: ComboBox<Library?> by singleAssign()
    private var soundsTableView: TableView<Sound> by singleAssign()
    private var startButton: Button by singleAssign()
    private var stopButton: Button by singleAssign()
    private var newLibraryButton: Button by singleAssign()
    private var editLibraryButton: Button by singleAssign()
    private var deleteLibraryButton: Button by singleAssign()
    private var newGameButton: Button by singleAssign()
    private var editGameButton: Button by singleAssign()
    private var deleteGameButton: Button by singleAssign()
    private var newSoundButton: Button by singleAssign()
    private var editSoundButton: Button by singleAssign()
    private var refreshSoundsButton: Button by singleAssign()
    private var userdataPathPane: Pane by singleAssign()
    private var controlsPane: Pane by singleAssign()

    override val root = vbox {
        menubar {
            menu(messages["help"]) {
                item(messages["about..."]) {
                    action {
                        aboutDialog()
                    }
                }
            }
        }
        vbox(8.0) {
            vgrow = Priority.ALWAYS
            paddingAll = 8.0
            controlsPane = vbox(8.0) {
                gridpane {
                    vgap = 8.0
                    hgap = 8.0
                    columnConstraints += ColumnConstraints(0.0, GridPane.USE_COMPUTED_SIZE, GridPane.USE_COMPUTED_SIZE,
                        Priority.NEVER, HPos.LEFT, true)
                    columnConstraints += ColumnConstraints(0.0, GridPane.USE_COMPUTED_SIZE, Double.MAX_VALUE,
                        Priority.ALWAYS, HPos.LEFT, true)
                    row {
                        label(messages["game"]) {
                            GridPane.setColumnIndex(this, 0)
                        }
                        gamesComboBox = combobox(model.currentGameProperty, model.games) {
                            GridPane.setColumnIndex(this, 1)
                            maxWidth = Double.MAX_VALUE
                            hgrow = Priority.ALWAYS
                            cellFactory = Callback { stringListCell(Game::name) }
                            buttonCell = stringListCell(Game::name)
                        }
                        newGameButton = button(messages["new"]) {
                            GridPane.setColumnIndex(this, 2)
                            action {
                                createNewGame()
                            }
                        }
                        editGameButton = button(messages["edit"]) {
                            GridPane.setColumnIndex(this, 3)
                            action {
                                editGame()
                            }
                        }
                        deleteGameButton = button(messages["delete"]) {
                            GridPane.setColumnIndex(this, 4)
                            action {
                                deleteGame()
                            }
                        }
                    }
                    row {
                        label(messages["library"]) {
                            GridPane.setColumnIndex(this, 0)
                        }
                        librariesComboBox = combobox(model.currentLibraryProperty, model.libraries) {
                            GridPane.setColumnIndex(this, 1)
                            maxWidth = Double.MAX_VALUE
                            hgrow = Priority.ALWAYS
                            cellFactory = Callback { stringListCell { it.name } }
                            buttonCell = stringListCell { it.name }
                        }
                        newLibraryButton = button(messages["new"]) {
                            GridPane.setColumnIndex(this, 2)
                            action {
                                createNewLibrary()
                            }
                        }
                        editLibraryButton = button(messages["edit"]) {
                            GridPane.setColumnIndex(this, 3)
                            action {
                                editLibrary()
                            }
                        }
                        deleteLibraryButton = button(messages["delete"]) {
                            GridPane.setColumnIndex(this, 4)
                            action {
                                deleteLibrary()
                            }
                        }
                    }
                }
                separator(Orientation.HORIZONTAL)
                vbox(8.0) {
                    vgrow = Priority.ALWAYS
                    soundsTableView = tableview(model.currentLibrarySounds) {
                        vgrow = Priority.ALWAYS
                        columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
                        column(messages["path"], Sound::relativePath)
                    }
                    hbox(8.0) {
                        alignment = Pos.CENTER_RIGHT
                        newSoundButton = button(messages["import"]) {
                            action {
                                newSounds()
                            }
                        }
                        editSoundButton = button(messages["edit"]) {
                            action {
                                soundsTableView.selectedItem?.let(this@RootView::editSound)
                            }
                        }
                        refreshSoundsButton = button(messages["refresh"]) {
                            tooltip(messages["refreshTooltip"])
                            action {
                                model.currentLibrary?.loadSounds()
                            }
                        }
                    }
                    hbox(8.0) {
                        alignment = Pos.CENTER_LEFT
                        label(messages["ffmpegPath"])
                        textfield(model.ffmpegPathProperty) {
                            hgrow = Priority.ALWAYS
                        }
                        button(messages["browse..."]) {
                            action {
                                browseForFile(messages["ffmpegPath"], initial = model.ffmpegPath)?.let {
                                    model.ffmpegPath = it
                                }
                            }
                        }
                    }
                }
                separator(Orientation.HORIZONTAL)
                userdataPathPane = hbox(8.0) {
                    alignment = Pos.CENTER_LEFT
                    label(messages["userdataPath"]) {
                        tooltip(messages["userdataPathTooltip"])
                    }
                    textfield(model.userdataPathProperty) {
                        hgrow = Priority.ALWAYS
                        tooltip(messages["userdataPathTooltip"])
                    }
                    button(messages["browse..."]) {
                        action {
                            browseForDirectory(messages["userdataPath"], model.userdataPath)?.let {
                                model.userdataPath = it
                            }
                        }
                    }
                }
                hbox(16.0) {
                    hbox(8.0) {
                        alignment = Pos.CENTER_LEFT
                        hgrow = Priority.ALWAYS
                        label(messages["togglePlayKey"]) {
                            tooltip(messages["togglePlayKeyTooltip"])
                        }
                        textfield(model.togglePlayKeyProperty) {
                            hgrow = Priority.ALWAYS
                            tooltip(messages["togglePlayKeyTooltip"])
                        }
                    }
                    hbox(8.0) {
                        alignment = Pos.CENTER_LEFT
                        hgrow = Priority.ALWAYS
                        label(messages["relayKey"]) {
                            tooltip(messages["relayKeyTooltip"])
                        }
                        textfield(model.relayKeyProperty) {
                            hgrow = Priority.ALWAYS
                            tooltip(messages["relayKeyTooltip"])
                        }
                    }
                }
            }
            hbox(8.0) {
                alignment = Pos.CENTER_RIGHT
                startButton = button(messages["start"]) {
                    action {
                        model.apply {
                            currentLibrary!!.start(model.currentGame!!, model.userdataPath, togglePlayKey, relayKey)
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
    }

    init {
        model.apply {
            isStartedProperty.addListener { _, _, _ ->
                updateStartButton()
                updateStopButton()
                updateControlsPane()
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
                updateNewSoundButton()
                updateRefreshSoundsButton()
                updateEditLibraryButton()
                updateDeleteLibraryButton()
            }
            currentGameProperty.addListener { _, _, _ ->
                updateStartButton()
                updateEditGameButton()
                updateDeleteGameButton()
                updateUserdataPathPane()
            }
            togglePlayKeyProperty.addListener { _, _, _ ->
                updateStartButton()
            }
            relayKeyProperty.addListener { _, _, _ ->
                updateStartButton()
            }
            userdataPathProperty.addListener { _, _, _ ->
                updateStartButton()
            }
            ffmpegPathProperty.addListener { _, _, _ ->
                updateNewSoundButton()
                updateEditSoundButton()
            }
        }
        soundsTableView.selectionModel.apply {
            selectionMode = SelectionMode.SINGLE
            selectedItemProperty().addListener { _, _, _ ->
                updateEditSoundButton()
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
        updateNewSoundButton()
        updateEditSoundButton()
        updateUserdataPathPane()
        updateControlsPane()
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
            val game = Game(gameView.model.name, gameView.model.id.toInt(), gameView.model.path,
                gameView.model.cfgPath, gameView.model.useUserData)
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
            game.useUserdata = gameView.model.useUserData
            saveModel()
        }
    }

    private fun deleteGame() {
        model.currentGame?.let { game ->
            confirmDialog(MessageFormat.format(messages["confirmDeleteGame"], game.name))?.let { result ->
                if (result == ButtonType.OK) {
                    model.games -= game
                    gamesComboBox.selectionModel.selectFirst()
                }
            }
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
            confirmDialog(MessageFormat.format(messages["confirmDeleteLibrary"], library.name))?.let { result ->
                if (result == ButtonType.OK) {
                    model.libraries -= library
                    librariesComboBox.selectionModel.selectFirst()
                }
            }
        }
    }

    private fun newSounds() {
        model.currentLibrary!!.let { library ->
            var destination: String?
            do {
                destination = browseForDirectory(messages["selectImportPath"], library.directory)
                if (destination == null) {
                    return
                }
                if (File(destination).absolutePath != library.directoryFile.absolutePath &&
                    !FileUtils.directoryContains(library.directoryFile, File(destination))) {
                    error(messages["importPathNotInLibraryHeader"],
                        messages["importPathNotInLibraryContent"], owner = primaryStage)
                    destination = null
                }
            } while (destination == null)

            browseForFiles(messages["selectSounds"],
                FileChooser.ExtensionFilter(messages["sound"], Sound.importableExtensions),
                model.lastNewSoundPath)?.let { paths ->

                model.lastNewSoundPath = paths[0]
                root.isDisable = true
                runAsync {
                    controller.importSounds(paths, destination)
                } ui {
                    alert(Alert.AlertType.INFORMATION, messages["success"], content = messages["importSuccess"],
                        title = messages["success"], owner = primaryStage)
                    root.isDisable = false
                    library.loadSounds()
                } fail { e ->
                    e.printStackTrace()
                    error(messages["importFailed"], e.toString(), owner = primaryStage)
                    root.isDisable = false
                }
            }
        }
    }

    private fun editSound(sound: Sound) {
        editSoundView.initialize(model.ffmpegPath, sound)
        editSoundView.openModal(modality = Modality.WINDOW_MODAL, owner = currentStage, block = true)
        editSoundView.dispose()
    }

    private fun aboutDialog() {
        aboutView.openModal(modality = Modality.WINDOW_MODAL, owner = currentStage, block = true)
    }

    private fun updateStartButton() {
        startButton.isDisable = model.currentGame?.let { game ->
            model.currentLibrary == null || model.isStarted ||
                model.togglePlayKey.isBlank() || model.relayKey.isBlank() ||
                (game.useUserdata && model.userdataPath.isBlank())
        } ?: true
    }

    private fun updateStopButton() {
        stopButton.isDisable = model.currentLibrary == null || !model.isStarted
    }

    private fun updateRefreshSoundsButton() {
        refreshSoundsButton.isDisable = model.currentLibrary == null
    }

    private fun updateEditGameButton() {
        editGameButton.isDisable = model.currentGame == null
    }

    private fun updateDeleteGameButton() {
        deleteGameButton.isDisable = model.currentGame == null
    }

    private fun updateEditLibraryButton() {
        editLibraryButton.isDisable = model.currentLibrary == null
    }

    private fun updateDeleteLibraryButton() {
        deleteLibraryButton.isDisable = model.currentLibrary == null
    }

    private fun updateNewSoundButton() {
        newSoundButton.isDisable = model.currentLibrary == null || model.ffmpegPath.isBlank()
    }

    private fun updateEditSoundButton() {
        editSoundButton.isDisable = soundsTableView.selectionModel.selectedItem == null || model.ffmpegPath.isBlank()
    }

    private fun updateUserdataPathPane() {
        userdataPathPane.isDisable = model.currentGame?.let { !it.useUserdata } ?: false
    }

    private fun updateControlsPane() {
        controlsPane.isDisable = model.isStarted
    }

    companion object {
        const val MODEL_CONFIG_PATH = "settings.yml"

        private val objectMapper: ObjectMapper = ObjectMapper(YAMLFactory())
    }
}
