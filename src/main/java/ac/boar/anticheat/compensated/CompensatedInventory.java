package ac.boar.anticheat.compensated;

import ac.boar.anticheat.compensated.cache.container.ContainerCache;
import ac.boar.anticheat.player.BoarPlayer;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;

@RequiredArgsConstructor
public class CompensatedInventory {
    private final BoarPlayer player;

    public int heldItemSlot;

    public ContainerCache inventoryContainer = new ContainerCache((byte) ContainerId.INVENTORY, ContainerType.INVENTORY, null, -1L);
    public ContainerCache offhandContainer = new ContainerCache((byte) ContainerId.OFFHAND, ContainerType.INVENTORY, null, -1L);
    public ContainerCache armorContainer = new ContainerCache((byte) ContainerId.ARMOR, ContainerType.INVENTORY, null, -1L);
    public ContainerCache hudContainer = new ContainerCache((byte) ContainerId.UI, ContainerType.INVENTORY, null, -1L);

    public ContainerCache openContainer = null;

    public ItemStack translate(ItemData data) {
        return ItemTranslator.translateToJava(player.getSession(), data);
    }

    public ItemData getHeldItemData() {
        if (this.heldItemSlot >= inventoryContainer.getContents().size()) {
            return ItemData.AIR;
        }

        return inventoryContainer.getContents().get(heldItemSlot);
    }

    public GeyserItemStack getHeldItem() {
        return GeyserItemStack.from(translate(getHeldItemData()));
    }

    public ContainerCache getContainer(byte id) {
        if (id == inventoryContainer.getId()) {
            return inventoryContainer;
        } else if (id == offhandContainer.getId()) {
            return offhandContainer;
        } else if (id == armorContainer.getId()) {
            return armorContainer;
        } else if (id == hudContainer.getId()) {
            return hudContainer;
        } else if (id == openContainer.getId()) {
            return openContainer;
        }

        return null;
    }
}
