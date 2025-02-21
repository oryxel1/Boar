package ac.boar.anticheat.validator.click;

import ac.boar.anticheat.compensated.CompensatedInventory;
import ac.boar.anticheat.compensated.cache.container.ContainerCache;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.validator.ItemTransactionValidator;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.*;

import java.util.List;

@RequiredArgsConstructor
public class BedrockClickProcessor {
    private static final List<ContainerType> CANT_HANDLE = List.of(ContainerType.WORKBENCH);

    private final BoarPlayer player;

    public boolean processAction(final ItemStackRequestAction action) {
        final CompensatedInventory inventory = player.compensatedInventory;

        final ItemStackRequestActionType type = action.getType();
        final ContainerCache cache = inventory.openContainer;

        switch (type) {
            case TAKE, PLACE -> {
                final TransferItemStackRequestAction transferAction = (TransferItemStackRequestAction) action;
                // TODO: bundle lol.

                final ItemStackRequestSlotData source = transferAction.getSource();
                final ItemStackRequestSlotData destination = transferAction.getDestination();

                final ContainerCache sourceContainer = this.findContainer(source.getContainer());
                final ContainerCache destinationContainer = this.findContainer(destination.getContainer());

                final int sourceSlot = source.getSlot();
                final int destinationSlot = destination.getSlot();

                // This seems to be handled client-sided, like crafting result.
                final boolean cantDoValidation = CANT_HANDLE.contains(sourceContainer.getType()) || CANT_HANDLE.contains(destinationContainer.getType());
                if (cantDoValidation) {
                    sourceContainer.setOutOfSync(true);
                    destinationContainer.setOutOfSync(true);
                    System.out.println("OUT OF SYNC!");
                }

                if (sourceSlot < 0 || destinationSlot < 0 || sourceSlot >= sourceContainer.getContents().size() || destinationSlot > 100) {
                    return cantDoValidation;
                }

                // TODO: properly implement container size, I will just cap it at 100 for now.
                if (destinationSlot >= destinationContainer.getContents().size()) {
                    while (destinationContainer.getContents().size() <= destinationSlot) {
                        destinationContainer.getContents().add(ItemData.AIR);
                    }
                }

                final ItemData sourceData = sourceContainer.getContents().get(sourceSlot);
                final ItemData destinationData = destinationContainer.getContents().get(destinationSlot);

                // Player try to move this item to an already occupied destination, and is sending TAKE/PLACE instead of SWAP.
                // This is not the same item too, so not possible...
                if (!destinationData.isNull() && !ItemTransactionValidator.validate(sourceData, destinationData)) {
                    // for debugging in case I fucked up.
                    System.out.println("INVALID DESTINATION!");
                    System.out.println(sourceData);
                    System.out.println(destinationSlot);
                    return cantDoValidation;
                }

                final int count = transferAction.getCount();
                // Source data is air, or count is invalid.
                if (sourceData.isNull() || count <= 0 || count > sourceData.getCount()) {
                    System.out.println("INVALID COUNT!"); // for debugging in case I fucked up.
                    System.out.println("First condition: " + sourceData.isNull());
                    System.out.println("Count: " + count);
                    System.out.println("Source Data: " + sourceData);
                    return cantDoValidation;
                }

                // Now simply move, lol.
                this.remove(sourceContainer, sourceSlot, sourceData, count);

                if (destinationData.isNull()) {
                    final ItemData.Builder builder = sourceData.toBuilder();
                    builder.count(count);

                    destinationContainer.getContents().set(destinationSlot, builder.build());
                } else {
                    this.add(destinationContainer, destinationSlot, destinationData, count);
                }
            }

            case SWAP -> {
                final SwapAction swapAction = (SwapAction) action;

                final ItemStackRequestSlotData source = swapAction.getSource();
                final ItemStackRequestSlotData destination = swapAction.getDestination();

                final ContainerCache sourceContainer = this.findContainer(source.getContainer());
                final ContainerCache destinationContainer = this.findContainer(destination.getContainer());

                final int sourceSlot = source.getSlot();
                final int destinationSlot = destination.getSlot();

                if (sourceSlot < 0 || destinationSlot < 0 || sourceSlot >= sourceContainer.getContents().size() || destinationSlot > 100) {
                    return false;
                }

                // TODO: properly implement container size, I will just cap it at 100 for now.
                if (destinationSlot >= destinationContainer.getContents().size()) {
                    while (destinationContainer.getContents().size() <= destinationSlot) {
                        destinationContainer.getContents().add(ItemData.AIR);
                    }
                }

                final ItemData sourceData = sourceContainer.getContents().get(sourceSlot);
                final ItemData destinationData = destinationContainer.getContents().get(destinationSlot);

                // Source/Destination slot is empty! Player is supposed to send TAKE/PLACE instead of SWAP!
                if (sourceData.isNull() || destinationData.isNull()) {
                    System.out.println("INVALID SWAP!"); // for debugging in case I fucked up.
                    return false;
                }

                // Now simply swap :D
                sourceContainer.getContents().set(sourceSlot, destinationData);
                destinationContainer.getContents().set(destinationSlot, sourceData);
            }

            case DROP -> {
                final DropAction dropAction = (DropAction) action;
                final int slot = dropAction.getSource().getSlot();
                if (slot < 0 || slot >= cache.getContents().size()) {
                    return false;
                }

                final ItemStackRequestSlotData source = dropAction.getSource();
                final ItemData data = cache.getContents().get(slot);

                // Player is clicking outside the window to drop.
                if (source.getContainer() == ContainerSlotType.CURSOR) {
                    if (inventory.hudContainer.getContents().isEmpty()) {
                        return false;
                    }

                    final ItemData cursor = inventory.hudContainer.getContents().getFirst();
                    if (!cursor.isValid() || slot != 0) { // Slot 0 is cursor slot.
                        return false;
                    }

                    this.remove(inventory.hudContainer, 0, data, dropAction.getCount());
                    System.out.println("drop cursor!");
                } else { // Dropping by pressing Q?
                    this.remove(cache, slot, data, dropAction.getCount());
                    System.out.println("drop count!");
                }
            }
        }

        return true;
    }

    public void add(final ContainerCache cache, final int slot, final ItemData data, final int counts) {
        final ItemData.Builder builder = data.toBuilder();
        builder.count(data.getCount() + counts);
        cache.getContents().set(slot, builder.build());
    }

    private void remove(final ContainerCache cache, final int slot, final ItemData data, final int counts) {
        if (counts >= data.getCount()) {
            cache.getContents().set(slot, ItemData.AIR);
        } else {
            if (data.getCount() > 0) {
                final ItemData.Builder builder = data.toBuilder();
                builder.count(data.getCount() - counts);

                final ItemData newData = builder.build();
                if (newData.getCount() <= 0) {
                    cache.getContents().set(slot, ItemData.AIR);
                } else {
                    cache.getContents().set(slot, builder.build());
                }
            }
        }
    }

    private ContainerCache findContainer(final ContainerSlotType type) {
        final CompensatedInventory inventory = player.compensatedInventory;

        ContainerCache cache;
        switch (type) {
            case CURSOR -> cache = inventory.hudContainer;
            case ARMOR -> cache = inventory.armorContainer;
            case OFFHAND -> cache = inventory.offhandContainer;
            case INVENTORY, HOTBAR, HOTBAR_AND_INVENTORY -> cache = inventory.inventoryContainer;
            default -> cache = inventory.openContainer;
        }

        return cache;
    }
}
