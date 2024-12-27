package ac.boar.protocol.network;

import ac.boar.protocol.PacketEvents;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.CloudburstPacketListener;
import lombok.RequiredArgsConstructor;

import ac.boar.anticheat.player.BoarPlayer;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.cloudburstmc.protocol.common.PacketSignal;

@RequiredArgsConstructor
public final class CloudburstReceiveListener implements BedrockPacketHandler {
    private final BoarPlayer player;
    private final BedrockPacketHandler oldHandler;

    @Override
    public PacketSignal handlePacket(BedrockPacket packet) {
        final CloudburstPacketEvent event = new CloudburstPacketEvent(this.player, packet);
        for (final CloudburstPacketListener listener : PacketEvents.getApi().getCloudburst().getListeners()) {
            listener.onPacketReceived(event);
        }

        if (event.isCancelled()) {
            return PacketSignal.HANDLED;
        }

        return this.oldHandler.handlePacket(event.getPacket());
    }
}