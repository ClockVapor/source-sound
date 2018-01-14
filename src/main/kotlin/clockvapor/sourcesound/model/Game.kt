package clockvapor.sourcesound.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import javafx.beans.property.*
import tornadofx.*

@JsonDeserialize(using = Game.Deserializer::class)
class Game(name: String = "", id: Int = -1, path: String = "", cfgPath: String = "", useUserdata: Boolean = false,
           soundsRate: Int = Sound.rates[0]) {

    @JsonIgnore
    val nameProperty: Property<String> = SimpleStringProperty(name)
    var name: String by nameProperty

    @JsonIgnore
    val idProperty: IntegerProperty = SimpleIntegerProperty(id)
    var id: Int by idProperty

    @JsonIgnore
    val pathProperty: Property<String> = SimpleStringProperty(path)
    var path: String by pathProperty

    @JsonIgnore
    val cfgPathProperty: Property<String> = SimpleStringProperty(cfgPath)
    var cfgPath: String by cfgPathProperty

    @JsonIgnore
    val useUserdataProperty: BooleanProperty = SimpleBooleanProperty(useUserdata)
    var useUserdata: Boolean by useUserdataProperty

    @JsonIgnore
    val soundsRateProperty: Property<Number> = SimpleIntegerProperty(soundsRate)
    var soundsRate: Number by soundsRateProperty

    class Deserializer : StdDeserializer<Game>(Game::class.java) {
        override fun deserialize(parser: JsonParser, context: DeserializationContext): Game {
            val rootNode = parser.codec.readTree<JsonNode>(parser)
            val name = rootNode["name"]?.asText() ?: ""
            val id = rootNode["id"]?.asInt() ?: -1
            val path = rootNode["path"]?.asText() ?: ""
            val cfgPath = rootNode["cfgPath"]?.asText() ?: ""
            val useUserdata = rootNode["useUserdata"]?.asBoolean() ?: false
            val soundsRate = rootNode["soundsRate"]?.asInt() ?: -1
            return Game(name, id, path, cfgPath, useUserdata, soundsRate)
        }
    }
}
