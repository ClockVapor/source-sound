package clockvapor.sourcesound.utils

import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.control.ListCell
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Modality
import tornadofx.*
import java.io.File
import java.nio.file.Paths

inline fun Boolean.alsoIfTrue(op: () -> Unit): Boolean {
    if (this) op()
    return this
}

fun <T> stringListCell(toString: (T) -> String): ListCell<T?> = object : ListCell<T?>() {
    override fun updateItem(item: T?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            text = null
            graphic = null
        } else {
            text = toString(item)
        }
    }
}

fun View.browseForFile(title: String, extensionFilter: FileChooser.ExtensionFilter? = null,
                       initial: String? = null): String? {
    val fileChooser = FileChooser()
    if (!initial.isNullOrBlank()) {
        fileChooser.initialDirectory = File(initial).parentFile
    }
    fileChooser.title = title
    if (extensionFilter != null) {
        fileChooser.extensionFilters += extensionFilter
        fileChooser.selectedExtensionFilter = extensionFilter
    }
    var path: String?
    try {
        path = fileChooser.showOpenDialog(primaryStage)?.absolutePath
    } catch (e: Exception) {
        fileChooser.initialDirectory = null
        path = fileChooser.showOpenDialog(primaryStage)?.absolutePath
    }
    return path
}

fun View.browseForFiles(title: String, extensionFilter: FileChooser.ExtensionFilter? = null,
                        initial: String? = null): List<String>? {
    val fileChooser = FileChooser()
    if (!initial.isNullOrBlank()) {
        fileChooser.initialDirectory = File(initial).parentFile
    }
    fileChooser.title = title
    if (extensionFilter != null) {
        fileChooser.extensionFilters += extensionFilter
        fileChooser.selectedExtensionFilter = extensionFilter
    }
    var files: List<String>?
    try {
        files = fileChooser.showOpenMultipleDialog(primaryStage)?.let { list -> list.map { it.absolutePath } }
    } catch (e: Exception) {
        fileChooser.initialDirectory = null
        files = fileChooser.showOpenMultipleDialog(primaryStage)?.let { list -> list.map { it.absolutePath } }
    }
    return files
}

fun View.browseForDirectory(title: String, initial: String? = null): String? {
    val directoryChooser = DirectoryChooser()
    if (!initial.isNullOrBlank()) {
        directoryChooser.initialDirectory = File(initial)
    }
    directoryChooser.title = title
    var path: String?
    try {
        path = directoryChooser.showDialog(primaryStage)?.absolutePath
    } catch (e: Exception) {
        directoryChooser.initialDirectory = null
        path = directoryChooser.showDialog(primaryStage)?.absolutePath
    }
    return path
}

fun View.confirmDialog(content: String): ButtonType? {
    val alert = Alert(Alert.AlertType.CONFIRMATION)
    alert.title = messages["confirm"]
    alert.headerText = messages["confirm"]
    alert.contentText = content
    alert.initOwner(primaryStage)
    alert.initModality(Modality.WINDOW_MODAL)
    return alert.showAndWait().orElse(null)
}

val View.pathExistsValidator: ValidationContext.(String?) -> ValidationMessage?
    get() = {
        try {
            Paths.get(it)
            if (File(it).exists()) {
                success()
            } else {
                error(messages["pathDoesntExist"])
            }
        } catch (e: Exception) {
            error(messages["invalidPath"])
        }
    }
