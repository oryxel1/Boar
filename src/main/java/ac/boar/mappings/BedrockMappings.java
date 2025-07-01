package ac.boar.mappings;

import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.type.Block;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BedrockMappings {
    private static Map<Item, Block> ITEM_TO_BLOCK = new HashMap<>();

    public static void load() {
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

    public static Map<Item, Block> getItemToBlock() {
        return Collections.unmodifiableMap(ITEM_TO_BLOCK);
    }
}
