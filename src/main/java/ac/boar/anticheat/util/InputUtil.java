package ac.boar.anticheat.util;

import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.math.Vec3;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;

public class InputUtil {
    public static void processInput(final BoarPlayer player, final PlayerAuthInputPacket packet) {
        Vec3 input = Vec3.ZERO.clone();

        // We can only check if player is trying to for example, move an item...
        // We can't really check for if openContainer != null since Bedrock send the container close packet late (as of 1.21.8x)
        // so we won't know for sure if inventory is actually open.
        if (player.doingInventoryAction) {
            player.input = input;
            return;
        }

        if (packet.getAnalogMoveVector().lengthSquared() <= 1.0E-7) {
            input.x = MathUtil.sign(packet.getMotion().getX());
            input.z = MathUtil.sign(packet.getMotion().getY());

            // Simplified version of non-analog input normalize.
            if (input.x != 0 && input.z != 0) {
                input = input.multiply(0.70710677F);
            }
        } else {
            input = new Vec3(MathUtil.clamp(packet.getRawMoveVector().getX(), -1, 1), 0, MathUtil.clamp(packet.getRawMoveVector().getY(), -1, 1));

            // Player input should only be normalized if player won't gain any advantage after normalizing input.
            if (input.length() >= 1) {
                input = input.normalize();
            }
        }

        player.input = input;
    }
}
