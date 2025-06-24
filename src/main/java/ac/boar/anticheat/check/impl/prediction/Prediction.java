package ac.boar.anticheat.check.impl.prediction;

import ac.boar.anticheat.Boar;
import ac.boar.anticheat.check.api.annotations.CheckInfo;
import ac.boar.anticheat.check.api.impl.OffsetHandlerCheck;
import ac.boar.anticheat.check.impl.velocity.Velocity;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.engine.data.VectorType;

@CheckInfo(name = "Prediction")
public class Prediction extends OffsetHandlerCheck {
    public Prediction(BoarPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(double offset) {
        if (player.tick < 10) {
            System.out.println(player.tick);
            return;
        }

        if (offset > player.getMaxOffset()) {
            if (shouldDoFail() && offset >= Boar.getConfig().alertThreshold()) {
                if (player.bestPossibility.getType() == VectorType.VELOCITY) {
                    player.getCheckHolder().manuallyFail(Velocity.class, "o=" + offset);
                } else {
                    this.fail("o=" + offset);
                }
            }

            player.getTeleportUtil().rewind(player.tick);
        }
    }

    public boolean shouldDoFail() {
        return player.tickSinceBlockResync <= 0;
    }
}
