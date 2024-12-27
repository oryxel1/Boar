package ac.boar.anticheat.player;

import ac.boar.anticheat.compensated.CompensatedWorld;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import ac.boar.anticheat.player.data.PlayerData;
import ac.boar.anticheat.util.ChatUtil;
import ac.boar.util.GeyserUtil;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.cloudburstmc.protocol.bedrock.packet.NetworkStackLatencyPacket;
import org.geysermc.geyser.registry.type.GeyserBedrockBlock;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.network.tcp.TcpSession;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodecHelper;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class BoarPlayer extends PlayerData {
    @Getter
    private final GeyserSession session;
    public BedrockServerSession cloudburstSession;
    public TcpSession mcplSession;

    public final long joinedTime = System.currentTimeMillis();
    public long runtimeEntityId, javaEntityId;

    // Lag compensation
    public final CompensatedWorld compensatedWorld = new CompensatedWorld(this);

    // Mappings
    public final Map<BlockDefinition, Integer> bedrockToJavaBlocks = new HashMap<>();

    public void loadBlockMappings() {
        final GeyserBedrockBlock[] javaToBedrockBlocks = this.session.getBlockMappings().getJavaToBedrockBlocks();
        for (int i = 0; i < javaToBedrockBlocks.length; i++) {
            this.bedrockToJavaBlocks.put(javaToBedrockBlocks[i], i);
        }
    }

    public void sendTransaction() {
        sendTransaction(false);
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

    // Mappings related
    public int bedrockToJavaBlockId(final BlockDefinition definition) {
        return this.bedrockToJavaBlocks.getOrDefault(definition, -1);
    }

    // Other
    public MinecraftCodecHelper getCodecHelper() {
        return (MinecraftCodecHelper) this.mcplSession.getCodecHelper();
    }
}
