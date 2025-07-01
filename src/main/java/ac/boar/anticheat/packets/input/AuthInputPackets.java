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
import ac.boar.anticheat.util.math.Vec3;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.PacketListener;
import org.cloudburstmc.protocol.bedrock.data.PlayerActionType;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.level.BedrockDimension;

import java.util.Iterator;
import java.util.Map;

public class AuthInputPackets extends TeleportHandler implements PacketListener {
    @Override
    public void onPacketReceived(CloudburstPacketEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final BoarPlayer player = event.getPlayer();
        if (!(event.getPacket() instanceof PlayerAuthInputPacket packet)) {
            return;
        }

        player.sinceLoadingScreen++;

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
            if (packet.getTick() != player.tick || packet.getTick() <= 0) {
                player.kick("Invalid tick id=" + packet.getTick());
                return;
            }
        }
        // -------------------------------------------------------------------------

        player.breakingValidator.handle(packet);
        player.tick();

        LegacyAuthInputPackets.processAuthInput(player, packet, true);
        LegacyAuthInputPackets.updateUnvalidatedPosition(player, packet);

        // System.out.println("Unvalidated position: " + player.unvalidatedPosition);

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

    @Override
    public void onPacketSend(CloudburstPacketEvent event, boolean immediate) {
        final BoarPlayer player = event.getPlayer();

        if (event.getPacket() instanceof ChangeDimensionPacket packet) {
            int dimensionId = packet.getDimension();
            final BedrockDimension dimension = dimensionId == BedrockDimension.OVERWORLD_ID ? BedrockDimension.OVERWORLD
                    : dimensionId == BedrockDimension.BEDROCK_NETHER_ID ? BedrockDimension.THE_NETHER : BedrockDimension.THE_END;

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

                player.tick = Long.MIN_VALUE;
            });
        }

        if (event.getPacket() instanceof RespawnPacket packet && packet.getState() == RespawnPacket.State.SERVER_READY) {
            if (packet.getRuntimeEntityId() != 0) { // Vanilla behaviour according Geyser.
                return;
            }

            player.sendLatencyStack(immediate);
            player.getLatencyUtil().addTaskToQueue(player.sentStackId.get(), () -> {
                player.tick = Long.MIN_VALUE; // I only need this.
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
