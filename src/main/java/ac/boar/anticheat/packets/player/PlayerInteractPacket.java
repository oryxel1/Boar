package ac.boar.anticheat.packets.player;

import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.CloudburstPacketListener;
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket;

public class PlayerInteractPacket implements CloudburstPacketListener {
    @Override
    public void onPacketReceived(final CloudburstPacketEvent event) {
        final BoarPlayer player = event.getPlayer();
        if (!(event.getPacket() instanceof InventoryTransactionPacket packet)) {
            return;
        }

        player.transactionValidator.handle(packet);
    }
}
