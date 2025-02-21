package ac.boar.anticheat.compensated.cache.container;

import lombok.Getter;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;

import java.util.Arrays;

public class ContainerCache {
    @Getter
    private final byte id;
    @Getter
    private final ContainerType type;
    @Getter
    private final Vector3i blockPosition;
    @Getter
    private final long uniqueEntityId;

    @Getter
    private final int containerSize, offset;
    private final ItemData[] contents;

    public ContainerCache(byte id, ContainerType type, Vector3i blockPosition, long uniqueEntityId) {
        this.id = id;
        this.type = type;
        this.blockPosition = blockPosition;
        this.uniqueEntityId = uniqueEntityId;

        // TODO
        this.offset = switch (type) {
            case ENCHANTMENT -> 14;
            case LOOM -> 9;
            case WORKBENCH -> 32;
            case BEACON -> 27;
            case ANVIL -> 1;
            case STONECUTTER -> 3;
            default -> 0;
        };
        this.containerSize = switch (type) {
            case FURNACE, BLAST_FURNACE, SMOKER, LOOM -> 3;
            case BREWING_STAND, HOPPER, MINECART_HOPPER -> 5;
            case DROPPER, DISPENSER, WORKBENCH -> 9;
            case ENCHANTMENT, ANVIL, HORSE -> 2;
            case BEACON, STONECUTTER -> 1;
            case STRUCTURE_EDITOR, COMMAND_BLOCK -> 0;
            case MINECART_CHEST, CHEST_BOAT -> 26;
            case ARMOR -> 4;
            default -> 36;
        };

        if (this.containerSize > 0) {
            this.contents = new ItemData[this.containerSize];
            Arrays.fill(this.contents, ItemData.AIR);
        } else {
            this.contents = null;
        }
    }

    public ItemData get(final int slot) {
        return this.contents[slot - this.offset];
    }
}
