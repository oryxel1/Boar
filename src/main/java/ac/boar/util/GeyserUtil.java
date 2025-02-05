package ac.boar.util;

import ac.boar.anticheat.player.BoarPlayer;

import ac.boar.protocol.network.CloudburstReceiveListener;
import ac.boar.protocol.network.CloudburstSendListener;
import ac.boar.protocol.network.MCPLSendListener;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.data.PlayerActionType;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.cloudburstmc.protocol.bedrock.packet.PlayerActionPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.CooldownUtils;
import org.geysermc.mcprotocollib.network.ClientSession;
import org.geysermc.mcprotocollib.network.event.session.SessionListener;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerState;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerAbilitiesPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerCommandPacket;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class GeyserUtil {
    public final static long MAGIC_FORM_IMAGE_HACK_TIMESTAMP = 1234567890L;

    public static void syncInputData(final BoarPlayer player, final boolean send, final PlayerAuthInputPacket packet) {
        final GeyserSession session = player.getSession();
        final SessionPlayerEntity entity = session.getPlayerEntity();

        for (PlayerAuthInputData input : packet.getInputData()) {
            switch (input) {
                case PERFORM_ITEM_INTERACTION -> {
                    return; // processItemUseTransaction(session, packet.getItemUseTransaction());
                }
                case PERFORM_BLOCK_ACTIONS -> {
                    return; // BedrockBlockActions.translate(session, packet.getPlayerActions());
                }
                case START_SNEAKING -> {
                    if (send) {
                        ServerboundPlayerCommandPacket startSneakPacket = new ServerboundPlayerCommandPacket(entity.getEntityId(), PlayerState.START_SNEAKING);
                        session.sendDownstreamGamePacket(startSneakPacket);
                    }

                    session.startSneaking();
                }
                case STOP_SNEAKING -> {
                    if (send) {
                        ServerboundPlayerCommandPacket stopSneakPacket = new ServerboundPlayerCommandPacket(entity.getEntityId(), PlayerState.STOP_SNEAKING);
                        session.sendDownstreamGamePacket(stopSneakPacket);
                    }

                    session.stopSneaking();
                }
                case START_SPRINTING -> {
                    if (!entity.getFlag(EntityFlag.SWIMMING)) {
                        if (send) {
                            ServerboundPlayerCommandPacket startSprintPacket = new ServerboundPlayerCommandPacket(entity.getEntityId(), PlayerState.START_SPRINTING);
                            session.sendDownstreamGamePacket(startSprintPacket);
                        }

                        session.setSprinting(true);
                    }
                }
                case STOP_SPRINTING -> {
                    if (!entity.getFlag(EntityFlag.SWIMMING) && send) {
                        ServerboundPlayerCommandPacket stopSprintPacket = new ServerboundPlayerCommandPacket(entity.getEntityId(), PlayerState.STOP_SPRINTING);
                        session.sendDownstreamGamePacket(stopSprintPacket);
                    }
                    session.setSprinting(false);
                }
                case START_SWIMMING -> session.setSwimming(true);
                case STOP_SWIMMING -> session.setSwimming(false);
                case START_CRAWLING -> session.setCrawling(true);
                case STOP_CRAWLING -> session.setCrawling(false);
                case START_FLYING -> { // Since 1.20.30
                    if (session.isCanFly()) {
                        if (session.getGameMode() == GameMode.SPECTATOR) {
                            // should already be flying
                            session.sendAdventureSettings();
                            break;
                        }

                        if (session.getPlayerEntity().getFlag(EntityFlag.SWIMMING) && session.getCollisionManager().isPlayerInWater()) {
                            // As of 1.18.1, Java Edition cannot fly while in water, but it can fly while crawling
                            // If this isn't present, swimming on a 1.13.2 server and then attempting to fly will put you into a flying/swimming state that is invalid on JE
                            session.sendAdventureSettings();
                            break;
                        }

                        session.setFlying(true);

                        if (send) {
                            session.sendDownstreamGamePacket(new ServerboundPlayerAbilitiesPacket(true));
                        }
                    } else {
                        // update whether we can fly
                        session.sendAdventureSettings();
                        // stop flying
                        if (send) {
                            PlayerActionPacket stopFlyingPacket = new PlayerActionPacket();
                            stopFlyingPacket.setRuntimeEntityId(session.getPlayerEntity().getGeyserId());
                            stopFlyingPacket.setAction(PlayerActionType.STOP_FLYING);
                            stopFlyingPacket.setBlockPosition(Vector3i.ZERO);
                            stopFlyingPacket.setResultPosition(Vector3i.ZERO);
                            stopFlyingPacket.setFace(0);
                            session.sendUpstreamPacket(stopFlyingPacket);
                        }
                    }
                }
                case STOP_FLYING -> {
                    session.setFlying(false);
                    if (send) {
                        session.sendDownstreamGamePacket(new ServerboundPlayerAbilitiesPacket(false));
                    }
                }
                case START_GLIDING -> {
                    if (send) {
                        // Otherwise gliding will not work in creative
                        ServerboundPlayerAbilitiesPacket playerAbilitiesPacket = new ServerboundPlayerAbilitiesPacket(false);
                        session.sendDownstreamGamePacket(playerAbilitiesPacket);
                        ServerboundPlayerCommandPacket glidePacket = new ServerboundPlayerCommandPacket(entity.getEntityId(), PlayerState.START_ELYTRA_FLYING);
                        session.sendDownstreamGamePacket(glidePacket);
                    }
                }
                case STOP_GLIDING -> {
                    if (send) {
                        ServerboundPlayerCommandPacket glidePacket = new ServerboundPlayerCommandPacket(entity.getEntityId(), PlayerState.START_ELYTRA_FLYING);
                        session.sendDownstreamGamePacket(glidePacket);
                    }
                }
                case MISSED_SWING -> CooldownUtils.sendCooldown(session); // Java edition sends a cooldown when hitting air.
            }
        }
    }

    public static void injectCloudburst(final BoarPlayer player) {
        final GeyserConnection connection = player.getSession();

        try {
            player.cloudburstSession = findCloudburstSession(connection);

            injectCloudburstUpstream(player);
            injectCloudburstDownstream(player);
        } catch (Exception ignored) {
            player.disconnect("Failed to inject into cloudburst session!");
        }
    }

    public static void injectMCPL(final BoarPlayer player) {
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