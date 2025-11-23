package ac.boar.protocol.mitm;

import ac.boar.anticheat.Boar;
import ac.boar.anticheat.util.DimensionUtil;
import ac.boar.anticheat.validator.blockbreak.ServerBreakBlockValidator;
import ac.boar.protocol.PacketEvents;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.PacketListener;
import lombok.NonNull;

import ac.boar.anticheat.player.BoarPlayer;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.data.AuthoritativeMovementMode;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateClientInputLocksPacket;
import org.geysermc.geyser.session.UpstreamSession;

public final class CloudburstSendListener extends UpstreamSession {
    private final BoarPlayer player;
    private final UpstreamSession oldSession;

    public CloudburstSendListener(BoarPlayer player, BedrockServerSession session, UpstreamSession oldSession) {
        super(session);
        this.player = player;
        this.oldSession = oldSession;
    }

    @Override
    public void disconnect(String reason) {
        oldSession.disconnect(reason);
    }

    @Override
    public void sendPacket(@NonNull BedrockPacket packet) {
        if (player.isClosed()) {
            return;
        }

        if (packet instanceof UpdateClientInputLocksPacket) {
            // Nope, don't, pain in the ass to support this.
            return;
        }

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

            player.compensatedWorld.setDimension(DimensionUtil.dimensionFromId(start.getDimensionId()));
            player.currentLoadingScreen = null;
            player.inLoadingScreen = true;

            // We need this to do rewind teleport.
            start.setAuthoritativeMovementMode(AuthoritativeMovementMode.SERVER_WITH_REWIND);
            start.setRewindHistorySize(Boar.getConfig().rewindHistory());
            player.serverBreakBlockValidator = new ServerBreakBlockValidator(player);

            player.sendLatencyStack();
            player.getLatencyUtil().addTaskToQueue(player.sentStackId.get(), () -> player.gameType = start.getPlayerGameType());
        }

        oldSession.sendPacket(event.getPacket());
        event.getPostTasks().forEach(Runnable::run);
        event.getPostTasks().clear();
    }

    @Override
    public void sendPacketImmediately(@NonNull BedrockPacket packet) {
        if (player.isClosed()) {
            return;
        }

        final CloudburstPacketEvent event = new CloudburstPacketEvent(this.player, packet);
        for (final PacketListener listener : PacketEvents.getApi().getListeners()) {
            listener.onPacketSend(event, true);
        }

        if (event.isCancelled()) {
            return;
        }

        oldSession.sendPacketImmediately(event.getPacket());
        event.getPostTasks().forEach(Runnable::run);
        event.getPostTasks().clear();
    }
}