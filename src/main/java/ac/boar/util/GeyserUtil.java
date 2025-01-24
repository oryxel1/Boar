package ac.boar.util;

import ac.boar.anticheat.player.BoarPlayer;

import ac.boar.protocol.network.CloudburstReceiveListener;
import ac.boar.protocol.network.CloudburstSendListener;
import ac.boar.protocol.network.MCPLSendListener;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.network.ClientSession;
import org.geysermc.mcprotocollib.network.event.session.SessionListener;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class GeyserUtil {
    public static final long MAGIC_FORM_IMAGE_HACK_TIMESTAMP = 1234567890L;

    public static void injectCloudburst(BoarPlayer player) {
        final GeyserConnection connection = player.getSession();

        try {
            player.cloudburstSession = findCloudburstSession(connection);

            injectCloudburstUpstream(player);
            injectCloudburstDownstream(player);
        } catch (Exception ignored) {
            player.disconnect("Failed to inject into cloudburst session!");
        }
    }

    public static void injectMCPL(BoarPlayer player) {
        final GeyserConnection connection = player.getSession();

        try {
            final ClientSession session = findClientSession(connection);

            List<SessionListener> adapters = new ArrayList<>(session.getListeners());
            session.getListeners().forEach(session::removeListener);
            session.addListener(new MCPLSendListener(player, adapters));

            player.mcplSession = session;
        } catch (Exception ignored) {
            player.disconnect("Failed to inject into MCPL session!");
        }
    }

    private static void injectCloudburstDownstream(final BoarPlayer player) {
        final BedrockServerSession session = player.cloudburstSession;
        final BedrockPacketHandler handler = session.getPacketHandler();
        session.setPacketHandler(new CloudburstReceiveListener(player, handler));
    }

    private static void injectCloudburstUpstream(final BoarPlayer player) throws Exception {
        final BedrockServerSession session = player.cloudburstSession;
        final Field upstream = GeyserSession.class.getDeclaredField("upstream");
        upstream.setAccessible(true);
        upstream.set(player.getSession(), new CloudburstSendListener(player, session));
    }

    private static ClientSession findClientSession(final GeyserConnection connection) throws Exception {
        final Field upstream = GeyserSession.class.getDeclaredField("downstream");
        upstream.setAccessible(true);
        final Object session = upstream.get(connection);
        final Field field = session.getClass().getDeclaredField("session");
        field.setAccessible(true);
        return (ClientSession) field.get(session);
    }

    private static BedrockServerSession findCloudburstSession(final GeyserConnection connection) throws Exception {
        final Field upstream = GeyserSession.class.getDeclaredField("upstream");
        upstream.setAccessible(true);
        final Object session = upstream.get(connection);
        final Field field = session.getClass().getDeclaredField("session");
        field.setAccessible(true);
        return (BedrockServerSession) field.get(session);
    }
}