package ac.boar.anticheat.check.impl.prediction;

import ac.boar.anticheat.check.api.annotations.CheckInfo;
import ac.boar.anticheat.check.api.impl.OffsetHandlerCheck;
import ac.boar.anticheat.check.impl.velocity.Velocity;
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

        if (((Velocity)player.checkHolder.get(Velocity.class)).check(offset)) {
            return;
        }

        if (offset > player.getMaxOffset()) {
            if (player.sinceTeleport > 20) {
                this.fail("o=" + offset);
            }

            if (player.teleportUtil.teleportInQueue()) {
                return;
            }
            player.teleportUtil.rewind(player.tick);
        }
    }
}
