package ac.boar.anticheat.validator;

import ac.boar.anticheat.check.impl.place.AirPlace;
import ac.boar.anticheat.compensated.CompensatedInventory;
import ac.boar.anticheat.data.block.BoarBlockState;
import ac.boar.anticheat.data.inventory.ItemCache;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.validator.click.ItemRequestProcessor;
import ac.boar.anticheat.util.MathUtil;
import ac.boar.anticheat.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.definitions.SimpleItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryActionData;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventorySource;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.ItemUseTransaction;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.LegacySetItemSlotData;
import org.cloudburstmc.protocol.bedrock.packet.InventorySlotPacket;
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket;
import org.cloudburstmc.protocol.bedrock.packet.ItemStackRequestPacket;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.type.BlockItem;
import org.geysermc.geyser.level.physics.Direction;
import org.geysermc.geyser.util.BlockUtils;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public final class ItemTransactionValidator {
    private final BoarPlayer player;

    public boolean handle(final InventoryTransactionPacket packet) {
        final CompensatedInventory inventory = player.compensatedInventory;
        switch (packet.getTransactionType()) {
            case NORMAL -> {
                if (packet.getActions().size() != 2) {
                    return false;
                }
                // https://github.com/GeyserMC/Geyser/blob/master/core/src/main/java/org/geysermc/geyser/translator/protocol/bedrock/BedrockInventoryTransactionTranslator.java#L123
                final InventoryActionData world = packet.getActions().get(0), container = packet.getActions().get(1);

                if (world.getSource().getType() != InventorySource.Type.WORLD_INTERACTION || world.getSource().getFlag() != InventorySource.Flag.DROP_ITEM) {
                    return false;
                }

                final int slot = container.getSlot();
                if (slot < 0 || slot > 8) {
                    return false;
                }

                final ItemData slotData = inventory.inventoryContainer.getItemFromSlot(slot).getData();
                final ItemData claimedData = world.getToItem();
                final int dropCounts = claimedData.getCount();

                // Invalid drop, item or whatever
                if (dropCounts < 1 || dropCounts > slotData.getCount() || !validate(slotData, claimedData)) {
                    return false;
                }

                // Since Geyser proceed to drop everything anyway, as long as you send anything larger than 1.
                // Also, it is possible to drop more than 1 but not all? I don't know.
                if (dropCounts > 1 && dropCounts < slotData.getCount()) {
                    final InventorySlotPacket slotPacket = new InventorySlotPacket();
                    slotPacket.setItem(ItemData.AIR);
                    slotPacket.setContainerId(ContainerId.INVENTORY);
                    slotPacket.setSlot(slot);
                    player.cloudburstUpstream.sendPacket(slotPacket);
                }

                if (dropCounts == slotData.getCount()) {
                    inventory.inventoryContainer.set(slot, ItemData.AIR);
                } else {
                    ItemData.Builder builder = slotData.toBuilder();
                    builder.count(Math.max(0, slotData.getCount() - dropCounts));

                    inventory.inventoryContainer.set(slot, builder.build());
                }
            }

            case ITEM_USE_ON_ENTITY -> {
//                final int slot = packet.getHotbarSlot();
//                if (slot < 0 || slot > 8) {
//                    return false;
//                }
//
//                final ItemData SD1 = inventory.inventoryContainer.getItemFromSlot(slot).getData();
//                final ItemData SD2 = inventory.inventoryContainer.getHeldItemData();
//                if (!validate(SD1, packet.getItemInHand()) && !validate(SD2, packet.getItemInHand())) {
//                    return false;
//                }
//
//                final EntityCache entity = player.compensatedWorld.getEntity(packet.getRuntimeEntityId());
//                if (entity == null || entity.getTransactionId() > player.receivedStackId.get()) {
//                    return false;
//                }
//
//                final boolean tooFar = entity.getServerPosition().distanceTo(player.position) > 6;
//                if (tooFar) {
//                    return false;
//                }
            }

            case ITEM_RELEASE -> {
                // Self-explanatory.
                if (packet.getActionType() == 0) {
                    player.getUseItemCache().release();
                }
            }

            case ITEM_USE -> {
                final Vector3i position = packet.getBlockPosition();
                final int slot = packet.getHotbarSlot();
                if (slot < 0 || slot > 8) {
                    return false;
                }

                final ItemData SD1 = inventory.inventoryContainer.getHeldItemData();

                boolean noActions = packet.getActions().isEmpty();

                if (!noActions) {
                    for (final InventoryActionData action : packet.getActions()) {
                        if (action.getSlot() < 0 || action.getSlot() > 8) {
                            return false;
                        }

                        final ItemData SD2 = inventory.inventoryContainer.getItemFromSlot(action.getSlot()).getData();
                        if (!validate(SD2, action.getFromItem())) {
                            return false;
                        }
                    }
                }

                if (noActions && !validate(SD1, packet.getItemInHand())) {
                    return false;
                }

                double distance = player.position.toVector3f().distanceSquared(position.getX(), position.getY(), position.getZ());
                if (!MathUtil.isValid(position) || distance > 12 * 12 && position.getX() + position.getY() + position.getZ() != 0) {
                    return false;
                }

                // The rest is going to validate by Geyser.

                final BoarBlockState state = player.compensatedWorld.getBlockState(position, 0);
                switch (packet.getActionType()) {
                    case 0 -> {
                        if (packet.getItemInHand() == null || !validate(SD1, packet.getItemInHand())) {
                            return true; // nope, not a mistake, Geyser going to take care of it anyway.
                        }

                        if (packet.getClientInteractPrediction() == ItemUseTransaction.PredictedResult.FAILURE) {
                            return true; // Player claimed to be failing this action, no need to process it.
                        }

                        int blockFace = packet.getBlockFace();
                        if (blockFace < 0 || blockFace > 5) {
                            return false; // Invalid.
                        }

                        ItemCache heldItem = inventory.inventoryContainer.getHeldItemCache();
                        boolean heldItemExist = !heldItem.isEmpty();
                        boolean doingSecondaryAction = player.getInputData().contains(PlayerAuthInputData.SNEAKING) && heldItemExist;

//                        if (!doingSecondaryAction) {
//                            state.useItemOn(inventory.inventoryContainer.getHeldItemCache(), player);
//                            return true;
//                        }

                        GeyserItemStack geyserItemStack = GeyserItemStack.from(inventory.translate(heldItem.getData()));
                        if (geyserItemStack.asItem() instanceof BlockItem) {
                            if (state.isAir()) {
                                player.getCheckHolder().get(AirPlace.class).fail();
                            }

                            Vector3i newBlockPos = BlockUtils.getBlockPosition(packet.getBlockPosition(), packet.getBlockFace());
                        }

                        System.out.println(packet);
                    }

                    // This seems to for things that is not related to block interact and only for item interaction.
                    case 1 -> {
                        if (packet.getItemInHand() == null || !validate(SD1, packet.getItemInHand())) {
                            return true;
                        }

                        player.getUseItemCache().use(SD1);

                        List<LegacySetItemSlotData> legacySlots = packet.getLegacySlots();
                        if (packet.getActions().size() == 1 && !legacySlots.isEmpty()) {
                            if (packet.getHotbarSlot() != inventory.heldItemSlot) {
                                break;
                            }

                            LegacySetItemSlotData slotData = legacySlots.get(0);
                            if (slotData.getSlots().length == 0) {
                                break;
                            }

                            int actualSlot = slotData.getSlots()[0];
                            if (actualSlot < 0 || actualSlot >= inventory.armorContainer.getContainerSize()) {
                                break;
                            }

                            if (slotData.getContainerId() == 6) {
                                ItemData oldHotbar = inventory.inventoryContainer.getHeldItemData();
                                inventory.inventoryContainer.set(packet.getHotbarSlot(), inventory.armorContainer.get(actualSlot));
                                inventory.armorContainer.set(actualSlot, oldHotbar);
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    public void handle(final ItemStackRequestPacket packet) {
        final CompensatedInventory inventory = player.compensatedInventory;
        if (inventory.openContainer == null) {
            return;
        }

        final List<ItemStackRequest> clone = new ArrayList<>(packet.getRequests());
        packet.getRequests().clear();

        final ItemRequestProcessor processor = new ItemRequestProcessor(player);
        for (final ItemStackRequest request : clone) {
            if (request.getActions().length == 0) {
                packet.getRequests().add(request);
                continue;
            }

            if (!processor.processAll(request)) {
                return;
            }

            packet.getRequests().add(request);
        }
    }

    public static boolean validate(final ItemData predicted, final ItemData claimed) {
        if (predicted == null) {
            // Our fault?
            return true;
        }

        if (claimed == null) {
            return false;
        }

        final ItemDefinition ID1 = predicted.getDefinition();
        final ItemDefinition ID2 = claimed.getDefinition();
        if (!(ID1 instanceof SimpleItemDefinition SID1) || !(ID2 instanceof SimpleItemDefinition SID2)) {
            return true;
        }

        if (!StringUtil.sanitizePrefix(SID1.getIdentifier()).equalsIgnoreCase(StringUtil.sanitizePrefix(SID2.getIdentifier()))) {
            return false;
        }

        return ID1.getRuntimeId() == ID2.getRuntimeId();
    }

    public static boolean validate(final ItemDefinition predicted, final ItemDefinition claimed) {
        if (predicted == null) {
            // Our fault?
            return true;
        }

        if (claimed == null) {
            return false;
        }

        if (!StringUtil.sanitizePrefix(predicted.getIdentifier()).equalsIgnoreCase(StringUtil.sanitizePrefix(claimed.getIdentifier()))) {
            return false;
        }

        return predicted.getRuntimeId() == claimed.getRuntimeId();
    }
}