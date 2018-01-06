package clockvapor.sourcesound

import clockvapor.sourcesound.view.RootView
import javafx.application.Application
import tornadofx.*

class SourceSound : App(RootView::class) {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Application.launch(SourceSound::class.java, *args)
        }
    }
}
