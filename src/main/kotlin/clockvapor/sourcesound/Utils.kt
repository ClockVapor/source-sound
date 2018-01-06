package clockvapor.sourcesound

import javafx.scene.control.ListCell
import javafx.stage.DirectoryChooser
import javafx.stage.Window
import java.io.File

fun <T> stringListCell(toString: (T) -> String): ListCell<T?> {
    return object : ListCell<T?>() {
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
}

fun browseForDirectory(owner: Window?, title: String, initial: String? = null): String? {
    val directoryChooser = DirectoryChooser()
    if (!initial.isNullOrBlank()) {
        directoryChooser.initialDirectory = File(initial)
    }
    directoryChooser.title = title
    return directoryChooser.showDialog(owner)?.absolutePath
}
