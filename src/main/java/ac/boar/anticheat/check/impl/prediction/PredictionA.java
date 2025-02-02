package ac.boar.anticheat.check.impl.prediction;

import ac.boar.anticheat.check.api.CheckInfo;
import ac.boar.anticheat.check.api.impl.OffsetHandlerCheck;
import ac.boar.anticheat.player.BoarPlayer;

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
            this.fail("o=" + offset);
            //player.teleportUtil.setBackWithSimulation();
        }
    }
}
