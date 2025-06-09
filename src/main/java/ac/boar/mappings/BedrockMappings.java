package ac.boar.mappings;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.type.Block;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BedrockMappings {
    private static Gson GSON = new Gson();
    private static Map<String, CollisionBox> ENTITY_COLLISION_BOXES = new HashMap<>();
    private static Map<Item, Block> ITEM_TO_BLOCK = new HashMap<>();

    public static void load() {
        // Entity collision list
        JsonObject entityCollisions = toJson("/bedrock_packs/entities.json");
        for (Map.Entry<String, JsonElement> entry : entityCollisions.entrySet()) {
            JsonObject object = entry.getValue().getAsJsonObject();

            ENTITY_COLLISION_BOXES.put(entry.getKey(), new CollisionBox(object.get("width").getAsFloat(), object.get("height").getAsFloat()));
        }

        for (Field field : Blocks.class.getDeclaredFields()) {
            try {
                Object object = field.get(null);

                if (object instanceof Block block) {
                    Item item = Item.byBlock(block);
                    if (item.equals(Items.AIR)) {
                        continue;
                    }

                    ITEM_TO_BLOCK.put(item, block);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
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

    public static Map<Item, Block> getItemToBlock() {
        return Collections.unmodifiableMap(ITEM_TO_BLOCK);
    }

    public record CollisionBox(float width, float height) {
    }
}
