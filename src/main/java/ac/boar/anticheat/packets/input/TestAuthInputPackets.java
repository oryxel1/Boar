package ac.boar.anticheat.packets.input;

import ac.boar.anticheat.check.impl.timer.Timer;
import ac.boar.anticheat.data.input.TickData;
import ac.boar.anticheat.data.input.VelocityData;
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

import java.util.Iterator;
import java.util.Map;
import java.util.Queue;

public class TestAuthInputPackets implements PacketListener {
    @Override
    public void onPacketReceived(final CloudburstPacketEvent event) {
        final BoarPlayer player = event.getPlayer();
        if (!(event.getPacket() instanceof PlayerAuthInputPacket packet)) {
            return;
        }

        if (event.isCancelled()) {
            return;
        }

        LegacyAuthInputPackets.processAuthInput(player, packet, true);

        player.sinceLoadingScreen++;

        player.breakingValidator.handle(packet);
        player.tick();

        if (player.inLoadingScreen || player.sinceLoadingScreen < 2) {
            LegacyAuthInputPackets.updateUnvalidatedPosition(player, packet);
            final double offset = player.unvalidatedPosition.distanceTo(player.prevUnvalidatedPosition);
            if (offset > 1.0E-7) {
                player.getTeleportUtil().teleportTo(player.getTeleportUtil().getLastKnowValid());
            }
            return;
        }



        // This is likely the case, prediction run before teleport.
        LegacyAuthInputPackets.updateUnvalidatedPosition(player, packet);
        new PredictionRunner(player).run();

        player.getTeleportUtil().cachePosition(player.tick, player.position.add(0, player.getYOffset(), 0).toVector3f());


        if (player.isMovementExempted()) {
            LegacyAuthInputPackets.processAuthInput(player, packet, true);
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
    }


    @Override
    public void onPacketSend(final CloudburstPacketEvent event, final boolean immediate) {

    }
}