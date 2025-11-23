package ac.boar.protocol.mitm;

import ac.boar.protocol.PacketEvents;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.PacketListener;
import lombok.Getter;

import ac.boar.anticheat.player.BoarPlayer;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.cloudburstmc.protocol.common.PacketSignal;

@RequiredArgsConstructor
@Getter
public final class CloudburstReceiveListener implements BedrockPacketHandler {
    private final BoarPlayer player;
    private final BedrockPacketHandler oldHandler;

    @Override
    public PacketSignal handlePacket(BedrockPacket packet) {
        if (player.isClosed()) {
            return PacketSignal.HANDLED;
        }

        final CloudburstPacketEvent event = new CloudburstPacketEvent(this.player, packet);
        for (final PacketListener listener : PacketEvents.getApi().getListeners()) {
            listener.onPacketReceived(event);
        }

        if (event.isCancelled()) {
            return PacketSignal.HANDLED;
        }

        return oldHandler.handlePacket(event.getPacket());
    }

    @Override
    public void onDisconnect(CharSequence reason) {
        this.oldHandler.onDisconnect(reason);
    }
}