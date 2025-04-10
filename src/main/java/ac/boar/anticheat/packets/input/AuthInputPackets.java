package ac.boar.anticheat.packets.input;

import ac.boar.anticheat.data.VelocityData;
import ac.boar.anticheat.packets.input.legacy.LegacyAuthInputPackets;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.PredictionRunner;
import ac.boar.anticheat.teleport.data.TeleportCache;
import ac.boar.anticheat.util.math.Vec3;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.PacketListener;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.geysermc.geyser.entity.EntityDefinitions;

import java.util.Iterator;
import java.util.Map;
import java.util.Queue;

public class AuthInputPackets implements PacketListener {
    @Override
    public void onPacketReceived(final CloudburstPacketEvent event) {
        final BoarPlayer player = event.getPlayer();
        if (!(event.getPacket() instanceof PlayerAuthInputPacket packet)) {
            return;
        }

        if (player.tick == Long.MIN_VALUE) {
            player.tick = Math.max(0, packet.getTick()) - 1;
        }
        player.tick++;
        if (packet.getTick() != player.tick || packet.getTick() < 0) {
            player.kick("Invalid tick id=" + packet.getTick());
            return;
        }

        player.sinceTeleport++;
        player.sinceLoadingScreen++;

        player.breakingValidator.handle(packet);
        player.tick();

        player.prevVelocity = player.velocity.clone();

        // Priority teleport.
        boolean wasTeleport = this.processQueuedTeleports(player, packet);

        if (player.inLoadingScreen || player.sinceLoadingScreen < 2) {
            final double offset = player.unvalidatedPosition.distanceTo(player.prevUnvalidatedPosition);
            if (offset > 1.0E-7) {
                player.getTeleportUtil().teleportTo(player.getTeleportUtil().getLastKnowValid());
            }
            return;
        }

        if (player.isAbilityExempted()) {
            // TODO: Flying prediction?
            LegacyAuthInputPackets.processAuthInput(player, packet);
            player.velocity = player.unvalidatedTickEnd.clone();
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

            return;
        }

        if (player.getTeleportUtil().isTeleporting()) {
            return;
        }

        player.prevUnvalidatedPosition = player.unvalidatedPosition.clone();
        player.unvalidatedPosition = new Vec3(packet.getPosition().sub(0, EntityDefinitions.PLAYER.offset(), 0));

        player.unvalidatedTickEnd = new Vec3(packet.getDelta());

        LegacyAuthInputPackets.processAuthInput(player, packet);

        // Yes, I'm doing this twice to check for elytra-desync rewind, also don't do this if teleport already handle it.
        if (!player.getTeleportUtil().isTeleporting() && !wasTeleport) {
            new PredictionRunner(player).run();
        }

        LegacyAuthInputPackets.doPostPrediction(player, packet);
    }

    private boolean processQueuedTeleports(final BoarPlayer player, final PlayerAuthInputPacket packet) {
        final Queue<TeleportCache> queuedTeleports = player.getTeleportUtil().getQueuedTeleports();
        if (queuedTeleports.isEmpty()) {
            return false;
        }

        boolean teleport = false;
        TeleportCache cache;
        while ((cache = queuedTeleports.peek()) != null) {
            if (player.receivedStackId.get() < cache.getStackId()) {
                break;
            }

            teleport = true;
            queuedTeleports.poll();

            // Bedrock don't reply to teleport individually using a seperate tick packet instead it just simply set its position to
            // the teleported position and then let us know the *next tick*, so we do the same!
            if (cache instanceof TeleportCache.Normal normal) {
                this.processTeleport(player, normal, packet);
            } else if (cache instanceof TeleportCache.Rewind rewind) {
                this.processRewind(player, rewind, packet);
            } else {
                throw new RuntimeException("Failed to process queued teleports, invalid teleport=" + cache);
            }
        }

        return teleport;
    }

    private void processTeleport(final BoarPlayer player, final TeleportCache.Normal normal, final PlayerAuthInputPacket packet) {
        double distance = packet.getPosition().distance(normal.getPosition().toVector3f());
        // I think I'm being a bit lenient but on Bedrock the position error seems to be a bit high.
        if (packet.getInputData().contains(PlayerAuthInputData.HANDLE_TELEPORT) && distance <= 1.0E-3F) {
            player.setPos(new Vec3(packet.getPosition().sub(0, EntityDefinitions.PLAYER.offset(), 0)));
            player.unvalidatedPosition = player.prevUnvalidatedPosition = player.position.clone();

            if (!normal.isKeepVelocity()) {
                player.velocity = Vec3.ZERO.clone();
            }

            player.sinceTeleport = 0;
        } else {
            // Player rejected teleport OR this is not the latest teleport.
            if (!player.getTeleportUtil().isTeleporting()) {
                player.getTeleportUtil().teleportTo(normal);
            }
        }
    }

    private void processRewind(final BoarPlayer player, final TeleportCache.Rewind rewind, final PlayerAuthInputPacket packet) {
        if (player.isAbilityExempted()) { // Fully exempted from rewind teleport.
            return;
        }

        long tickDistance = player.tick - rewind.getTick();

        player.onGround = rewind.isOnGround();
        player.velocity = rewind.getTickEnd();
        player.setPos(rewind.getPosition().subtract(0, EntityDefinitions.PLAYER.offset(), 0));
        player.prevUnvalidatedPosition = player.unvalidatedPosition = player.position.clone();

        player.getTeleportUtil().cachePosition(rewind.getTick(), rewind.getPosition().toVector3f(), false);

        long currentTick = rewind.getTick();
        for (int i = 0; i < tickDistance; i++) {
            currentTick++;
            if (currentTick == player.tick) {
                LegacyAuthInputPackets.processAuthInput(player, packet);
                player.unvalidatedPosition = new Vec3(packet.getPosition().sub(0, EntityDefinitions.PLAYER.offset(), 0));
                player.unvalidatedTickEnd = new Vec3(packet.getDelta());
            } else if (player.getTeleportUtil().getAuthInputHistory().containsKey(currentTick)) {
                LegacyAuthInputPackets.processAuthInput(player, player.getTeleportUtil().getAuthInputHistory().get(currentTick));
            } else {
                throw new RuntimeException("Failed find auth input history for rewind.");
            }

            new PredictionRunner(player).run();

            player.getTeleportUtil().cachePosition(currentTick, player.position.add(0, EntityDefinitions.PLAYER.offset(), 0).toVector3f(), false);
            player.prevUnvalidatedPosition = player.unvalidatedPosition = player.position.clone();
        }

        player.unvalidatedPosition = player.position.clone();
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
        player.getTeleportUtil().queueTeleport(new Vec3(packet.getPosition()), immediate, packet.getMode());
    }
}