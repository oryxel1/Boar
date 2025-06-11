package ac.boar.protocol.mitm;

import ac.boar.anticheat.Boar;
import ac.boar.protocol.PacketEvents;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.PacketListener;
import lombok.NonNull;

import ac.boar.anticheat.player.BoarPlayer;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.data.AuthoritativeMovementMode;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket;
import org.geysermc.geyser.level.BedrockDimension;
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
        for (final PacketListener listener : PacketEvents.getApi().getListeners()) {
            listener.onPacketSend(event, false);
        }

        if (event.isCancelled()) {
            return;
        }

        if (event.getPacket() instanceof StartGamePacket start) {
            player.runtimeEntityId = start.getRuntimeEntityId();
            player.javaEntityId = player.getSession().getPlayerEntity().getEntityId();

            int dimensionId = start.getDimensionId();
            player.compensatedWorld.setDimension(dimensionId == BedrockDimension.OVERWORLD_ID ? BedrockDimension.OVERWORLD : dimensionId == BedrockDimension.BEDROCK_NETHER_ID ? BedrockDimension.THE_NETHER : BedrockDimension.THE_END);
            player.currentLoadingScreen = null;
            player.inLoadingScreen = true;

            // We need this to do rewind teleport.
            start.setAuthoritativeMovementMode(AuthoritativeMovementMode.SERVER_WITH_REWIND);
            start.setRewindHistorySize(Boar.getInstance().getConfig().rewindHistory());

            player.sendLatencyStack();
            player.latencyUtil.addTaskToQueue(player.sentStackId.get(), () -> player.gameType = start.getPlayerGameType());
        }

        super.sendPacket(event.getPacket());
        event.getPostTasks().forEach(Runnable::run);
        event.getPostTasks().clear();
    }

    @Override
    public void sendPacketImmediately(@NonNull BedrockPacket packet) {
        final CloudburstPacketEvent event = new CloudburstPacketEvent(this.player, packet);
        for (final PacketListener listener : PacketEvents.getApi().getListeners()) {
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