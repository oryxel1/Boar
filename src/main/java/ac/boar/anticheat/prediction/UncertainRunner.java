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

        if (Math.abs(player.position.y - player.unvalidatedPosition.y) - extra <= player.getMaxOffset() && player.soulSandBelow && actual.horizontalLengthSquared() < player.afterCollision.horizontalLengthSquared() && MathUtil.sameDirection(actual, predicted)) {
            extra = (float) offset;
        }

        // .... This is weird, no idea why.
        if (!player.getFlagTracker().has(EntityFlag.SWIMMING) && player.hasDepthStrider) {
            if (actual.horizontalLengthSquared() < predicted.horizontalLengthSquared() && Math.abs(player.unvalidatedTickEnd.y - player.velocity.y) < player.getMaxOffset()) {
                extra = (float) offset;
            }
        }

        if (offset <= 8.0E-4 && player.glideBoostTicks >= 0 && player.getFlagTracker().has(EntityFlag.GLIDING)) {
            extra = (float) offset;
        }

        return extra;
    }
}
