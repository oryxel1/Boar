package ac.boar.mappings;

import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.type.Block;

import java.lang.reflect.Field;
import java.util.*;

public class BlockMappings {
    private static final Map<Item, Block> ITEM_TO_BLOCK = new HashMap<>();
    private static final List<Block> CLIMBABLE_BLOCKS = new ArrayList<>();
    private static final List<Block> FENCE_BLOCKS = new ArrayList<>();
    private static final List<Block> FENCE_GATE_BLOCKS = new ArrayList<>();
    private static final List<Block> WALL_BLOCKS = new ArrayList<>();
    private static final List<Block> SHULKER_BLOCKS = new ArrayList<>();
    private static final List<Block> LEAVES_BLOCKS = new ArrayList<>();
    private static final List<Block> STAIRS_BLOCKS = new ArrayList<>();

    public static void load() {
        for (Field field : Blocks.class.getDeclaredFields()) {
            try {
                Object object = field.get(null);

                if (object instanceof Block block) {
                    final String lowercaseName = field.getName().toLowerCase(Locale.ROOT);
                    if (lowercaseName.endsWith("_fence")) {
                        FENCE_BLOCKS.add(block);
                    } else if (lowercaseName.endsWith("_wall")) {
                        WALL_BLOCKS.add(block);
                    } else if (lowercaseName.endsWith("_fence_gate")) {
                        FENCE_GATE_BLOCKS.add(block);
                    } else if (lowercaseName.endsWith("shulker_box")) {
                        SHULKER_BLOCKS.add(block);
                    } else if (lowercaseName.endsWith("_leaves")) {
                        LEAVES_BLOCKS.add(block);
                    } else if (lowercaseName.endsWith("_stairs")) {
                        STAIRS_BLOCKS.add(block);
                    }

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

        // Glow Berry
        CLIMBABLE_BLOCKS.add(Blocks.CAVE_VINES);
        CLIMBABLE_BLOCKS.add(Blocks.CAVE_VINES_PLANT);
        CLIMBABLE_BLOCKS.add(Blocks.CAVE_VINES_PLANT);

        CLIMBABLE_BLOCKS.add(Blocks.LADDER);
        CLIMBABLE_BLOCKS.add(Blocks.VINE);

        // Nether stuff.
        CLIMBABLE_BLOCKS.add(Blocks.TWISTING_VINES);
        CLIMBABLE_BLOCKS.add(Blocks.TWISTING_VINES_PLANT);
        CLIMBABLE_BLOCKS.add(Blocks.WEEPING_VINES);
        CLIMBABLE_BLOCKS.add(Blocks.WEEPING_VINES_PLANT);
//        System.out.println("Cache: " + Arrays.toString(LEAVES_BLOCKS.toArray()));
    }

    public static List<Block> getStairsBlocks() {
        return Collections.unmodifiableList(STAIRS_BLOCKS);
    }

    public static List<Block> getLeavesBlocks() {
        return Collections.unmodifiableList(LEAVES_BLOCKS);
    }

    public static List<Block> getShulkerBlocks() {
        return Collections.unmodifiableList(SHULKER_BLOCKS);
    }

    public static List<Block> getFenceGateBlocks() {
        return Collections.unmodifiableList(FENCE_GATE_BLOCKS);
    }

    public static List<Block> getWallBlocks() {
        return Collections.unmodifiableList(WALL_BLOCKS);
    }

    public static List<Block> getFenceBlocks() {
        return Collections.unmodifiableList(FENCE_BLOCKS);
    }

    public static List<Block> getClimbableBlocks() {
        return Collections.unmodifiableList(CLIMBABLE_BLOCKS);
    }

    public static Map<Item, Block> getItemToBlock() {
        return Collections.unmodifiableMap(ITEM_TO_BLOCK);
    }
}
