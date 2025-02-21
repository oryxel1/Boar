package ac.boar.anticheat.validator.click;

import ac.boar.anticheat.compensated.CompensatedInventory;
import ac.boar.anticheat.compensated.cache.container.ContainerCache;
import ac.boar.anticheat.player.BoarPlayer;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.DropAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestActionType;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.TransferItemStackRequestAction;

@RequiredArgsConstructor
public class BedrockClickProcessor {
    private final BoarPlayer player;

    public void processAction(final ItemStackRequestAction action) {
        final CompensatedInventory inventory = player.compensatedInventory;

        final ItemStackRequestActionType type = action.getType();
        final ContainerCache cache = inventory.openContainer;

        switch (type) {
            case TAKE, PLACE -> {
                final TransferItemStackRequestAction transferAction = (TransferItemStackRequestAction) action;
                System.out.println(transferAction);

                // TODO: bundle lol.

                final ItemStackRequestSlotData source = transferAction.getSource();
                final ItemStackRequestSlotData destination = transferAction.getDestination();
            }

            case DROP -> {
                final DropAction dropAction = (DropAction) action;
                final int slot = dropAction.getSource().getSlot();
                if (slot < 0 || slot >= cache.getContents().size()) {
                    System.out.println(cache.getClass());
                    System.out.println(slot + "," + cache.getContents().size());
                    break;
                }

                final ItemStackRequestSlotData source = dropAction.getSource();
                final ItemData data = cache.getContents().get(slot);

                // Player is clicking outside the window to drop.
                if (source.getContainer() == ContainerSlotType.CURSOR) {
                    if (inventory.hudContainer.getContents().isEmpty()) {
                        break;
                    }

                    final ItemData cursor = inventory.hudContainer.getContents().getFirst();
                    if (!cursor.isValid() || slot != 0) { // Slot 0 is cursor slot.
                        break;
                    }

                    this.drop(cache, 0, data, dropAction.getCount());
                    System.out.println("drop cursor!");
                } else { // Dropping by pressing Q?
                    this.drop(cache, slot, data, dropAction.getCount());
                    System.out.println("drop count!");
                }
            }
        }
    }

    private void drop(final ContainerCache cache, final int slot, final ItemData data, final int counts) {
        if (counts >= data.getCount()) {
            cache.getContents().set(slot, ItemData.AIR);
        } else {
            if (data.getCount() > 0) {
                ItemData.Builder builder = data.toBuilder();
                builder.count(data.getCount() - 1);

                final ItemData newData = builder.build();
                if (newData.getCount() <= 0) {
                    cache.getContents().set(slot, ItemData.AIR);
                } else {
                    cache.getContents().set(slot, builder.build());
                }
            }
        }
    }
}
