package ac.boar.anticheat.packets.input;

import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.PacketListener;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.geysermc.geyser.entity.EntityDefinitions;

public class PostAuthInputPackets implements PacketListener {
    @Override
    public void onPacketReceived(final CloudburstPacketEvent event) {
        final BoarPlayer player = event.getPlayer();

        if (event.getPacket() instanceof PlayerAuthInputPacket packet) {
            player.teleportUtil.getAuthInputHistory().put(packet.getTick(), packet);

            player.wasTeleport = false;
            player.wasRewind = false;

            player.teleportUtil.cacheKnowValid(packet.getTick(), player.position.add(0, EntityDefinitions.PLAYER.offset(), 0));

            // This packet doesn't matter, player supposed to be in the teleported position by now.
            // Cancel it don't let any position pass through unless they properly accept it.
            // Geyser also do this, but we made it stricter by checking for receivedStackId.get(), player can't accept it if they're still lagging.
            if (player.teleportUtil.teleportInQueue()) {
                event.setCancelled(true);
            }
        }
    }
}