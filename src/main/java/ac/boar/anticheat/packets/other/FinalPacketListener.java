package ac.boar.anticheat.packets.other;

import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.math.Vec3f;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.CloudburstPacketListener;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.geysermc.geyser.entity.EntityDefinitions;

public class FinalPacketListener implements CloudburstPacketListener {
    @Override
    public void onPacketReceived(final CloudburstPacketEvent event) {
        final BoarPlayer player = event.getPlayer();

        if (event.getPacket() instanceof PlayerAuthInputPacket packet) {
            player.lastTickWasTeleport = false;
            // This packet doesn't matter, player supposed to be in the teleported position by now.
            // Cancel it don't let any position pass through unless they properly accept it.
            // Geyser also do this, but we made it stricter by checking for lastReceivedId, player can't accept it if they're still lagging.
            if (player.teleportUtil.teleportInQueue()) {
                event.setCancelled(true);
            }

            if (event.isCancelled()) {
                return;
            }

            player.teleportUtil.setLastKnowValid(packet.getTick(), new Vec3f(player.x, player.y + EntityDefinitions.PLAYER.offset(), player.z));
        }
    }
}