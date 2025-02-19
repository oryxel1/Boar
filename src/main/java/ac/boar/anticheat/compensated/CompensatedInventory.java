package ac.boar.anticheat.compensated;

import ac.boar.anticheat.compensated.cache.container.ContainerCache;
import ac.boar.anticheat.player.BoarPlayer;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;

@RequiredArgsConstructor
public class CompensatedInventory {
    private final BoarPlayer player;

    public int heldItemSlot;

    public ContainerCache inventoryContainer = new ContainerCache((byte) ContainerId.INVENTORY, ContainerType.INVENTORY, null, -1L);
    public ContainerCache offhandContainer = new ContainerCache((byte) ContainerId.OFFHAND, ContainerType.INVENTORY, null, -1L);
    public ContainerCache armorContainer = new ContainerCache((byte) ContainerId.ARMOR, ContainerType.INVENTORY, null, -1L);
    public ContainerCache hudContainer = new ContainerCache((byte) ContainerId.UI, ContainerType.INVENTORY, null, -1L);

    public ContainerCache openContainer = null;

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
