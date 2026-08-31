package ac.boar.anticheat.packets.input;

import ac.boar.anticheat.Boar;
import ac.boar.anticheat.ack.types.DimensionSwitchAck;
import ac.boar.anticheat.check.impl.reach.Reach;
import ac.boar.anticheat.check.impl.badpackets.BadPacketA;
import ac.boar.anticheat.check.impl.timer.Timer;
import ac.boar.anticheat.packets.input.legacy.LegacyAuthInputPackets;
import ac.boar.anticheat.packets.input.teleport.TeleportHandler;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.PredictionRunner;
import ac.boar.anticheat.teleport.data.TeleportData;
import ac.boar.anticheat.util.Dimension;
import ac.boar.anticheat.util.DimensionUtil;
import ac.boar.anticheat.util.math.Vec3;
import ac.boar.protocol.api.CloudburstPacketEvent;
import ac.boar.protocol.api.PacketListener;
import org.cloudburstmc.protocol.bedrock.packet.ChangeDimensionPacket;
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;

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

        if (claimedTick < 0) { // Impossible, no way this can happen.
            if (player.disableMitigations()) {
                player.getCheckHolder().manuallyFail(BadPacketA.class, "impossible tick=" + claimedTick);
            } else {
                player.kick("Impossible tick id=" + claimedTick);
            }
            return;
        }

        player.tick = claimedTick;
        player.sinceAuthInput = System.currentTimeMillis();

        final Timer timer = (Timer) player.getCheckHolder().get(Timer.class);
        if (timer != null && timer.isInvalid()) {
            if (!player.disableMitigations()) {
                event.setCancelled(true);
                Boar.debug("[movement-debug] cancelled auth-input reason=timer tick=" + player.tick + " packetTick=" + packet.getTick() + " pos=" + packet.getPosition() + " delta=" + packet.getDelta(), Boar.DebugMessage.WARNING);
                return;
            }
        }

        // Timer check end here.
        // -------------------------------------------------------------------------

        if (player.serverBreakBlockValidator != null) {
            player.serverBreakBlockValidator.handle(packet);
        }

        LegacyAuthInputPackets.processAuthInput(player, packet, true);
        LegacyAuthInputPackets.updateUnvalidatedPosition(player, packet);
        player.insideUnloadedChunk = !player.compensatedWorld.isChunkLoadedAt(
                player.unvalidatedPosition.x, player.unvalidatedPosition.z);

        // Start a fresh movement trace for this tick, with a snapshot of the start state.
        player.getMovementTrace().begin();

        final Reach reach = (Reach) player.getCheckHolder().get(Reach.class);
        if (reach != null) { // null when the Reach check is disabled via disabled-checks - don't NPE.
            reach.validatePending();
        }

        player.tick();

        if (player.vehicleData != null) { // TODO: Vehicle prediction.
            player.getMovementTrace().log("path: vehicle, accepted client position");
            player.position = player.unvalidatedPosition;
            player.compensatedWorld.cleanChunksAtPlayerPosition();
            return;
        }

        if (player.getEntity().bedPosition() != null) {
            player.getMovementTrace().log("path: in bed, skipped");
            return;
        }

        if (player.getTeleportUtil().isTeleporting()) {
            player.getMovementTrace().log("path: teleporting, processing queued teleports");
            this.processQueuedTeleports(player, packet);
        } else if (player.insideUnloadedChunk) {
            player.getMovementTrace().log("path: unloaded chunk, velocity zeroed");
            player.velocity = Vec3.ZERO.clone();
        } else {
            if (player.isMovementExempted()
                    || player.inLoadingScreen
                    || player.sinceLoadingScreen < 2
                    || player.tickSinceBlockResync > 0) {
                player.getMovementTrace().log("path: exempted (movementExempt=" + player.isMovementExempted()
                        + " inLoadingScreen=" + player.inLoadingScreen
                        + " sinceLoadingScreen=" + player.sinceLoadingScreen
                        + " blockResync=" + player.tickSinceBlockResync + ")");
                processExempted(player);
            } else {
                player.getMovementTrace().log("path: prediction");
                new PredictionRunner(player).run();
            }
        }

        player.compensatedWorld.cleanChunksAtPlayerPosition();
        player.insideUnloadedChunk = !player.compensatedWorld.isChunkLoadedAt(
                player.unvalidatedPosition.x, player.unvalidatedPosition.z);
        // Don't try to predict player position in an unloaded chunk, it's not worth it and uh won't go well!
        // Just keep teleporting the player back until they loaded in, that way we shouldn't false post teleport... I think!
        // There isn't much room to abuse considering they're not loaded in any way... and the position is validated so
        // the player can't just send a position 100000 blocks out to avoid for eg: velocity.
        // TODO: Test properly uhhhh in some cases, I'm too lazy to care.
        if (player.insideUnloadedChunk && !player.inLoadingScreen && !player.disableMitigations()) {
            player.getTeleportUtil().teleport(player.getTeleportUtil().getLastKnownValid());
        }

        LegacyAuthInputPackets.doPostPrediction(player, packet);
    }

    @Override
    public void onPacketSend(CloudburstPacketEvent event) {
        final BoarPlayer player = event.getPlayer();

        if (event.getPacket() instanceof ChangeDimensionPacket packet) {
            int dimensionId = packet.getDimension();
            final Dimension dimension = DimensionUtil.dimensionFromId(dimensionId);

            player.pendingDimensionSwitches++;
            player.queueAcknowledgment(new DimensionSwitchAck(dimension, packet.getLoadingScreenId()));
        }

        if (event.getPacket() instanceof MovePlayerPacket packet
                && packet.getRuntimeEntityId() == player.runtimeEntityId
                && packet.getMode() != MovePlayerPacket.Mode.HEAD_ROTATION) {
            // Convert unsupported smoothed and respawn movement modes to teleports.
            if (packet.getMode() == MovePlayerPacket.Mode.NORMAL || packet.getMode() == MovePlayerPacket.Mode.RESPAWN) {
                packet.setMode(MovePlayerPacket.Mode.TELEPORT);
            }

            packet.setOnGround(true);
            player.getTeleportUtil().queue(new TeleportData(new Vec3(packet.getPosition()), packet.isOnGround()));
        }
    }
}
