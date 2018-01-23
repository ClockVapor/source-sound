package clockvapor.sourcesound.view

import clockvapor.sourcesound.view.model.YouTubeImportModel
import javafx.geometry.Pos
import tornadofx.*

class YouTubeImportView : View() {
    val model = YouTubeImportModel()

    override val root = form {
        prefWidth = 400.0
        fieldset {
            field(messages["url"]) {
                textfield(model.urlProperty)
            }
        }
        hbox(8.0) {
            alignment = Pos.CENTER_RIGHT
            button(messages["ok"]) {
                enableWhen(model.urlProperty.isNotBlank())
                action {
                    model.success = true
                    close()
                }
            }
            button(messages["cancel"]) {
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
        model.url = ""
    }
}
