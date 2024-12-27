package ac.boar.anticheat.packets.other;

import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.CloudburstPacketListener;
import ac.boar.util.GeyserUtil;
import org.cloudburstmc.protocol.bedrock.packet.NetworkStackLatencyPacket;

public class NetworkLatencyPacket implements CloudburstPacketListener {
    private static final long LATENCY_MAGNITUDE = 1000000L;

    @Override
    public void onPacketReceived(CloudburstPacketEvent event) {
        if (!(event.getPacket() instanceof NetworkStackLatencyPacket packet)) {
            return;
        }

        long id = packet.getTimestamp();
        if (id >= 0 || (id / -GeyserUtil.MAGIC_FORM_IMAGE_HACK_TIMESTAMP) % 10 == 0 || (-GeyserUtil.MAGIC_FORM_IMAGE_HACK_TIMESTAMP / id) % 10 == 0) {
            return;
        }

        boolean cancelled = event.getPlayer().latencyUtil.confirmTransaction(Math.abs(id / LATENCY_MAGNITUDE));
        event.setCancelled(cancelled);
    }
}