package ac.boar.anticheat.packets.player;

import ac.boar.anticheat.GlobalSetting;
import ac.boar.anticheat.data.teleport.RewindTeleportCache;
import ac.boar.anticheat.packets.MovementCheckRunner;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.data.teleport.TeleportCache;
import ac.boar.anticheat.prediction.ticker.PlayerTicker;
import ac.boar.anticheat.util.ChatUtil;
import ac.boar.anticheat.util.math.Vec3f;
import ac.boar.plugin.BoarPlugin;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.CloudburstPacketListener;
import ac.boar.util.GeyserUtil;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.geysermc.geyser.entity.EntityDefinitions;

import java.util.Queue;

public class PlayerTeleportPacket implements CloudburstPacketListener {
    private final static float MAX_TOLERANCE_ERROR = 0.001f;

    @Override
    public void onPacketReceived(final CloudburstPacketEvent event) {
        final BoarPlayer player = event.getPlayer();
        if (!(event.getPacket() instanceof PlayerAuthInputPacket packet)) {
            return;
        }

        handleNormalTeleport(player, packet);
        handleRewindTeleport(player, packet);
    }

    // Rewind should be handled separately since it is not teleport. We also need to catch up with the predicted tick.
    private void handleRewindTeleport(final BoarPlayer player, final PlayerAuthInputPacket packet) {
        if (player.lastTickWasTeleport) {
            return;
        }

        final Queue<RewindTeleportCache> queue = player.teleportUtil.getRewindTeleportCaches();
        if (queue.isEmpty()) {
            return;
        }

        RewindTeleportCache cache;
        while ((cache = queue.peek()) != null) {
            if (player.lastReceivedId < cache.getTransactionId()) {
                break;
            }
            queue.poll();

            player.lastTickWasRewind = true;

            final long tickDistance = packet.getTick() - cache.getTick() - 1;

            player.onGround = cache.isOnGround();

            player.eotVelocity = cache.getVelocity();
            player.prevX = cache.getLastPosition().getX();
            player.prevY = cache.getLastPosition().getY() - EntityDefinitions.PLAYER.offset();
            player.prevZ = cache.getLastPosition().getZ();

            player.x = cache.getPosition().getX();
            player.y = cache.getPosition().getY() - EntityDefinitions.PLAYER.offset();
            player.z = cache.getPosition().getZ();

            player.updateBoundingBox(player.prevX, player.prevY, player.prevZ);
            player.updateBoundingBox(player.x, player.y, player.z);

            if (GlobalSetting.REWIND_INFO_DEBUG) {
                ChatUtil.alert("Required ticks to catch up: " + tickDistance);
            }

            if (tickDistance < 1) {
                return;
            }

            long currentTick = cache.getTick();
            for (int i = 0; i < tickDistance; i++) {
                currentTick++;
                if (player.savedInputMap.containsKey(currentTick)) {
                    final PlayerAuthInputPacket old = player.savedInputMap.get(currentTick);

                    GeyserUtil.syncInputData(player, true, old);
                    MovementCheckRunner.processInputMovePacket(player, old);
                }

                player.actualVelocity = new Vec3f(player.x - player.prevX, player.y - player.prevY, player.z - player.prevZ);
                // ChatUtil.alert("Actual velocity: " + player.actualVelocity.toVector3f());

                new PlayerTicker(player).tick();

                player.prevX = player.x;
                player.prevY = player.y;
                player.prevZ = player.z;

                player.x = player.x + player.predictedVelocity.x;
                player.y = player.y + player.predictedVelocity.y;
                player.z = player.z + player.predictedVelocity.z;

                if (player.canControlEOT()) {
                    player.eotVelocity = player.claimedEOT;
                }

                player.postPredictionVelocities.clear();
            }
        }
    }

    private void handleNormalTeleport(final BoarPlayer player, final PlayerAuthInputPacket packet) {
        final Queue<TeleportCache> queue = player.teleportUtil.getTeleportQueue();
        if (queue.isEmpty()) {
            return;
        }

        // We can't check for player.lastReceivedId == cache.getTransactionId() since bedrock teleport is different.
        // Instead of respond with an extra move packet like java, it just simply set position
        // and add HANDLE_TELEPORT to input data next tick to let us know that it accepted the teleport.
        // Which also means player will be in the position of the latest teleport they got and accept that one, not every teleport like Java.
        TeleportCache temp;
        TeleportCache cache = null;
        while ((temp = queue.peek()) != null) {
            if (player.lastReceivedId < temp.getTransactionId()) {
                break;
            }

            cache = queue.poll();
        }

        if (cache == null) {
            return;
        }

        // This is not precise as java, since it being sent this tick instead of right away (also because of floating point I think?), we can't check for 0
        // I have seen it reach 2e-6 in some cases, but I haven't test enough to know.
        double distance = packet.getPosition().distanceSquared(cache.getPosition().toVector3f());
        if (packet.getInputData().contains(PlayerAuthInputData.HANDLE_TELEPORT) && distance < MAX_TOLERANCE_ERROR) {
            BoarPlugin.LOGGER.info("Accepted teleport, d=" + distance);
            player.lastTickWasTeleport = true;
            player.teleportUtil.prevRewindTeleport = cache.getData();
        } else {
            // This is not the latest teleport, just ignore this one, we only force player to accept the latest one.
            // We don't want to teleport player to old teleport position when they're supposed to teleport to the latest tone.
            if (!queue.isEmpty()) {
                return;
            }
            // Set player back to where they're supposed to be.
            player.teleportUtil.setbackTo(cache);
            BoarPlugin.LOGGER.info("Received=" + packet.getPosition().sub(0, EntityDefinitions.PLAYER.offset(), 0)
                    + ", Required=" + cache.getPosition().subtract(0, EntityDefinitions.PLAYER.offset(), 0).toVector3f());
            BoarPlugin.LOGGER.info("Is there handle teleport data: " + packet.getInputData().contains(PlayerAuthInputData.HANDLE_TELEPORT));
            BoarPlugin.LOGGER.info("Player rejected teleport, re-sync teleport....");
        }
    }

    @Override
    public void onPacketSend(final CloudburstPacketEvent event, final boolean immediate) {
        if (!(event.getPacket() instanceof MovePlayerPacket packet)) {
            return;
        }

        final BoarPlayer player = event.getPlayer();
        if (packet.getMode() == MovePlayerPacket.Mode.HEAD_ROTATION) {
            return;
        }

        if (player.runtimeEntityId != packet.getRuntimeEntityId()) {
            return;
        }

        player.queuedVelocities.clear();
        player.teleportUtil.addTeleportToQueue(null, new Vec3f(packet.getPosition()), immediate);
    }
}