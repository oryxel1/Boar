package ac.boar.anticheat.packets.other;

import ac.boar.geyser.util.GeyserUtil;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.PacketListener;
import org.cloudburstmc.protocol.bedrock.packet.NetworkStackLatencyPacket;

public class NetworkLatencyPackets implements PacketListener {
    public final static long LATENCY_MAGNITUDE = 1000000L;

    @Override
    public void onPacketReceived(final CloudburstPacketEvent event) {
        if (!(event.getPacket() instanceof NetworkStackLatencyPacket packet)) {
            return;
        }

        long id = packet.getTimestamp() / LATENCY_MAGNITUDE;

        // Positive id is for keep alive passthrough hack, and there also only 2 other negative id that we just need to check for.
        // This implementation could be a problem later on considering that Networking API will soon become a thing but oh welp.
        if (id >= 0 || id == GeyserUtil.MAGIC_FORM_IMAGE_HACK_TIMESTAMP || id == GeyserUtil.MAGIC_VIRTUAL_INVENTORY_HACK) {
            return;
        }

        event.getPlayer().getLatencyUtil().confirmStackId(Math.abs(id));

        // No reason to pass these to Geyser since it won't use it anyway... If Geyser suddenly decide to send a new id for a new hack
        // then manually hardcoded it here... It's easier to handle thing that way.
        event.setCancelled(true);
    }
}