package ac.boar.anticheat.packets.other;

import ac.boar.geyser.util.GeyserUtil;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.PacketListener;
import org.cloudburstmc.protocol.bedrock.packet.NetworkStackLatencyPacket;

public class NetworkLatencyPackets implements PacketListener {
    private final static long LATENCY_MAGNITUDE = 1000000L;

    @Override
    public void onPacketReceived(final CloudburstPacketEvent event) {
        if (!(event.getPacket() instanceof NetworkStackLatencyPacket packet)) {
            return;
        }

        long id = packet.getTimestamp() / LATENCY_MAGNITUDE;
        if (id >= 0 || id == GeyserUtil.MAGIC_FORM_IMAGE_HACK_TIMESTAMP || id == GeyserUtil.MAGIC_VIRTUAL_INVENTORY_HACK) {
            return;
        }

        boolean cancelled = event.getPlayer().latencyUtil.confirmTransaction(Math.abs(id));
        event.setCancelled(cancelled);
    }
}