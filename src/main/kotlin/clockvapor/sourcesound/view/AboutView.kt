package clockvapor.sourcesound.view

import clockvapor.sourcesound.SourceSound
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import javafx.scene.text.FontWeight
import tornadofx.*

class AboutView : View() {
    override val root = vbox(8.0) {
        paddingAll = 8.0
        alignment = Pos.CENTER
        prefWidth = 250.0
        prefHeight = 200.0
        region { vgrow = Priority.ALWAYS }
        label(SourceSound.TITLE) {
            style {
                fontSize = Dimension(24.0, Dimension.LinearUnits.px)
                fontWeight = FontWeight.BOLD
            }
        }
        label("${messages["version"]} ${SourceSound.resources["version"]}") {
            style {
                fontSize = Dimension(16.0, Dimension.LinearUnits.px)
                fontWeight = FontWeight.BOLD
            }
        }
        region { vgrow = Priority.ALWAYS }
        hyperlink(messages["sourceCode"]) {
            val url = SourceSound.resources["sourceCodeUrl"]
            tooltip(url)
            action {
                hostServices.showDocument(url)
            }
        }
        region { vgrow = Priority.ALWAYS }
    }

    init {
        title = messages["about"]
    }
}
