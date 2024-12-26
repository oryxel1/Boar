package ac.boar.anticheat.player.api;

import ac.boar.anticheat.util.ChatUtil;
import ac.boar.util.GeyserUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.network.tcp.TcpSession;

@RequiredArgsConstructor
public class BoarPlayer {
    @Getter
    private final GeyserSession session;
    @Getter
    @Setter
    private BedrockServerSession cloudburstSession;
    @Getter
    @Setter
    private TcpSession mcplSession;

    public final long joinedTime = System.currentTimeMillis();
    public long runtimeEntityId, javaEntityId;

    public void init() {
        GeyserUtil.injectCloudburst(this);
    }

    public void disconnect(String reason) {
        this.session.disconnect(ChatUtil.PREFIX + " " + reason);
    }
}
