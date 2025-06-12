package ac.boar.anticheat.prediction;

import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.MathUtil;
import ac.boar.anticheat.util.math.Vec3;
import lombok.RequiredArgsConstructor;

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
        if (player.affectedByFluidPushing) {
            Vec3 newPosition = player.prevUnvalidatedPosition.add(player.afterCollision.add(player.pushingVelocity));
            Vec3 predicted2 = newPosition.subtract(player.prevUnvalidatedPosition);
            Vec3 pushingMax = player.guessedFluidPushingVelocity;
            float yOffset = Math.abs(player.position.y - player.unvalidatedPosition.y);

            if (MathUtil.sameDirection(actual, predicted2)) {
                if ((actual.y * actual.y) - (predicted.y * predicted.y) - (pushingMax.y * pushingMax.y) <= player.getMaxOffset()) {
                    extra += yOffset;
                }

                if (actual.horizontalLength() - predicted.horizontalLength() - pushingMax.horizontalLength() - 0.0145F <= player.getMaxOffset() && yOffset - extra <= player.getMaxOffset()) {
                    extra = (float) offset;
                }
            }
        }

        if (Math.abs(player.position.y - player.unvalidatedPosition.y) - extra <= player.getMaxOffset() && player.soulSandBelow && actual.horizontalLengthSquared() < player.afterCollision.horizontalLengthSquared() && MathUtil.sameDirection(actual, predicted)) {
            extra = (float) offset;
        }

        return extra;
    }
}
