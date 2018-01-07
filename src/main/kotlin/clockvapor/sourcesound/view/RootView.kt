package clockvapor.sourcesound.view

import clockvapor.sourcesound.Library
import clockvapor.sourcesound.Sound
import clockvapor.sourcesound.stringListCell
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import javafx.beans.property.ListProperty
import javafx.beans.property.Property
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.geometry.Pos
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
    private val newLibraryView: NewLibraryView by inject()
    private var librariesComboBox: ComboBox<Library?> by singleAssign()

    private val libraries: ObservableList<Library> = FXCollections.observableArrayList<Library>().apply {
        addListener { _: ListChangeListener.Change<out Library> ->
            saveLibraries()
        }
    }

    private val currentLibraryProperty: Property<Library?> = SimpleObjectProperty<Library?>(null).apply {
        addListener { _, oldValue, newValue ->
            oldValue?.unloadSounds()
            newValue?.let {
                it.createDirectory()
                it.loadSounds()
            }
            currentLibrarySounds.value = newValue?.sounds ?: FXCollections.emptyObservableList()
        }
    }
    private var currentLibrary: Library? by currentLibraryProperty
    private val currentLibrarySounds: ListProperty<Sound> = SimpleListProperty(FXCollections.emptyObservableList())

    override val root = vbox(8.0) {
        paddingAll = 8.0
        hbox(8.0) {
            alignment = Pos.CENTER_LEFT
            label(messages["library"])
            librariesComboBox = combobox(currentLibraryProperty, libraries) {
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
        tableview(currentLibrarySounds) {
            columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
            column(messages["path"], Sound::relativePath)
        }
    }

    init {
        title = messages["title"]
    }

    override fun onDock() {
        super.onDock()
        loadLibraries()
    }

    override fun onUndock() {
        super.onUndock()
        saveLibraries()
    }

    private fun saveLibraries() {
        val mapper = ObjectMapper(YAMLFactory())
        FileOutputStream(File(LIBRARIES_CONFIG_PATH)).use {
            mapper.writeValue(it, libraries)
        }
    }

    private fun loadLibraries() {
        try {
            val mapper = ObjectMapper(YAMLFactory())
            FileInputStream(File(LIBRARIES_CONFIG_PATH)).use { fileInputStream ->
                mapper.readValue(fileInputStream, List::class.java).forEach { item ->
                    if (item is Map<*, *>) {
                        libraries += Library.fromMap(item)
                    } else {
                        error(header = messages["loadLibrariesError"], content = messages["invalidLibrary"],
                            owner = primaryStage)
                    }
                }
            }
        } catch (e: Exception) {
            error(header = messages["loadLibrariesError"], content = e.localizedMessage, owner = primaryStage)
        }
    }

    private fun createNewLibrary() {
        newLibraryView.openModal(modality = Modality.WINDOW_MODAL, owner = currentStage, block = true)
        if (newLibraryView.success) {
            val library = Library(newLibraryView.soundsPath.value, newLibraryView.soundsRate.value as Int,
                newLibraryView.cfgPath.value)
            libraries += library
            currentLibrary = library
        }
    }

    companion object {
        const val LIBRARIES_CONFIG_PATH = "libraries.yml"
    }
}
