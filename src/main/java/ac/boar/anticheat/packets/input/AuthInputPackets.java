package ac.boar.anticheat.packets.input;

import ac.boar.anticheat.check.impl.timer.Timer;
import ac.boar.anticheat.data.input.PredictionData;
import ac.boar.anticheat.data.input.VelocityData;
import ac.boar.anticheat.packets.input.legacy.LegacyAuthInputPackets;
import ac.boar.anticheat.packets.input.teleport.TeleportHandler;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.PredictionRunner;
import ac.boar.anticheat.prediction.engine.data.Vector;
import ac.boar.anticheat.teleport.data.TeleportCache;
import ac.boar.anticheat.util.DimensionUtil;
import ac.boar.anticheat.util.math.Vec3;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.PacketListener;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.level.BedrockDimension;

import java.util.Iterator;
import java.util.Map;

public class AuthInputPackets extends TeleportHandler implements PacketListener {
    @Override
    public void onPacketReceived(final CloudburstPacketEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final BoarPlayer player = event.getPlayer();
        if (!(event.getPacket() instanceof PlayerAuthInputPacket packet)) {
            return;
        }

        player.sinceLoadingScreen++;

        // -------------------------------------------------------------------------
        // Timer check start here.
        final long claimedTick = packet.getTick();
        if (claimedTick < 0 || claimedTick < player.tick) { // Impossible, no way this can happen.
            player.kick("Impossible tick id=" + claimedTick);
            return;
        }

        // This is to prevent player skipping (more) ticks (than they're supposed to) after respawn to fuck up our rewind system.
        long distanceSincePrev = System.currentTimeMillis() - player.sinceAuthInput;
        if (distanceSincePrev < 50L) {
            player.tick++;
        } else {
            player.tick += Math.min(claimedTick - player.tick, distanceSincePrev / 45L);
        }

        player.sinceAuthInput = System.currentTimeMillis();

        if (player.tick != packet.getTick()) {
            player.kick("Invalid tick id, predicted=" + player.tick + ", actual=" + packet.getTick());
            return;
        }

        final Timer timer = (Timer) player.getCheckHolder().get(Timer.class);
        if (timer != null && timer.isInvalid()) {
            event.setCancelled(true);
            return;
        }

        // Timer check end here.
        // -------------------------------------------------------------------------

        player.breakingValidator.handle(packet);
        player.tick();

        LegacyAuthInputPackets.processAuthInput(player, packet, true);
        LegacyAuthInputPackets.updateUnvalidatedPosition(player, packet);

        // System.out.println("Unvalidated position: " + player.unvalidatedPosition);

        if (player.vehicleData != null) { // TODO: Vehicle prediction.
            player.position = player.unvalidatedPosition;
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

    @Override
    public void onPacketSend(CloudburstPacketEvent event, boolean immediate) {
        final BoarPlayer player = event.getPlayer();

        if (event.getPacket() instanceof ChangeDimensionPacket packet) {
            int dimensionId = packet.getDimension();
            final BedrockDimension dimension = DimensionUtil.dimensionFromId(dimensionId);

            player.sendLatencyStack(immediate);
            player.getTeleportUtil().getQueuedTeleports().add(new TeleportCache.DimensionSwitch(player.sentStackId.get(), new Vec3(packet.getPosition().up(EntityDefinitions.PLAYER.offset()))));
            player.getLatencyUtil().addTaskToQueue(player.sentStackId.get(), () -> {
                player.compensatedWorld.getChunks().clear();
                player.compensatedWorld.setDimension(dimension);

                player.currentLoadingScreen = packet.getLoadingScreenId();
                player.inLoadingScreen = true;

                player.getFlagTracker().clear();
                player.getFlagTracker().flying(false);

                // We shouldn't do this, if we still are handling things correctly, we wouldn't have to clear teleport.
                // player.getTeleportUtil().getQueuedTeleports().clear();
            });
        }

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
