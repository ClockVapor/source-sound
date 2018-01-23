package clockvapor.sourcesound.view

import clockvapor.sourcesound.SourceSound
import clockvapor.sourcesound.controller.RootController
import clockvapor.sourcesound.model.Game
import clockvapor.sourcesound.model.Library
import clockvapor.sourcesound.model.Sound
import clockvapor.sourcesound.utils.*
import clockvapor.sourcesound.view.model.RootModel
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import javafx.beans.binding.Bindings
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.geometry.HPos
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
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
    private val libraryEditor: LibraryEditor = LibraryEditor(model.libraries)
    private val gameEditor: GameEditor by lazy { GameEditor(model.games) }
    private val soundEditor: SoundEditor = SoundEditor()
    private val aboutView: AboutView = AboutView()
    private var gamesComboBox: ComboBox<Game?> by singleAssign()
    private var librariesComboBox: ComboBox<Library?> by singleAssign()
    private var soundsTableView: TableView<Sound> by singleAssign()

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
            vbox(8.0) {
                disableProperty().bind(model.isStartedProperty)
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
                        button(messages["new"]) {
                            GridPane.setColumnIndex(this, 2)
                            action {
                                newGame()
                            }
                        }
                        button(messages["edit"]) {
                            GridPane.setColumnIndex(this, 3)
                            disableWhen(Bindings.isNull(model.currentGameProperty))
                            action {
                                model.currentGame?.let {
                                    editGame(it)
                                }
                            }
                        }
                        button(messages["delete"]) {
                            GridPane.setColumnIndex(this, 4)
                            disableWhen(Bindings.isNull(model.currentGameProperty))
                            action {
                                deleteGame()
                            }
                        }
                    }
                    row {
                        label(messages["library"]) {
                            GridPane.setColumnIndex(this, 0)
                        }
                        librariesComboBox = combobox(model.currentLibraryProperty, model.filteredLibraries) {
                            GridPane.setColumnIndex(this, 1)
                            maxWidth = Double.MAX_VALUE
                            hgrow = Priority.ALWAYS
                            cellFactory = Callback { stringListCell { it.name } }
                            buttonCell = stringListCell { it.name }
                        }
                        button(messages["new"]) {
                            GridPane.setColumnIndex(this, 2)
                            action {
                                newLibrary()
                            }
                        }
                        button(messages["edit"]) {
                            GridPane.setColumnIndex(this, 3)
                            disableWhen(Bindings.isNull(model.currentLibraryProperty))
                            action {
                                model.currentLibrary?.let {
                                    editLibrary(it)
                                }
                            }
                        }
                        button(messages["delete"]) {
                            GridPane.setColumnIndex(this, 4)
                            disableWhen(Bindings.isNull(model.currentLibraryProperty))
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
                        button(messages["import"]) {
                            disableWhen(
                                Bindings.isNull(model.currentLibraryProperty)
                                    .or(model.ffmpegPathProperty.isBlank())
                            )
                            action {
                                newSounds()
                            }
                        }
                        button(messages["edit"]) {
                            disableWhen(
                                Bindings.isNull(soundsTableView.selectionModel.selectedItemProperty())
                                    .or(model.ffmpegPathProperty.isBlank())
                            )
                            action {
                                soundsTableView.selectedItem?.let(this@RootView::editSound)
                            }
                        }
                        button(messages["refresh"]) {
                            disableWhen(Bindings.isNull(model.currentLibraryProperty))
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
                hbox(8.0) {
                    alignment = Pos.CENTER_LEFT
                    disableProperty().bind(model.currentGameUseUserdataProperty.not())
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
                button(messages["start"]) {
                    disableWhen(
                        Bindings.isNull(model.currentGameProperty)
                            .or(Bindings.isNull(model.currentLibraryProperty))
                            .or(model.isStartedProperty)
                            .or(model.togglePlayKeyProperty.isBlank())
                            .or(model.relayKeyProperty.isBlank())
                            .or(model.currentGameUseUserdataProperty.and(model.userdataPathProperty.isBlank()))
                    )
                    action {
                        model.apply {
                            currentLibrary!!.start(model.currentGame!!, model.userdataPath, togglePlayKey, relayKey)
                            isStarted = true
                        }
                    }
                }
                button(messages["stop"]) {
                    disableWhen(
                        Bindings.isNull(model.currentGameProperty)
                            .or(model.isStartedProperty.not())
                    )
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
            libraries.addListener { _: ListChangeListener.Change<out Library> ->
                saveModel()
            }
            filteredLibraries.addListener { _: ListChangeListener.Change<out Library> ->
                if (currentLibrary !in filteredLibraries) {
                    currentLibrary = filteredLibraries.firstOrNull()
                }
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
            }
            currentGameProperty.addListener { _, _, _ ->
                refreshFilteredLibraries()
            }
            refreshFilteredLibraries()
        }
        soundsTableView.selectionModel.selectionMode = SelectionMode.SINGLE
    }

    override fun onDock() {
        super.onDock()
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

    private fun newLibrary() {
        Library().let { library ->
            if (editLibrary(library)) {
                model.apply {
                    libraries += library
                    currentLibrary = library
                }
            }
        }
    }

    private fun editLibrary(library: Library): Boolean {
        libraryEditor.model.focus = library
        libraryEditor.openModal(modality = Modality.WINDOW_MODAL, owner = primaryStage, block = true)
        val success = libraryEditor.model.success
        if (success) {
            saveModel()
            model.refreshFilteredLibraries()
        }
        return success
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

    private fun newGame() {
        val game = Game()
        if (editGame(game)) {
            model.apply {
                games += game
                currentGame = game
            }
        }
    }

    private fun editGame(game: Game): Boolean {
        gameEditor.model.focus = game
        gameEditor.openModal(modality = Modality.WINDOW_MODAL, owner = primaryStage, block = true)
        val success = gameEditor.model.success
        if (success) {
            saveModel()
            model.refreshFilteredLibraries()
        }
        return success
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
        soundEditor.initialize(model.ffmpegPath, sound)
        soundEditor.openModal(modality = Modality.WINDOW_MODAL, owner = currentStage, block = true)
        soundEditor.dispose()
    }

    private fun aboutDialog() {
        aboutView.openModal(modality = Modality.WINDOW_MODAL, owner = currentStage, block = true)
    }

    companion object {
        const val MODEL_CONFIG_PATH = "settings.yml"

        private val objectMapper: ObjectMapper = ObjectMapper(YAMLFactory())
    }
}
