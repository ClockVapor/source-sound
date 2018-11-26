package clockvapor.sourcesound.view.model

import clockvapor.sourcesound.model.Game
import clockvapor.sourcesound.model.Library
import clockvapor.sourcesound.model.Sound
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import javafx.beans.property.*
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import tornadofx.cleanBind
import tornadofx.getValue
import tornadofx.setValue
import java.io.File

@JsonDeserialize(using = RootModel.Deserializer::class)
class RootModel {
    val libraries: ObservableList<Library> = FXCollections.observableArrayList()
    val games: ObservableList<Game> = FXCollections.observableArrayList()

    @JsonIgnore
    val isStartedProperty: BooleanProperty = SimpleBooleanProperty(false)
    // can't do delegated property because @JsonIgnore doesn't work on them
    var isStarted: Boolean
        @JsonIgnore
        get() = isStartedProperty.value
        @JsonIgnore
        set(value) {
            isStartedProperty.value = value
        }

    @JsonIgnore
    val currentLibraryProperty: ObjectProperty<Library?> = SimpleObjectProperty<Library?>(null).apply {
        addListener { _, oldValue, newValue ->
            oldValue?.unloadSounds()
            newValue?.let {
                it.createBaseDirectory()
                it.loadSounds()
            }
            currentLibrarySounds.value = newValue?.sounds ?: FXCollections.emptyObservableList()
        }
    }
    // can't do delegated property because @JsonIgnore doesn't work on them
    var currentLibrary: Library?
        @JsonIgnore
        get() = currentLibraryProperty.value
        @JsonIgnore
        set(value) {
            currentLibraryProperty.value = value
        }

    @JsonIgnore
    val currentLibrarySounds: ListProperty<Sound> = SimpleListProperty(FXCollections.emptyObservableList())

    @JsonIgnore
    val currentGameProperty: ObjectProperty<Game?> = SimpleObjectProperty<Game?>(null).apply {
        addListener { _, _, newValue ->
            currentGameUseUserdataProperty.unbind()
            newValue?.let { game ->
                currentGameUseUserdataProperty.cleanBind(game.useUserdataProperty)
            }
        }
    }
    // can't do delegated property because @JsonIgnore doesn't work on them
    var currentGame: Game?
        @JsonIgnore
        get() = currentGameProperty.value
        @JsonIgnore
        set(value) {
            currentGameProperty.value = value
        }

    @JsonIgnore
    val currentGameUseUserdataProperty: BooleanProperty = SimpleBooleanProperty(false)

    @JsonIgnore
    val userdataPathProperty: StringProperty = SimpleStringProperty("")
    var userdataPath: String by userdataPathProperty

    @JsonIgnore
    val togglePlayKeyProperty: StringProperty = SimpleStringProperty("t")
    var togglePlayKey: String by togglePlayKeyProperty

    @JsonIgnore
    val relayKeyProperty: StringProperty = SimpleStringProperty("KP_END")
    var relayKey: String by relayKeyProperty

    @JsonIgnore
    val ffmpegPathProperty: StringProperty = SimpleStringProperty("")
    var ffmpegPath: String by ffmpegPathProperty

    @JsonIgnore
    var lastNewSoundPath: String? = null

    init {
        libraries.addListener { _: ListChangeListener.Change<out Library> ->
            save()
        }
        games.addListener { _: ListChangeListener.Change<out Game> ->
            save()
        }
        currentLibraryProperty.addListener { _, oldValue, newValue ->
            oldValue?.unloadSounds()
            newValue?.let {
                it.createBaseDirectory()
                it.loadSounds()
            }
            currentLibrarySounds.value = newValue?.sounds ?: FXCollections.emptyObservableList()
        }
        userdataPathProperty.addListener { _, _, _ ->
            save()
        }
        ffmpegPathProperty.addListener { _, _, _ ->
            save()
        }
    }

    fun save() {
        File(MODEL_CONFIG_PATH).writer().use { writer ->
            objectMapper.writeValue(writer, this)
        }
    }

    companion object {
        const val MODEL_CONFIG_PATH = "settings.yml"
        private val objectMapper: ObjectMapper = ObjectMapper(YAMLFactory())

        fun load(): RootModel = try {
            File(MODEL_CONFIG_PATH).reader().use { reader ->
                objectMapper.readValue(reader, RootModel::class.java)
            }
        } catch (e: Exception) {
            RootModel()
        }
    }

    class Deserializer : StdDeserializer<RootModel>(RootModel::class.java) {
        override fun deserialize(parser: JsonParser, context: DeserializationContext): RootModel {
            val rootNode = parser.codec.readTree<JsonNode>(parser)
            val libraries = arrayListOf<Library>()
            rootNode["libraries"]?.let { node ->
                if (node is ArrayNode) {
                    node.mapTo(libraries) {
                        parser.codec.treeToValue(it, Library::class.java)
                    }
                }
            }
            val games = arrayListOf<Game>()
            rootNode["games"]?.let { node ->
                if (node is ArrayNode) {
                    node.mapTo(games) {
                        parser.codec.treeToValue(it, Game::class.java)
                    }
                }
            }
            val togglePlayKey = rootNode["togglePlayKey"]?.asText() ?: ""
            val relayKey = rootNode["relayKey"]?.asText() ?: ""
            val userdataPath = rootNode["userdataPath"]?.asText() ?: ""
            val ffmpegPath = rootNode["ffmpegPath"]?.asText() ?: ""

            return RootModel().also {
                it.libraries += libraries
                it.games += games
                it.togglePlayKey = togglePlayKey
                it.relayKey = relayKey
                it.userdataPath = userdataPath
                it.ffmpegPath = ffmpegPath
            }
        }
    }
}
