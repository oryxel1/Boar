package ac.boar.anticheat.packets.input;

import ac.boar.anticheat.data.input.TickData;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.PacketListener;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;

public class PostAuthInputPackets implements PacketListener {
    @Override
    public void onPacketReceived(final CloudburstPacketEvent event) {
        final BoarPlayer player = event.getPlayer();

        if (event.getPacket() instanceof PlayerAuthInputPacket packet) {
            player.dirtyRiptide = false;
            player.thisTickSpinAttack = false;
            player.getTeleportUtil().getAuthInputHistory().put(packet.getTick(), new TickData(packet, player.getFlagTracker().cloneFlags()));

            // Self-explanatory, player ain't supposed to move if they're teleporting.
            if (player.getTeleportUtil().isTeleporting()) {
                event.setCancelled(true);
                return;
            }

            player.getTeleportUtil().clearRewindHistory();
        }
    }
}