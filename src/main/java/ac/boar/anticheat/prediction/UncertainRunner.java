package ac.boar.anticheat.prediction;

import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.MathUtil;
import ac.boar.anticheat.util.math.Vec3;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;

// Things that I don't even bother account for...
@RequiredArgsConstructor
public class UncertainRunner {
    private final BoarPlayer player;

    public void doTickEndUncertain() {
    }

    public float extraOffset(double offset) {
        float extra = 0;
        if (player.thisTickSpinAttack) {
            extra += player.thisTickOnGroundSpinAttack ? 0.08F : 0.008F;
        }

        Vec3 actual = player.unvalidatedPosition.subtract(player.prevUnvalidatedPosition);
        Vec3 predicted = player.position.subtract(player.prevUnvalidatedPosition);

        boolean validYOffset = Math.abs(player.position.y - player.unvalidatedPosition.y) - extra <= player.getMaxOffset();
        boolean sameDirection = MathUtil.sameDirection(actual, predicted);
        boolean actualSpeedSmallerThanPredicted = actual.horizontalLengthSquared() < predicted.horizontalLengthSquared();

        if (player.soulSandBelow && validYOffset && actualSpeedSmallerThanPredicted && sameDirection) {
            extra = (float) offset;
        }

        if (player.beingPushByLava && validYOffset) {
            extra += 0.004F;

            if (sameDirection) {
                if (player.input.horizontalLengthSquared() > 0) {
                    Vec3 subtractedSpeed = actual.subtract(MathUtil.sign(player.afterCollision.x) * 0.02F, 0, MathUtil.sign(player.afterCollision.z) * 0.02F);

                    if (subtractedSpeed.horizontalLengthSquared() < predicted.horizontalLengthSquared()) {
                        extra = (float) offset;
                    }
                } else {
                    if (actual.horizontalLengthSquared() < predicted.horizontalLengthSquared()) {
                        extra = (float) offset;
                    }
                }
            }
        }

        // .... This is weird, no idea why.
        if (!player.getFlagTracker().has(EntityFlag.SWIMMING) && player.hasDepthStrider) {
            if (actualSpeedSmallerThanPredicted && validYOffset) {
                extra = (float) offset;
            }
        }

        if (offset <= 8.0E-4 && player.glideBoostTicks >= 0 && player.getFlagTracker().has(EntityFlag.GLIDING)) {
            extra = (float) offset;
        }

        return extra;
    }
}
