package clockvapor.sourcesound.view

import clockvapor.sourcesound.Library
import clockvapor.sourcesound.Sound
import clockvapor.sourcesound.model.RootModel
import clockvapor.sourcesound.stringListCell
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

class RootView : View() {
    private val model: RootModel by lazy { loadModel() }
    private val newLibraryView: NewLibraryView by inject()
    private var librariesComboBox: ComboBox<Library?> by singleAssign()
    private var startButton: Button by singleAssign()
    private var stopButton: Button by singleAssign()

    override val root = vbox(8.0) {
        paddingAll = 8.0
        hbox(8.0) {
            alignment = Pos.CENTER_LEFT
            label(messages["library"])
            librariesComboBox = combobox(model.currentLibraryProperty, model.libraries) {
                maxWidth = Double.MAX_VALUE
                hgrow = Priority.ALWAYS
                cellFactory = Callback { stringListCell { it.soundsPath } }
                buttonCell = stringListCell { it.soundsPath }
            }
            button(messages["new"]) {
                action {
                    createNewLibrary()
                }
            }
        }
        tableview(model.currentLibrarySounds) {
            columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
            column(messages["path"], Sound::relativePath)
        }
        hbox(8.0) {
            startButton = button(messages["start"]) {
                action {
                    model.apply {
                        currentLibrary!!.start(togglePlayKey, relayKey)
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
        title = messages["title"]
        model.apply {
            isStartedProperty.addListener { _, _, _ ->
                updateStartButton()
                updateStopButton()
            }
            libraries.addListener { _: ListChangeListener.Change<out Library> ->
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
            }
        }
    }

    override fun onDock() {
        super.onDock()
        updateStartButton()
        updateStopButton()
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
        newLibraryView.openModal(modality = Modality.WINDOW_MODAL, owner = currentStage, block = true)
        if (newLibraryView.success) {
            val library = Library(newLibraryView.soundsPath.value, newLibraryView.soundsRate.value as Int,
                newLibraryView.cfgPath.value)
            model.libraries += library
            model.currentLibrary = library
        }
    }

    private fun updateStartButton() {
        model.currentLibrary.let {
            startButton.isDisable = it == null || model.isStarted
        }
    }

    private fun updateStopButton() {
        model.currentLibrary.let {
            stopButton.isDisable = it == null || !model.isStarted
        }
    }

    companion object {
        const val LIBRARIES_CONFIG_PATH = "libraries.yml"
        const val MODEL_CONFIG_PATH = "settings.yml"

        private val objectMapper: ObjectMapper = ObjectMapper(YAMLFactory())
    }
}
