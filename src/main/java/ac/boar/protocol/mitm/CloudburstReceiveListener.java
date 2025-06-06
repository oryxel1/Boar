package ac.boar.protocol.mitm;

import ac.boar.protocol.PacketEvents;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.PacketListener;
import lombok.Getter;

import ac.boar.anticheat.player.BoarPlayer;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.network.UpstreamPacketHandler;

@Getter
public final class CloudburstReceiveListener extends UpstreamPacketHandler {
    private final BoarPlayer player;

    public CloudburstReceiveListener(BoarPlayer player) {
        super(GeyserImpl.getInstance(), player.getSession());
        this.player = player;
    }

    @Override
    public PacketSignal handlePacket(BedrockPacket packet) {
        final CloudburstPacketEvent event = new CloudburstPacketEvent(this.player, packet);
        for (final PacketListener listener : PacketEvents.getApi().getListeners()) {
            listener.onPacketReceived(event);
        }

        if (event.isCancelled()) {
            return PacketSignal.HANDLED;
        }

        return super.handlePacket(event.getPacket());
    }
}