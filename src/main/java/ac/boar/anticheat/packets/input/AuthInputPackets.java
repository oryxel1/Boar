package ac.boar.anticheat.packets.input;

import ac.boar.anticheat.data.input.PredictionData;
import ac.boar.anticheat.data.input.VelocityData;
import ac.boar.anticheat.packets.input.legacy.LegacyAuthInputPackets;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.PredictionRunner;
import ac.boar.anticheat.prediction.engine.data.Vector;
import ac.boar.anticheat.prediction.engine.data.VectorType;
import ac.boar.anticheat.teleport.data.TeleportCache;
import ac.boar.anticheat.util.math.Vec3;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.PacketListener;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket;
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
        if (!(event.getPacket() instanceof PlayerAuthInputPacket packet)) {
            return;
        }

        player.breakingValidator.handle(packet);
        player.tick();

        LegacyAuthInputPackets.processAuthInput(player, packet, true);
        LegacyAuthInputPackets.updateUnvalidatedPosition(player, packet);

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
        } else {
            new PredictionRunner(player).run();
        }
        this.processQueuedTeleports(player, packet);

        player.getTeleportUtil().cachePosition(player.tick, player.position.add(0, player.getYOffset(), 0).toVector3f());
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

            boolean poll;

            // Bedrock don't reply to teleport individually using a separate tick packet instead it just simply set its position to
            // the teleported position and then let us know the *next tick*, so we do the same!
            if (cache instanceof TeleportCache.Normal normal) {
                poll = this.processTeleport(player, normal, packet);
            } else if (cache instanceof TeleportCache.Rewind rewind) {
                this.processRewind(player, rewind, packet);
                poll = true;
            } else {
                throw new RuntimeException("Failed to process queued teleports, invalid teleport=" + cache);
            }

            if (poll) {
                queuedTeleports.poll();
            } else {
                // We stop processing since player likely haven't received the reset and 2 teleport won't hang on the same stack id.
                break;
            }
        }
    }

    private boolean processTeleport(final BoarPlayer player, final TeleportCache.Normal normal, final PlayerAuthInputPacket packet) {
        double distance = packet.getPosition().distance(normal.getPosition().toVector3f());
        // I think I'm being a bit lenient but on Bedrock the position error seems to be a bit high.
        if ((packet.getInputData().contains(PlayerAuthInputData.HANDLE_TELEPORT) || normal.isSilent()) && distance <= 1.0E-3F) {
            player.setPos(new Vec3(packet.getPosition().sub(0, player.getYOffset(), 0)));
            player.unvalidatedPosition = player.prevUnvalidatedPosition = player.position.clone();

            player.velocity = Vec3.ZERO.clone();
            player.predictionResult = new PredictionData(Vec3.ZERO, Vec3.ZERO, Vec3.ZERO); // Yep!

            // This value can be true but since Geyser always send false then it is always false.
            player.onGround = false;
        } else {
            boolean isThisTheLastTeleport = player.getTeleportUtil().getQueuedTeleports().size() == 1;
            // Player rejected teleport OR this is not the latest teleport.
            if (!isThisTheLastTeleport) {
                return true;
            }

            // If this is a packet that does re position player but with no way to confirm it (PlayerAuthInputData.HANDLE_TELEPORT for ex)
            // Then if player "reject" this teleport, we will have to wait till player receive the second stack latency
            // to see if player actually reject this or not.
            if (normal.isSilent() && player.receivedStackId.get() < normal.getStackId() + 1) {
                return false;
            }

            player.getTeleportUtil().teleportTo(normal);
        }

        return true;
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

            player.getTeleportUtil().queueTeleport(new Vec3(packet.getPosition()), immediate, packet.getMode() != MovePlayerPacket.Mode.TELEPORT);
        }
    }
}
