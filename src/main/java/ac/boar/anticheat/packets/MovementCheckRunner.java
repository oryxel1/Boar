package ac.boar.anticheat.packets;

import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.CloudburstPacketListener;

import ac.boar.util.MathUtil;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.geysermc.geyser.entity.EntityDefinitions;

public class MovementCheckRunner implements CloudburstPacketListener {
    @Override
    public void onPacketReceived(final CloudburstPacketEvent event) {
        final BoarPlayer player = event.getPlayer();
        if (!(event.getPacket() instanceof PlayerAuthInputPacket packet)) {
            return;
        }

        player.tick = packet.getTick();

        player.getInputData().clear();
        player.getInputData().addAll(packet.getInputData());

        player.movementInput = Vector2f.from(MathUtil.sign(packet.getMotion().getX()), MathUtil.sign(packet.getMotion().getY()));
        this.processInputData(player);

        player.prevX = player.x;
        player.prevY = player.y;
        player.prevZ = player.z;
        player.x = packet.getPosition().getX();
        player.y = packet.getPosition().getY() - EntityDefinitions.PLAYER.offset();
        player.z = packet.getPosition().getZ();

        player.prevYaw = player.yaw;
        player.prevPitch = player.pitch;
        player.yaw = packet.getRotation().getY();
        player.pitch = packet.getRotation().getX();
    }

    public void processInputData(final BoarPlayer player) {
        player.wasSprinting = player.sprinting;
        player.wasSneaking = player.sneaking;
        player.wasGliding = player.gliding;

        for (final PlayerAuthInputData input : player.getInputData()) {
            switch (input) {
                // TODO: Prevent player from spoofing gliding.
                case START_GLIDING -> player.gliding = true;
                case STOP_GLIDING -> player.gliding = false;

                // Don't let player do backwards sprinting!
                case START_SPRINTING -> player.sprinting = player.movementInput.getY() > 0;
                case STOP_SPRINTING -> player.sprinting = false;

                case START_SNEAKING -> player.sneaking = true;
                case STOP_SNEAKING -> player.sneaking = false;
            }
        }
    }
}
