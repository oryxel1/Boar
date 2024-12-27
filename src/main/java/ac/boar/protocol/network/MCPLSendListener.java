package ac.boar.protocol.network;

import ac.boar.protocol.PacketEvents;
import ac.boar.protocol.event.MCPLPacketEvent;
import ac.boar.protocol.listener.MCPLPacketListener;
import lombok.RequiredArgsConstructor;

import ac.boar.anticheat.player.BoarPlayer;

import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.network.event.session.*;

import java.util.List;

@RequiredArgsConstructor
public class MCPLSendListener extends SessionAdapter {
    private final BoarPlayer player;
    private final List<SessionListener> listeners;

    @Override
    public void packetReceived(Session session, Packet packet) {
        // if (session != player.getTcpSession()) return;

        final MCPLPacketEvent event = new MCPLPacketEvent(this.player, packet);
        for (final MCPLPacketListener listener : PacketEvents.getApi().getMcpl().getListeners()) {
            listener.onPacketSend(event);
        }
        if (!event.isCancelled()) {
            listeners.forEach(l -> l.packetReceived(session, packet));
        }

        event.getPostTasks().forEach(Runnable::run);
        event.getPostTasks().clear();
    }

    @Override
    public void packetSending(PacketSendingEvent event) {
        listeners.forEach(l -> l.packetSending(event));
    }

    @Override
    public void connected(ConnectedEvent event) {
        listeners.forEach(l -> l.connected(event));
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        listeners.forEach(l -> l.disconnected(event));
    }

    @Override
    public void packetError(PacketErrorEvent event) {
        listeners.forEach(l -> l.packetError(event));
    }
}
