package ac.boar.anticheat.check.impl.velocity;

import ac.boar.anticheat.check.api.Check;
import ac.boar.anticheat.check.api.annotations.CheckInfo;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.engine.data.VectorType;

@CheckInfo(name = "Velocity", type = "*")
public class Velocity extends Check {
    public Velocity(BoarPlayer player) {
        super(player);
    }
}
