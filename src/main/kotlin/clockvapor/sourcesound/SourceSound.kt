package clockvapor.sourcesound

import clockvapor.sourcesound.view.RootView
import javafx.application.Application
import tornadofx.*
import java.util.*

class SourceSound : App(RootView::class) {
    companion object {
        const val TITLE = "SourceSound"
        val resources: ResourceBundle = ResourceBundle.getBundle(TITLE)

        @JvmStatic
        fun main(args: Array<String>) {
            Application.launch(SourceSound::class.java, *args)
        }
    }
}
