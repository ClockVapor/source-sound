package clockvapor.sourcesound.view

import clockvapor.sourcesound.model.Game
import clockvapor.sourcesound.model.GamePreset
import clockvapor.sourcesound.model.Sound
import clockvapor.sourcesound.utils.browseForDirectory
import clockvapor.sourcesound.utils.stringListCell
import clockvapor.sourcesound.utils.validatePathExists
import clockvapor.sourcesound.view.model.GameEditorModel
import javafx.util.Callback
import javafx.util.converter.NumberStringConverter
import tornadofx.*

class GameEditor(model: GameEditorModel) : AbstractEditor<Game, GameEditorModel>(model) {
    override fun Form.fieldSets() {
        fieldset {
            field(messages["preset"]) {
                combobox(model.presetProperty, GamePreset.all) {
                    maxWidth = Double.MAX_VALUE
                    cellFactory = Callback { stringListCell { it.name } }
                    buttonCell = stringListCell { it.name }
                }
                button(messages["apply"]) {
                    disableWhen(model.presetProperty.isNull)
                    action {
                        model.preset?.let { preset ->
                            model.name = preset.name
                            model.id = preset.id
                            model.useUserdata = preset.useUserdata
                            model.soundsRate = preset.soundsRate
                        }
                    }
                }
            }
            field(messages["name"]) {
                textfield(model.nameProperty) {
                    validator {
                        if (text.isBlank()) {
                            error(messages["nameBlank"])
                        } else if (text in model.allGames.filter { it !== model.focus }.map(Game::name)) {
                            error(messages["nameTaken"])
                        } else {
                            success()
                        }
                    }
                }
            }
            field(messages["appId"]) {
                textfield(model.idProperty, NumberStringConverter()) {
                    validator {
                        val i = text.toIntOrNull()
                        when {
                            i == null -> error(messages["appIdNotInteger"])
                            i < 0 -> error(messages["appIdMustBeNonnegative"])
                            else -> success()
                        }
                    }
                }
            }
            field(messages["path"]) {
                textfield(model.pathProperty) {
                    isEditable = false
                    validator(validator = validatePathExists() as ValidationContext.(String?) -> ValidationMessage?)
                }
                button(messages["browse..."]) {
                    action {
                        browseForDirectory(messages["path"], model.path)?.let {
                            model.path = it
                        }
                    }
                }
            }
            field(messages["cfgPath"]) {
                textfield(model.cfgPathProperty) {
                    isEditable = false
                    validator(validator = validatePathExists() as ValidationContext.(String?) -> ValidationMessage?)
                }
                button(messages["browse..."]) {
                    action {
                        browseForDirectory(messages["cfgPath"], model.cfgPath)?.let {
                            model.cfgPath = it
                        }
                    }
                }
            }
            field(messages["soundsRate"]) {
                combobox(model.soundsRateProperty, Sound.rates) {
                    maxWidth = Double.MAX_VALUE
                }
            }
            field(messages["useUserdata"]) {
                checkbox(property = model.useUserdataProperty)
            }
        }
    }

    override fun onDock() {
        super.onDock()
        model.preset = null
    }
}
