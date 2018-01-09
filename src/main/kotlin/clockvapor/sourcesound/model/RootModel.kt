package clockvapor.sourcesound.model

import clockvapor.sourcesound.Game
import clockvapor.sourcesound.Library
import clockvapor.sourcesound.Sound
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ArrayNode
import javafx.beans.property.*
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import tornadofx.*

@JsonDeserialize(using = RootModel.Deserializer::class)
class RootModel {
    val libraries: ObservableList<Library> = FXCollections.observableArrayList()
    val games: ObservableList<Game> = FXCollections.observableArrayList()

    @JsonIgnore
    val isStartedProperty: Property<Boolean> = SimpleBooleanProperty(false)
    // can't do delegated property because @JsonIgnore doesn't work on them
    var isStarted: Boolean
        @JsonIgnore
        get() = isStartedProperty.value
        @JsonIgnore
        set(value) {
            isStartedProperty.value = value
        }

    @JsonIgnore
    val currentLibraryProperty: Property<Library?> = SimpleObjectProperty<Library?>(null).apply {
        addListener { _, oldValue, newValue ->
            oldValue?.unloadSounds()
            newValue?.let {
                it.createSoundsDirectory()
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
    val currentGameProperty: Property<Game?> = SimpleObjectProperty(null)
    // can't do delegated property because @JsonIgnore doesn't work on them
    var currentGame: Game?
        @JsonIgnore
        get() = currentGameProperty.value
        @JsonIgnore
        set(value) {
            currentGameProperty.value = value
        }

    @JsonIgnore
    val userdataPathProperty: Property<String> = SimpleStringProperty("")
    var userdataPath: String by userdataPathProperty

    @JsonIgnore
    val togglePlayKeyProperty: Property<String> = SimpleStringProperty("t")
    var togglePlayKey: String by togglePlayKeyProperty

    @JsonIgnore
    val relayKeyProperty: Property<String> = SimpleStringProperty("kp_end")
    var relayKey: String by relayKeyProperty

    class Deserializer : StdDeserializer<RootModel>(RootModel::class.java) {
        override fun deserialize(parser: JsonParser, context: DeserializationContext): RootModel {
            val rootNode = parser.codec.readTree<JsonNode>(parser)
            val libraries = (rootNode["libraries"] as ArrayNode).map { node ->
                parser.codec.treeToValue(node, Library::class.java)
            }
            val games = (rootNode["games"] as ArrayNode).map { node ->
                parser.codec.treeToValue(node, Game::class.java)
            }
            val togglePlayKey = rootNode["togglePlayKey"].asText()
            val relayKey = rootNode["relayKey"].asText()
            val userdataPath = rootNode["userdataPath"].asText()

            return RootModel().apply {
                this.libraries += libraries
                this.games += games
                this.togglePlayKey = togglePlayKey
                this.relayKey = relayKey
                this.userdataPath = userdataPath
            }
        }
    }
}
