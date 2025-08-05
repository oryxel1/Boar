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

    public float extraOffsetNonTickEnd(float offset) {
        float extra = 0;

        Vec3 actual = player.unvalidatedPosition.subtract(player.prevUnvalidatedPosition);
        Vec3 predicted = player.position.subtract(player.prevUnvalidatedPosition);
        boolean validYOffset = Math.abs(player.position.y - player.unvalidatedPosition.y) - extra <= player.getMaxOffset();
        boolean actualSpeedSmallerThanPredicted = actual.horizontalLengthSquared() < predicted.horizontalLengthSquared();
        boolean sameDirection = MathUtil.sameDirection(actual, predicted);
        boolean sameDirectionOrZero = (MathUtil.sign(actual.x) == MathUtil.sign(predicted.x) || actual.x == 0)
                && MathUtil.sign(actual.y) == MathUtil.sign(predicted.y) && (MathUtil.sign(actual.z) == MathUtil.sign(predicted.z) || actual.z == 0);
        if (validYOffset && (sameDirection || sameDirectionOrZero) && actualSpeedSmallerThanPredicted && player.nearBamboo && player.horizontalCollision) {
            extra = offset;
        }

        return extra;
    }

    public float extraOffset(float offset) {
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
            extra = offset;
        }

        if (player.beingPushByLava && validYOffset) {
            extra += 0.004F;

            if (sameDirection) {
                if (player.input.horizontalLengthSquared() > 0) {
                    Vec3 subtractedSpeed = actual.subtract(MathUtil.sign(player.afterCollision.x) * 0.02F, 0, MathUtil.sign(player.afterCollision.z) * 0.02F);

                    if (subtractedSpeed.horizontalLengthSquared() < predicted.horizontalLengthSquared()) {
                        extra = offset;
                    }
                } else {
                    if (actual.horizontalLengthSquared() < predicted.horizontalLengthSquared()) {
                        extra = offset;
                    }
                }
            }
        }

        // .... This is weird, no idea why.
        if (player.hasDepthStrider) {
            if (actualSpeedSmallerThanPredicted && validYOffset) {
                extra = offset;
            }
        }

        if (player.getFlagTracker().has(EntityFlag.GLIDING)) {
            extra += 1.0E-4F; // gliding accuracy is... yuck.

            if (offset <= 8.0E-4 && player.glideBoostTicks >= 0) {
                extra = offset;
            }
        }

        return extra;
    }
}
