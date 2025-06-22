package ac.boar.anticheat.packets.input;

import ac.boar.anticheat.check.impl.timer.Timer;
import ac.boar.anticheat.data.input.PredictionData;
import ac.boar.anticheat.data.input.TickData;
import ac.boar.anticheat.data.input.VelocityData;
import ac.boar.anticheat.packets.input.legacy.LegacyAuthInputPackets;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.PredictionRunner;
import ac.boar.anticheat.prediction.engine.data.Vector;
import ac.boar.anticheat.teleport.data.TeleportCache;
import ac.boar.anticheat.util.math.Vec3;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.PacketListener;
import org.cloudburstmc.protocol.bedrock.data.PlayerActionType;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerActionPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;

import java.util.Iterator;
import java.util.Map;
import java.util.Queue;

public class AuthInputPackets implements PacketListener {
    @Override
    public void onPacketReceived(CloudburstPacketEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final BoarPlayer player = event.getPlayer();
        if (event.getPacket() instanceof PlayerActionPacket packet) {
            if (packet.getRuntimeEntityId() != player.runtimeEntityId) {
                return;
            }

            if (packet.getAction() == PlayerActionType.DIMENSION_CHANGE_SUCCESS) {
            }
        }

        if (!(event.getPacket() instanceof PlayerAuthInputPacket packet)) {
            return;
        }

        // TODO: ugggggh, current timer logic is a bit broken.
        // -------------------------------------------------------------------------
        Timer timer = (Timer) player.getCheckHolder().get(Timer.class);
        if (player.tick == Long.MIN_VALUE) {
            if (timer == null) {
                player.tick = Math.max(0, packet.getTick()) - 1;
            }
        }
        if (timer != null) {
            if (timer.tick(packet.getTick())) {
                return;
            }
        } else {
            player.tick++;
            if (packet.getTick() != player.tick || packet.getTick() < 0) {
                player.kick("Invalid tick id=" + packet.getTick());
                return;
            }
        }
        // -------------------------------------------------------------------------

        player.breakingValidator.handle(packet);
        player.tick();

        LegacyAuthInputPackets.processAuthInput(player, packet, true);
        LegacyAuthInputPackets.updateUnvalidatedPosition(player, packet);

        if (player.vehicleData != null) { // TODO: Vehicle prediction.
            return;
        }

        if (player.isMovementExempted()) {
            player.setPos(player.unvalidatedPosition);

            // Clear velocity out manually since we haven't handled em.
            Iterator<Map.Entry<Long, VelocityData>> iterator = player.queuedVelocities.entrySet().iterator();

            Map.Entry<Long, VelocityData> entry;
            while (iterator.hasNext() && (entry = iterator.next()) != null) {
                if (entry.getKey() >= player.receivedStackId.get()) {
                    break;
                } else {
                    iterator.remove();
                }
            }

            // This is fine, we only need tick end and use before and after to calculate ground.
            player.predictionResult = new PredictionData(Vec3.ZERO, player.velocity.y < 0 && player.getInputData().contains(PlayerAuthInputData.VERTICAL_COLLISION) ? new Vec3(0, 1, 0) : Vec3.ZERO, player.unvalidatedTickEnd);
            player.velocity = player.unvalidatedTickEnd.clone();

            player.bestPossibility = Vector.NONE;
        } else {
            new PredictionRunner(player).run();
        }
        this.processQueuedTeleports(player, packet);
        LegacyAuthInputPackets.doPostPrediction(player, packet);
    }

    private void processQueuedTeleports(final BoarPlayer player, final PlayerAuthInputPacket packet) {
        final Queue<TeleportCache> queuedTeleports = player.getTeleportUtil().getQueuedTeleports();

        if (queuedTeleports.isEmpty()) {
            return;
        }

        TeleportCache cache;
        while ((cache = queuedTeleports.peek()) != null) {
            if (player.receivedStackId.get() < cache.getStackId()) {
                break;
            }

            queuedTeleports.poll();

            // Bedrock don't reply to teleport individually using a separate tick packet instead it just simply set its position to
            // the teleported position and then let us know the *next tick*, so we do the same!
            if (cache instanceof TeleportCache.Normal normal) {
                this.processTeleport(player, normal, packet);
            } else if (cache instanceof TeleportCache.DimensionSwitch dimension) {

            } else if (cache instanceof TeleportCache.Rewind rewind) {
                this.processRewind(player, rewind, packet);
            } else {
                throw new RuntimeException("Failed to process queued teleports, invalid teleport=" + cache);
            }
        }
    }

    private void processDimensionSwitch(final BoarPlayer player, final TeleportCache.DimensionSwitch dimension, final PlayerAuthInputPacket packet) {

    }

    private void processTeleport(final BoarPlayer player, final TeleportCache.Normal normal, final PlayerAuthInputPacket packet) {
        double distance = packet.getPosition().distance(normal.getPosition().toVector3f());
        // I think I'm being a bit lenient but on Bedrock the position error seems to be a bit high.
        if ((packet.getInputData().contains(PlayerAuthInputData.HANDLE_TELEPORT)) && distance <= 1.0E-3F) {
            player.setPos(new Vec3(packet.getPosition().sub(0, player.getYOffset(), 0)));
            player.unvalidatedPosition = player.prevUnvalidatedPosition = player.position.clone();

            player.velocity = Vec3.ZERO.clone();
            player.predictionResult = new PredictionData(Vec3.ZERO, Vec3.ZERO, Vec3.ZERO); // Yep!

            // This value can be true but since Geyser always send false then it is always false.
            player.onGround = false;
        } else {
            // Player rejected teleport OR this is not the latest teleport.
            if (!player.getTeleportUtil().isTeleporting()) {
                player.getTeleportUtil().teleportTo(normal);
            }
        }
    }

    // Wouldn't it be nice to provide us a way to know when player accept rewind mojang :(
    // There will be edge cases where player lag right after accepting the stack id responsible for rewind... maaaaaaaybe
    // we could check for current offset and if it's close then player might possibility HAVEN'T received the rewind yet?
    // Just ignore the edge cases for now.
    private void processRewind(final BoarPlayer player, final TeleportCache.Rewind rewind, final PlayerAuthInputPacket packet) {
        if (player.isMovementExempted()) { // Fully exempted from rewind teleport.
            return;
        }

        player.onGround = rewind.isOnGround();
        player.velocity = rewind.getTickEnd();
        player.setPos(rewind.getPosition().subtract(0, player.getYOffset(), 0));
        player.prevUnvalidatedPosition = player.unvalidatedPosition = player.position.clone();

        player.getTeleportUtil().cachePosition(rewind.getTick(), rewind.getPosition().toVector3f());

        // Keep running prediction until we catch up with the player current tick.
        long currentTick = rewind.getTick();
        while (currentTick != player.tick) {
            if (currentTick != rewind.getTick() && player.position.distanceTo(player.unvalidatedPosition) > player.getMaxOffset()) {
                player.unvalidatedPosition = player.position.clone();
            }

            currentTick++;

            if (currentTick == player.tick) {
                LegacyAuthInputPackets.processAuthInput(player, packet, true);
                LegacyAuthInputPackets.updateUnvalidatedPosition(player, packet);
            } else if (player.getTeleportUtil().getAuthInputHistory().containsKey(currentTick)) {
                final TickData data = player.getTeleportUtil().getAuthInputHistory().get(currentTick);
                LegacyAuthInputPackets.processAuthInput(player, data.packet(), false);
                LegacyAuthInputPackets.updateUnvalidatedPosition(player, packet);

                // Reverted back to the old flags.
                player.getFlagTracker().set(data.flags(), false);
            } else {
                throw new RuntimeException("Failed find auth input history for rewind.");
            }

            new PredictionRunner(player).run();
            // player.getTeleportUtil().cachePosition(currentTick, player.position.add(0, player.getYOffset(), 0).toVector3f());
        }
    }

    @Override
    public void onPacketSend(CloudburstPacketEvent event, boolean immediate) {
        final BoarPlayer player = event.getPlayer();
        if (event.getPacket() instanceof MovePlayerPacket packet) {
            if (packet.getMode() == MovePlayerPacket.Mode.HEAD_ROTATION) {
                // TODO: Actually... does this affect player motion?
                return;
            }

            if (player.runtimeEntityId != packet.getRuntimeEntityId()) {
                return;
            }

            // I think... there is some interpolation or some kind of smoothing when we use NORMAL?
            // Well it's a pain in the ass the support it, so just send teleport....
            if (packet.getMode() == MovePlayerPacket.Mode.NORMAL) {
                packet.setMode(MovePlayerPacket.Mode.TELEPORT);
            }

            player.getTeleportUtil().queueTeleport(new Vec3(packet.getPosition()), immediate);
        }
    }
}
