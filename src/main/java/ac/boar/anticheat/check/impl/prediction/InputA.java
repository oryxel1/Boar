package ac.boar.anticheat.check.impl.prediction;

import ac.boar.anticheat.check.api.annotations.CheckInfo;
import ac.boar.anticheat.check.api.impl.OffsetHandlerCheck;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.math.Vec3;
import ac.boar.anticheat.util.MathUtil;

@CheckInfo(name = "Input", type = "A")
public final class InputA extends OffsetHandlerCheck {
    public InputA(final BoarPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(double offset) {
        if (offset > player.getMaxOffset() || player.teleportUtil.teleportInQueue()) {
            return;
        }

        Vec3 predictedInput = new Vec3(MathUtil.sign(player.input.x), 0, MathUtil.sign(player.input.z));

        if (!player.lastTickWasJoystick && predictedInput.x != 0 && predictedInput.z != 0) {
            predictedInput = predictedInput.multiply(0.70710677F);
        }

        if (player.sneaking && !player.gliding && !player.isInLava() && !player.touchingWater) {
            predictedInput = predictedInput.multiply(0.3F);
        }

        if (Math.abs(predictedInput.getX()) < Math.abs(player.input.x) || Math.abs(predictedInput.getZ()) < Math.abs(player.input.z)) {
            fail();

            // This tick prediction isn't reliable, revert to the previous tick...
            player.teleportUtil.rewind(player.tick - 1);
        }
    }
}
