package ac.boar.anticheat.packets.player;

import ac.boar.anticheat.compensated.CompensatedInventory;
import ac.boar.anticheat.compensated.cache.container.ContainerCache;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.CloudburstPacketListener;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.packet.*;

public class InventorySimulationPacket implements CloudburstPacketListener {
    @Override
    public void onPacketReceived(final CloudburstPacketEvent event) {
        final BoarPlayer player = event.getPlayer();
        final CompensatedInventory inventory = player.compensatedInventory;

        if (event.getPacket() instanceof InventoryTransactionPacket packet) {
            event.setCancelled(!player.transactionValidator.handle(packet));
        }

        if (event.getPacket() instanceof ItemStackRequestPacket packet) {
            player.transactionValidator.handle(packet);
        }

        if (event.getPacket() instanceof InteractPacket packet) {
            if (player.runtimeEntityId != packet.getRuntimeEntityId()) {
                return;
            }

            if (packet.getAction() == InteractPacket.Action.OPEN_INVENTORY) {
                player.compensatedInventory.openContainer = player.compensatedInventory.inventoryContainer;
            }
        }

        if (event.getPacket() instanceof ContainerClosePacket packet) {
            if (inventory.openContainer == null) {
                return;
            }

            if (packet.getId() != inventory.openContainer.getId()) {
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
        }
    }

    @Override
    public void onPacketSend(final CloudburstPacketEvent event, final boolean immediate) {
        final BoarPlayer player = event.getPlayer();
        final CompensatedInventory inventory = player.compensatedInventory;

        if (event.getPacket() instanceof InventorySlotPacket packet) {
            player.sendTransaction(immediate);
            player.latencyUtil.addTransactionToQueue(player.lastSentId, () -> {
                final ContainerCache container = inventory.getContainer((byte) packet.getContainerId());
                if (container == null) {
                    return;
                }

                if (packet.getSlot() < 0 || packet.getSlot() >= container.getContents().size()) {
                    return;
                }

                container.getContents().set(packet.getSlot(), packet.getItem());
            });
        }

        if (event.getPacket() instanceof ContainerOpenPacket packet) {
            player.sendTransaction(immediate);
            player.latencyUtil.addTransactionToQueue(player.lastSentId, () -> inventory.openContainer = new ContainerCache(packet.getId(), packet.getType(), packet.getBlockPosition(), packet.getUniqueEntityId()));
        }

        if (event.getPacket() instanceof UpdateEquipPacket packet) {
            player.sendTransaction(immediate);
            player.latencyUtil.addTransactionToQueue(player.lastSentId, () -> { try {
                inventory.openContainer = new ContainerCache((byte) packet.getWindowId(),
                        ContainerType.from(packet.getWindowType()), Vector3i.ZERO, packet.getUniqueEntityId());
            } catch (Exception ignored) {}});
        }

        if (event.getPacket() instanceof UpdateTradePacket packet) {
            if (packet.getPlayerUniqueEntityId() != player.runtimeEntityId) {
                return;
            }

            player.sendTransaction(immediate);
            player.latencyUtil.addTransactionToQueue(player.lastSentId, () -> { try {
                inventory.openContainer = new ContainerCache((byte) packet.getContainerId(), packet.getContainerType(), Vector3i.ZERO, packet.getTraderUniqueEntityId());
            } catch (Exception ignored) {}});
        }

        if (event.getPacket() instanceof InventoryContentPacket packet) {
            player.sendTransaction(immediate);
            player.latencyUtil.addTransactionToQueue(player.lastSentId, () -> {
                final ContainerCache container = inventory.getContainer((byte) packet.getContainerId());
                if (container == null) {
                    return;
                }

                container.setContents(packet.getContents());
            });
        }

        if (event.getPacket() instanceof PlayerHotbarPacket packet) {
            if (packet.getContainerId() != inventory.inventoryContainer.getId() || !packet.isSelectHotbarSlot()) {
                return;
            }

            final int slot = packet.getSelectedHotbarSlot();
            if (slot >= 0 && slot < 9) {
                player.sendTransaction();
                player.latencyUtil.addTransactionToQueue(player.lastSentId, () -> inventory.heldItemSlot = slot);
            }
        }
    }
}
