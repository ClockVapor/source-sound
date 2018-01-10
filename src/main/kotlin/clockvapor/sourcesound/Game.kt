package clockvapor.sourcesound

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer

@JsonDeserialize(using = Game.Deserializer::class)
data class Game(var name: String, var id: Int, var path: String, var cfgPath: String, var useUserdata: Boolean,
                var soundsRate: Int) {
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
