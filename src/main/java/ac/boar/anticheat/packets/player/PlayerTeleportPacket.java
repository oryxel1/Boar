package ac.boar.anticheat.packets.player;

import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.TeleportUtil;
import ac.boar.anticheat.util.math.Vec3f;
import ac.boar.plugin.BoarPlugin;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.CloudburstPacketListener;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;

import java.util.Queue;

public class PlayerTeleportPacket implements CloudburstPacketListener {
    private static final float MAX_TOLERANCE_ERROR = 0.001f;

    @Override
    public void onPacketReceived(final CloudburstPacketEvent event) {
        final BoarPlayer player = event.getPlayer();
        if (!(event.getPacket() instanceof PlayerAuthInputPacket packet)) {
            return;
        }

        Queue<TeleportUtil.TeleportCache> queue = player.teleportUtil.getTeleportQueue();
        if (queue.isEmpty()) {
            return;
        }

        // We can't check for player.lastReceivedId == cache.getTransactionId() since bedrock teleport is different.
        // Instead of respond with an extra move packet like java, it just simply set position
        // and add HANDLE_TELEPORT to input data next tick to let us know that it accepted the teleport.
        // Which also means player will be in the position of the latest teleport they got and accept that one, not every teleport like Java.
        TeleportUtil.TeleportCache temp;
        TeleportUtil.TeleportCache cache = null;
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
        if ((packet.getInputData().contains(PlayerAuthInputData.HANDLE_TELEPORT)) && distance < MAX_TOLERANCE_ERROR) {
            BoarPlugin.LOGGER.info("Accepted teleport, d=" + distance);
            player.lastTickWasTeleport = true;
        } else {
            // This is not the latest teleport, just ignore this one, we only force player to accept the latest one.
            // We don't want to teleport player to old teleport position when they're supposed to teleport to the latest tone.
            if (!queue.isEmpty()) {
                return;
            }
            // Set player back to where they're supposed to be.
            player.teleportUtil.setbackTo(cache.getPosition());
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
        player.teleportUtil.addTeleportToQueue(new Vec3f(packet.getPosition()), immediate);
    }
}