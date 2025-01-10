package ac.boar.protocol.network;

import ac.boar.protocol.PacketEvents;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.CloudburstPacketListener;
import ac.boar.util.GeyserUtil;
import lombok.NonNull;

import ac.boar.anticheat.player.BoarPlayer;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.session.UpstreamSession;

public final class CloudburstSendListener extends UpstreamSession {
    private final BoarPlayer player;

    public CloudburstSendListener(BoarPlayer player, BedrockServerSession session) {
        super(session);
        this.player = player;
    }

    @Override
    public void sendPacket(@NonNull BedrockPacket packet) {
        final CloudburstPacketEvent event = new CloudburstPacketEvent(this.player, packet);
        for (final CloudburstPacketListener listener : PacketEvents.getApi().getCloudburst().getListeners()) {
            listener.onPacketSend(event, false);
        }

        if (event.isCancelled()) {
            return;
        }

        if (packet instanceof StartGamePacket startGamePacket) {
            player.runtimeEntityId = startGamePacket.getRuntimeEntityId();
            player.javaEntityId = player.getSession().getPlayerEntity().getEntityId();

            player.x = startGamePacket.getPlayerPosition().getX();
            player.y = startGamePacket.getPlayerPosition().getY() - EntityDefinitions.PLAYER.offset();
            player.z = startGamePacket.getPlayerPosition().getZ();

            player.updateBoundingBox(player.x, player.y, player.z);

            GeyserUtil.injectMCPL(this.player);
            player.compensatedWorld.loadDimension(false);
            player.loadBlockMappings();
        }

        super.sendPacket(event.getPacket());
        event.getPostTasks().forEach(Runnable::run);
        event.getPostTasks().clear();
    }

    @Override
    public void sendPacketImmediately(@NonNull BedrockPacket packet) {
        final CloudburstPacketEvent event = new CloudburstPacketEvent(this.player, packet);
        for (final CloudburstPacketListener listener : PacketEvents.getApi().getCloudburst().getListeners()) {
            listener.onPacketSend(event, true);
        }

        if (event.isCancelled()) {
            return;
        }

        super.sendPacketImmediately(event.getPacket());
        event.getPostTasks().forEach(Runnable::run);
        event.getPostTasks().clear();
    }
}