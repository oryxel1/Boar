package ac.boar.geyser.util;

import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.protocol.mitm.CloudburstReceiveListener;
import ac.boar.protocol.mitm.CloudburstSendListener;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.geysermc.geyser.network.UpstreamPacketHandler;
import org.geysermc.geyser.session.GeyserSession;

import java.lang.reflect.Field;

public class GeyserUtil {
    public final static long MAGIC_FORM_IMAGE_HACK_TIMESTAMP = -1234567890L;
    public static final long MAGIC_VIRTUAL_INVENTORY_HACK = -9876543210L;

    public static void hookIntoCloudburstMC(final BoarPlayer player) {
        try {
            injectCloudburstUpstream(player);
            injectCloudburstDownstream(player);
        } catch (Exception ignored) {
            player.kick("Failed to hook into cloudburst session!");
        }
    }

    private static void injectCloudburstDownstream(final BoarPlayer player) {
        final BedrockServerSession session = player.getCloudburstDownstream();
        final BedrockPacketHandler handler = session.getPacketHandler();
        session.setPacketHandler(player.downstreamPacketHandler = new CloudburstReceiveListener(player, (UpstreamPacketHandler) handler));
    }

    private static void injectCloudburstUpstream(final BoarPlayer player) throws Exception {
        final BedrockServerSession session = player.getCloudburstDownstream();
        final Field upstream = GeyserSession.class.getDeclaredField("upstream");
        upstream.setAccessible(true);
        upstream.set(player.getSession(), player.cloudburstUpstream = new CloudburstSendListener(player, session));
    }
}