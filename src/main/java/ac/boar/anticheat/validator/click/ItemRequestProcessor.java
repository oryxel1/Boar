package ac.boar.anticheat.validator.click;

import ac.boar.anticheat.compensated.CompensatedInventory;
import ac.boar.anticheat.compensated.cache.container.ContainerCache;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.validator.ItemTransactionValidator;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.protocol.bedrock.data.GameType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.*;

@RequiredArgsConstructor
public class ItemRequestProcessor {
    private final BoarPlayer player;

    public boolean processAll(final ItemStackRequest request) {
        for (final ItemStackRequestAction action : request.getActions()) {
            System.out.println(action);
//            if (!this.handle(action)) {
//                return false;
//            }
        }

        return true;
    }

//    public boolean handle(final ItemStackRequestAction action) {
//        final CompensatedInventory inventory = player.compensatedInventory;
//
//        final ItemStackRequestActionType type = action.getType();
//        final ContainerCache cache = inventory.openContainer;
//
//        switch (type) {
//            case CRAFT_RECIPE -> {
//                if (cache.getType() != ContainerType.WORKBENCH) {
//                    // Ehhh later.
//                    break;
//                }
//            }
//
//            case TAKE, PLACE -> {
//                final TransferItemStackRequestAction transferAction = (TransferItemStackRequestAction) action;
//                // TODO: bundle lol.
//
//                final ItemStackRequestSlotData source = transferAction.getSource();
//                final ItemStackRequestSlotData destination = transferAction.getDestination();
//
//                final ContainerCache sourceContainer = this.findContainer(source.getContainer());
//                final ContainerCache destinationContainer = this.findContainer(destination.getContainer());
//
//                final int sourceSlot = source.getSlot();
//                final int destinationSlot = destination.getSlot();
//
//                if (sourceSlot < 0 || destinationSlot < 0 || sourceSlot >= sourceContainer.getContainerSize() || destinationSlot >= destinationContainer.getContainerSize()) {
//                    return false;
//                }
//
//                final ItemData sourceData = sourceContainer.getContents()[sourceSlot];
//                final ItemData destinationData = destinationContainer.getContents()[destinationSlot];
//
//                // Player try to move this item to an already occupied destination, and is sending TAKE/PLACE instead of SWAP.
//                // This is not the same item too, so not possible...
//                if (!destinationData.isNull() && !ItemTransactionValidator.validate(sourceData, destinationData)) {
//                    // for debugging in case I fucked up.
//                    System.out.println("INVALID DESTINATION!");
//                    System.out.println(sourceData);
//                    System.out.println(destinationSlot);
//                    return false;
//                }
//
//                final int count = transferAction.getCount();
//                // Source data is air, or count is invalid.
//                if (sourceData.isNull() || count <= 0 || count > sourceData.getCount()) {
//                    System.out.println("INVALID COUNT!"); // for debugging in case I fucked up.
//                    System.out.println("First condition: " + sourceData.isNull());
//                    System.out.println("Count: " + count);
//                    System.out.println("Source Data: " + sourceData);
//                    return false;
//                }
//
//                // Now simply move, lol.
//                this.remove(sourceContainer, sourceSlot, sourceData, count);
//
//                if (destinationData.isNull()) {
//                    final ItemData.Builder builder = sourceData.toBuilder();
//                    builder.count(count);
//
//                    destinationContainer.getContents()[destinationSlot] = builder.build();
//                } else {
//                    this.add(destinationContainer, destinationSlot, destinationData, count);
//                }
//            }
//
//            case SWAP -> {
//                final SwapAction swapAction = (SwapAction) action;
//
//                final ItemStackRequestSlotData source = swapAction.getSource();
//                final ItemStackRequestSlotData destination = swapAction.getDestination();
//
//                final ContainerCache sourceContainer = this.findContainer(source.getContainer());
//                final ContainerCache destinationContainer = this.findContainer(destination.getContainer());
//
//                final int sourceSlot = source.getSlot();
//                final int destinationSlot = destination.getSlot();
//
//                if (sourceSlot < 0 || destinationSlot < 0 || sourceSlot >= sourceContainer.getContainerSize() || destinationSlot >= destinationContainer.getContainerSize()) {
//                    return false;
//                }
//
//                final ItemData sourceData = sourceContainer.getContents()[sourceSlot];
//                final ItemData destinationData = destinationContainer.getContents()[destinationSlot];
//
//                // Source/Destination slot is empty! Player is supposed to send TAKE/PLACE instead of SWAP!
//                if (sourceData.isNull() || destinationData.isNull()) {
//                    System.out.println("INVALID SWAP!"); // for debugging in case I fucked up.
//                    return false;
//                }
//
//                // Now simply swap :D
//                sourceContainer.getContents()[sourceSlot] = destinationData;
//                destinationContainer.getContents()[destinationSlot] = sourceData;
//            }
//
//            case DROP -> {
//                final DropAction dropAction = (DropAction) action;
//                final int slot = dropAction.getSource().getSlot();
//                if (slot < 0 || slot >= cache.getContainerSize()) {
//                    return false;
//                }
//
//                final ItemStackRequestSlotData source = dropAction.getSource();
//                final ItemData data = cache.getContents()[slot];
//
//                // Player is clicking outside the window to drop.
//                if (source.getContainer() == ContainerSlotType.CURSOR) {
//                    final ItemData cursor = inventory.hudContainer.getContents()[0];
//                    if (!cursor.isValid() || slot != 0) { // Slot 0 is cursor slot.
//                        return false;
//                    }
//
//                    this.remove(inventory.hudContainer, 0, data, dropAction.getCount());
//                    System.out.println("drop cursor!");
//                } else { // Dropping by pressing Q?
//                    this.remove(cache, slot, data, dropAction.getCount());
//                    System.out.println("drop count!");
//                }
//            }
//
//            case DESTROY -> {
//                if (player.gameType != GameType.CREATIVE) {
//                    break;
//                }
//
//                final DestroyAction destroyAction = (DestroyAction) action;
//                final ItemStackRequestSlotData source = destroyAction.getSource();
//                final ContainerCache sourceContainer = this.findContainer(source.getContainer());
//
//                final int slot = source.getSlot();
//                if (slot < 0 || slot > sourceContainer.getContainerSize()) {
//                    return false;
//                }
//
//                final ItemData itemData = sourceContainer.getContents()[slot];
//
//                if (destroyAction.getCount() > itemData.getCount()) {
//                    return false;
//                }
//
//                this.remove(sourceContainer, slot, itemData, destroyAction.getCount());
//            }
//        }
//
//        return true;
//    }
//
//    public void add(final ContainerCache cache, final int slot, final ItemData data, final int counts) {
//        final ItemData.Builder builder = data.toBuilder();
//        builder.count(data.getCount() + counts);
//        cache.getContents()[slot] = builder.build();
//    }
//
//    private void remove(final ContainerCache cache, final int slot, final ItemData data, final int counts) {
//        if (counts >= data.getCount()) {
//            cache.getContents()[slot] = ItemData.AIR;
//        } else {
//            if (data.getCount() > 0) {
//                final ItemData.Builder builder = data.toBuilder();
//                builder.count(data.getCount() - counts);
//
//                final ItemData newData = builder.build();
//                if (newData.getCount() <= 0) {
//                    cache.getContents()[slot] = ItemData.AIR;
//                } else {
//                    cache.getContents()[slot] = builder.build();
//                }
//            }
//        }
//    }
//
//    private ContainerCache findContainer(final ContainerSlotType type) {
//        final CompensatedInventory inventory = player.compensatedInventory;
//
//        ContainerCache cache;
//        switch (type) {
//            case CURSOR -> cache = inventory.hudContainer;
//            case ARMOR -> cache = inventory.armorContainer;
//            case OFFHAND -> cache = inventory.offhandContainer;
//            case INVENTORY, HOTBAR, HOTBAR_AND_INVENTORY -> cache = inventory.inventoryContainer;
//            default -> cache = inventory.openContainer;
//        }
//
//        return cache;
//    }
}
