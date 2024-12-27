package ac.boar.anticheat.player;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import ac.boar.anticheat.player.data.PlayerData;
import ac.boar.anticheat.util.ChatUtil;
import ac.boar.util.GeyserUtil;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.packet.NetworkStackLatencyPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.network.tcp.TcpSession;

@RequiredArgsConstructor
public class BoarPlayer extends PlayerData {
    @Getter
    private final GeyserSession session;
    public BedrockServerSession cloudburstSession;
    public TcpSession mcplSession;

    public final long joinedTime = System.currentTimeMillis();
    public long runtimeEntityId, javaEntityId;

    public void init() {
        GeyserUtil.injectCloudburst(this);
    }

    public void sendTransaction(boolean immediate) {
        lastSentId++;
        if (lastSentId == GeyserUtil.MAGIC_FORM_IMAGE_HACK_TIMESTAMP) {
            lastSentId++;
        }

        // We have to send negative values since geyser translate positive one.
        final NetworkStackLatencyPacket latencyPacket = new NetworkStackLatencyPacket();
        latencyPacket.setTimestamp(-lastSentId);
        latencyPacket.setFromServer(true);

        if (immediate) {
            this.cloudburstSession.sendPacketImmediately(latencyPacket);
        } else {
            this.cloudburstSession.sendPacket(latencyPacket);
        }

        this.latencyUtil.getSentTransactions().add(lastSentId);
    }

    public void disconnect(String reason) {
        this.session.disconnect(ChatUtil.PREFIX + " " + reason);
    }
}
