package ac.boar.anticheat.packets.player;

import ac.boar.anticheat.compensated.CompensatedInventory;
import ac.boar.anticheat.compensated.cache.container.ContainerCache;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.CloudburstPacketListener;
import org.cloudburstmc.protocol.bedrock.packet.*;

public class InventoryPacket implements CloudburstPacketListener {
    @Override
    public void onPacketReceived(final CloudburstPacketEvent event) {
        final BoarPlayer player = event.getPlayer();
        final CompensatedInventory inventory = player.compensatedInventory;

        if (event.getPacket() instanceof InventoryTransactionPacket packet) {
            player.transactionValidator.handle(packet);
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
    }

    @Override
    public void onPacketSend(final CloudburstPacketEvent event, final boolean immediate) {
        final BoarPlayer player = event.getPlayer();
        final CompensatedInventory inventory = player.compensatedInventory;

        if (event.getPacket() instanceof ContainerOpenPacket packet) {
            player.sendTransaction(immediate);
            player.latencyUtil.addTransactionToQueue(player.lastSentId, () -> inventory.openContainer = new ContainerCache(packet.getId(), packet.getType(), packet.getBlockPosition(), packet.getUniqueEntityId()));
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
