package ac.boar.anticheat.compensated.cache.container;

import ac.boar.anticheat.compensated.CompensatedInventory;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.inventory.GeyserItemStack;

public class PlayerContainerCache extends ContainerCache {
    private final CompensatedInventory inventory;

    public PlayerContainerCache(final CompensatedInventory inventory) {
        super((byte) ContainerId.INVENTORY, ContainerType.INVENTORY, null, -1L);

        this.inventory = inventory;
    }

    public ItemData getHeldItemData() {
        return this.getItemFromSlot(this.inventory.heldItemSlot);
    }

    public GeyserItemStack getHeldItem() {
        return GeyserItemStack.from(this.inventory.translate(getHeldItemData()));
    }

    public ItemData getItemFromSlot(final int slot) {
        if (slot < 0 || slot > 8 || slot >= this.getContents().size()) {
            return ItemData.AIR;
        }

        return this.getContents().get(slot);
    }
}
