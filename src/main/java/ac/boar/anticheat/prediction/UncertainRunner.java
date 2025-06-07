package ac.boar.anticheat.prediction;

import ac.boar.anticheat.player.BoarPlayer;
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
        if (player.affectedByFluidPushing && actual.horizontalLength() - player.afterCollision.horizontalLength() <=
                player.guessedFluidPushingVelocity.horizontalLength() + 0.014F &&
                Math.abs(player.position.y - player.unvalidatedPosition.y) <= player.getMaxOffset()) {
            extra = (float) offset;
        }

        return extra;
    }
}
