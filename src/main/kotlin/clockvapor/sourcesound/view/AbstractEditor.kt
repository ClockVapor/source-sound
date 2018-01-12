package clockvapor.sourcesound.view

import clockvapor.sourcesound.view.model.AbstractEditorModel
import javafx.geometry.Pos
import tornadofx.*

abstract class AbstractEditor<T, out M : AbstractEditorModel<T>>(val model: M) : View() {
    final override val root = form {
        fieldSets()
        hbox(8.0) {
            alignment = Pos.CENTER_RIGHT
            button(messages["ok"]) {
                enableWhen(model::valid)
                action {
                    model.commit {
                        model.success = true
                        close()
                    }
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

    protected abstract fun Form.fieldSets()

    init {
        title = messages["title"]
    }

    override fun onDock() {
        super.onDock()
        model.success = false
        model.rollback()
    }
}
