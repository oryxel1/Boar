package ac.boar.protocol.network;

import ac.boar.protocol.PacketEvents;
import ac.boar.protocol.event.MCPLPacketEvent;
import ac.boar.protocol.listener.MCPLPacketListener;
import ac.boar.util.MathUtil;
import lombok.RequiredArgsConstructor;

import ac.boar.anticheat.player.BoarPlayer;

import org.cloudburstmc.protocol.bedrock.data.PlayerActionType;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.network.event.session.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;

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

        // We already handle this ourselves, no need for geyser to check and confirm player teleport.
        if (event.getPacket() instanceof ServerboundAcceptTeleportationPacket) {
            player.getSession().setUnconfirmedTeleport(null);
        }

        // We remove the one that Geyser actually translate.
        if (event.getPacket() instanceof ServerboundPlayerActionPacket packet) {
            switch (packet.getAction()) {
                case START_DIGGING -> player.breakingValidator.getValid().removeIf(filter ->
                        filter.getAction() == PlayerActionType.START_BREAK &&
                                MathUtil.compare(packet.getPosition(), filter.getBlockPosition()));
                case CANCEL_DIGGING -> player.breakingValidator.getValid().removeIf(filter ->
                        filter.getAction() == PlayerActionType.ABORT_BREAK &&
                                MathUtil.compare(packet.getPosition(), filter.getBlockPosition()));
                case FINISH_DIGGING -> player.breakingValidator.getValid().removeIf(filter ->
                        filter.getAction() == PlayerActionType.STOP_BREAK &&
                                MathUtil.compare(packet.getPosition(), filter.getBlockPosition()));
            }
        }
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
