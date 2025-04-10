package ac.boar.anticheat.check.impl.prediction;

import ac.boar.anticheat.check.api.annotations.CheckInfo;
import ac.boar.anticheat.check.api.impl.OffsetHandlerCheck;
import ac.boar.anticheat.check.impl.velocity.Velocity;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.engine.data.VectorType;

@CheckInfo(name = "Prediction", type = "A")
public class PredictionA extends OffsetHandlerCheck {
    public PredictionA(BoarPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(double offset) {
        if (System.currentTimeMillis() - player.joinedTime < 2000L) {
            return;
        }

        if (offset > player.getMaxOffset()) {
            if (player.sinceTeleport > 20) {
                if (player.bestPossibility.getType() == VectorType.VELOCITY) {
                    player.getCheckHolder().get(Velocity.class).fail("o=" + offset);
                } else {
                    this.fail("o=" + offset);
                }
            }

            if (player.getTeleportUtil().isTeleporting()) {
                return;
            }
            System.out.println("Rewinding!");
            player.getTeleportUtil().rewind(player.tick);
        }
    }
}
