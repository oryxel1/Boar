package ac.boar.mappings;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BedrockMappings {
    private static Gson GSON = new Gson();
    private static Map<String, CollisionBox> ENTITY_COLLISION_BOXES = new HashMap<>();

    public static void load() {
        // Entity collision list
        JsonObject entityCollisions = toJson("/bedrock_packs/entities.json");
        for (Map.Entry<String, JsonElement> entry : entityCollisions.entrySet()) {
            JsonObject object = entry.getValue().getAsJsonObject();

            ENTITY_COLLISION_BOXES.put(entry.getKey(), new CollisionBox(object.get("width").getAsFloat(), object.get("height").getAsFloat()));
        }
    }

    private static JsonObject toJson(String path) {
        try {
            return GSON.fromJson(new String(BedrockMappings.class.getResourceAsStream(path).readAllBytes()).strip(), JsonObject.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, CollisionBox> getEntityCollisionBoxes() {
        return Collections.unmodifiableMap(ENTITY_COLLISION_BOXES);
    }

    public record CollisionBox(float width, float height) {
    }
}
