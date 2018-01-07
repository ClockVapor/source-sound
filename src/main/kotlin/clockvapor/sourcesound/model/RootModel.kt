package clockvapor.sourcesound.model

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

@JsonDeserialize(using = RootModel.Deserializer::class)
class RootModel {
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

    val libraries: ObservableList<Library> = FXCollections.observableArrayList<Library>()

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

    var togglePlayKey: String = "t" // TODO: make this configurable
    var relayKey: String = "kp_end" // TODO: make this configurable

    class Deserializer : StdDeserializer<RootModel>(RootModel::class.java) {
        override fun deserialize(parser: JsonParser, context: DeserializationContext): RootModel {
            val rootNode = parser.codec.readTree<JsonNode>(parser)
            val libraries = (rootNode["libraries"] as ArrayNode).map { node ->
                parser.codec.treeToValue(node, Library::class.java)
            }
            val togglePlayKey = rootNode["togglePlayKey"].asText()
            val relayKey = rootNode["relayKey"].asText()
            return RootModel().apply {
                this.libraries.addAll(libraries)
                this.togglePlayKey = togglePlayKey
                this.relayKey = relayKey
            }
        }
    }
}
