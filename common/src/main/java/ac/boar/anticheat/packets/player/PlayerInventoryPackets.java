package ac.boar.anticheat.packets.player;

import ac.boar.anticheat.Boar;
import ac.boar.anticheat.ack.types.ContainerOpenAck;
import ac.boar.anticheat.ack.types.CraftingDataAck;
import ac.boar.anticheat.ack.types.CreativeContentAck;
import ac.boar.anticheat.ack.types.HotbarSlotAck;
import ac.boar.anticheat.ack.types.InventoryContentAck;
import ac.boar.anticheat.ack.types.InventorySlotAck;
import ac.boar.anticheat.ack.types.UpdateTradeAck;
import ac.boar.anticheat.compensated.CompensatedInventory;
import ac.boar.anticheat.check.impl.inventory.Inventory;
import ac.boar.anticheat.data.ItemUseTracker;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.validator.inventory.ItemTransactionValidator;
import ac.boar.protocol.api.CloudburstPacketEvent;
import ac.boar.protocol.api.PacketListener;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.packet.*;

public class PlayerInventoryPackets implements PacketListener {
    @Override
    public void onPacketReceived(final CloudburstPacketEvent event) {
        final BoarPlayer player = event.getPlayer();
        final CompensatedInventory inventory = player.compensatedInventory;

        if (event.getPacket() instanceof InventoryTransactionPacket packet) {
            try { // In case I messed up.
                boolean cancelled = !player.transactionValidator.handle(packet);
                if (cancelled) {
                    final String details = "invalid transaction: " + player.transactionValidator.getFailReason()
                            + " | type=" + packet.getTransactionType()
                            + ", action=" + packet.getActionType()
                            + ", hotbarSlot=" + packet.getHotbarSlot()
                            + ", heldSlot=" + inventory.heldItemSlot
                            + ", blockPos=" + packet.getBlockPosition()
                            + ", blockFace=" + packet.getBlockFace()
                            + ", clickPos=" + packet.getClickPosition()
                            + ", itemInHand=" + ItemTransactionValidator.describe(packet.getItemInHand())
                            + ", actions=" + packet.getActions().size()
                            + ", gameType=" + player.gameType;

                    if (player.disableMitigations()) {
                        player.getCheckHolder().manuallyFail(Inventory.class, details);
                    } else {
                        Boar.debug(player.getSession().name() + ": " + details, Boar.DebugMessage.WARNING);
                    }
                }
//                if (cancelled) {
//                    System.out.println("Cancel inventory action: " + packet);
//                }
                event.setCancelled(cancelled && !player.disableMitigations());
            } catch (Exception exception) {
                Boar.getInstance().getPlatform().logger().error(
                        "Failed to validate inventory transaction for player " + player.getSession().name()
                                + ": type=" + packet.getTransactionType()
                                + ", action=" + packet.getActionType()
                                + ", slot=" + packet.getHotbarSlot()
                                + ", blockPosition=" + packet.getBlockPosition(),
                        exception);
            }
        }

        if (event.getPacket() instanceof ItemStackRequestPacket packet) {
            // TODO: Reverse engineer the 1.26.30 client and compare against other server software(s) to make sure inventory handling is matching.
            // Also should look into why certain actions are failing to be validated.
            if (!player.transactionValidator.handle(packet)) {
                final String details = "invalid item stack request: " + player.transactionValidator.getFailReason()
                        + " | requests=" + packet.getRequests().size()
                        + ", openContainer=" + (inventory.openContainer == null ? "none"
                                : inventory.openContainer.getType() + ":" + inventory.openContainer.getId())
                        + ", gameType=" + player.gameType;

                if (player.disableMitigations()) {
                    player.getCheckHolder().manuallyFail(Inventory.class, details);
                } else {
                    Boar.debug(player.getSession().name() + ": " + details, Boar.DebugMessage.WARNING);
                }
            }
        }

        if (event.getPacket() instanceof InteractPacket packet) {
            if (player.runtimeEntityId != packet.getRuntimeEntityId()) {
                return;
            }

            // This is controlled by server as Geyser use server auth.
            if (packet.getAction() == InteractPacket.Action.OPEN_INVENTORY) {
                // player.compensatedInventory.openContainer = player.compensatedInventory.inventoryContainer;
            }
        }

        if (event.getPacket() instanceof ContainerClosePacket packet) {
            if (inventory.openContainer == null) {
                return;
            }

            if (packet.getId() != inventory.openContainer.getId() && packet.getId() != -1) {
                return;
            }

            inventory.openContainer = null;
        }

        if (event.getPacket() instanceof MobEquipmentPacket packet) {
            final int newSlot = packet.getHotbarSlot();
            if (player.runtimeEntityId != packet.getRuntimeEntityId()) {
                return;
            }

            if (newSlot < 0 || newSlot > 8 || packet.getContainerId() != ContainerId.INVENTORY || inventory.heldItemSlot == newSlot) {
                return;
            }

            inventory.heldItemSlot = newSlot;

            if (player.getItemUseTracker().getItem() != null || player.getFlagTracker().has(EntityFlag.USING_ITEM)) {
                player.getItemUseTracker().release();
                player.getItemUseTracker().setDirtyUsing(ItemUseTracker.DirtyUsing.NONE);
            }
        }
    }

    @Override
    public void onPacketSend(final CloudburstPacketEvent event) {
        final BoarPlayer player = event.getPlayer();
        final CompensatedInventory inventory = player.compensatedInventory;

        if (event.getPacket() instanceof CreativeContentPacket packet) {
            player.queueAcknowledgment(new CreativeContentAck(packet.getContents()));
        }

        if (event.getPacket() instanceof CraftingDataPacket packet) {
            player.queueAcknowledgment(new CraftingDataAck(packet.getCraftingData(), packet.getPotionMixData()));
        }

        if (event.getPacket() instanceof ContainerOpenPacket packet) {
            // System.out.println(packet);
            player.queueAcknowledgment(new ContainerOpenAck(packet.getId(), packet.getType(), packet.getBlockPosition(), packet.getUniqueEntityId()));
        }
//
        if (event.getPacket() instanceof UpdateEquipPacket packet) {
//            System.out.println(packet);
//            player.sendLatencyStack();
//            player.getLatencyUtil().addTaskToQueue(player.sentStackId.get(), () -> { try {
//                inventory.openContainer = new ContainerCache((byte) packet.getWindowId(),
//                        ContainerType.from(packet.getWindowType()), Vector3i.ZERO, packet.getUniqueEntityId());
//            } catch (Exception ignored) {}});
        }
//
        if (event.getPacket() instanceof UpdateTradePacket packet) {
            if (packet.getPlayerUniqueEntityId() != player.runtimeEntityId || packet.getContainerType() != ContainerType.TRADE) {
                return;
            }

            player.sendLatencyStack(new UpdateTradeAck((byte) packet.getContainerId(), packet.getContainerType(), packet.getOffers(), packet.getTraderUniqueEntityId()));
        }

        if (event.getPacket() instanceof InventorySlotPacket packet) {
            player.sendLatencyStack(new InventorySlotAck(packet.getContainerId(), packet.getSlot(), packet.getItem(), packet.getStorageItem()));
        }

        if (event.getPacket() instanceof InventoryContentPacket packet) {
            player.sendLatencyStack(new InventoryContentAck(packet.getContainerId(), packet.getContents(), packet.getStorageItem()));
        }

        if (event.getPacket() instanceof PlayerHotbarPacket packet) {
            if (packet.getContainerId() != inventory.inventoryContainer.getId() || !packet.isSelectHotbarSlot()) {
                return;
            }

            final int slot = packet.getSelectedHotbarSlot();
            if (slot >= 0 && slot < 9) {
                player.sendLatencyStack(new HotbarSlotAck(slot));
            }
        }
    }
}
