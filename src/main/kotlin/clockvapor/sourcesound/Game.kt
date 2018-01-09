package clockvapor.sourcesound

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer

@JsonDeserialize(using = Game.Deserializer::class)
data class Game(var name: String, var id: Int, var path: String, var cfgPath: String) {
    class Deserializer : StdDeserializer<Game>(Game::class.java) {
        override fun deserialize(parser: JsonParser, context: DeserializationContext): Game {
            val rootNode = parser.codec.readTree<JsonNode>(parser)
            return Game(rootNode["name"].asText(), rootNode["id"].asInt(), rootNode["path"].asText(),
                rootNode["cfgPath"].asText())
        }
    }
}
