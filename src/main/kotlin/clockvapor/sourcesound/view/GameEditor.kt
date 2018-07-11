package clockvapor.sourcesound.view

import clockvapor.sourcesound.model.Game
import clockvapor.sourcesound.model.GamePreset
import clockvapor.sourcesound.model.Sound
import clockvapor.sourcesound.utils.browseForDirectory
import clockvapor.sourcesound.utils.pathExistsValidator
import clockvapor.sourcesound.utils.stringListCell
import clockvapor.sourcesound.view.model.GameEditorModel
import javafx.collections.ObservableList
import javafx.util.Callback
import javafx.util.converter.NumberStringConverter
import tornadofx.*
import java.text.MessageFormat

class GameEditor(allGames: ObservableList<Game>) : AbstractEditor<Game, GameEditorModel>(GameEditorModel(allGames)) {
    override fun Form.fieldSets() {
        fieldset {
            field(messages["preset"]) {
                tooltip(MessageFormat.format(messages["presetTooltip"], messages["apply"]))
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
            separator()
            field(messages["name"]) {
                tooltip(messages["nameTooltip"])
                textfield(model.nameProperty) {
                    validator {
                        when {
                            it == null -> error(messages["nameNull"])
                            it.isBlank() -> error(messages["nameBlank"])
                            it in model.allGames.asSequence().filter { it !== model.focus }.map(Game::name) ->
                                error(messages["nameTaken"])
                            else -> success()
                        }
                    }
                }
            }
            field(messages["appId"]) {
                tooltip(messages["appIdTooltip"])
                textfield(model.idProperty, NumberStringConverter()) {
                    validator {
                        val i = it?.toIntOrNull()
                        when {
                            i == null -> error(messages["appIdNotInteger"])
                            i < 0 -> error(messages["appIdMustBeNonnegative"])
                            else -> success()
                        }
                    }
                }
            }
            field(messages["path"]) {
                tooltip(messages["pathTooltip"])
                textfield(model.pathProperty) {
                    isEditable = false
                    validator(validator = pathExistsValidator)
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
                tooltip(messages["cfgPathTooltip"])
                textfield(model.cfgPathProperty) {
                    isEditable = false
                    validator(validator = pathExistsValidator)
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
                tooltip(messages["soundsRateTooltip"])
                combobox(model.soundsRateProperty, Sound.rates) {
                    maxWidth = Double.MAX_VALUE
                }
            }
            field(messages["useUserdata"]) {
                tooltip(messages["useUserdataTooltip"])
                checkbox(property = model.useUserdataProperty)
            }
        }
    }

    override fun onDock() {
        super.onDock()
        model.preset = null
    }
}
