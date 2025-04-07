package ac.boar.anticheat.check.api.holder;

import ac.boar.anticheat.check.api.Check;
import ac.boar.anticheat.check.impl.prediction.InputA;
import ac.boar.anticheat.check.impl.prediction.PredictionA;
import ac.boar.anticheat.check.impl.timer.Timer;
import ac.boar.anticheat.check.impl.velocity.Velocity;
import ac.boar.anticheat.player.BoarPlayer;

import java.util.HashMap;

public class CheckHolder extends HashMap<Class<?>, Check> {
    public CheckHolder(final BoarPlayer player) {
        this.put(Timer.class, new Timer(player));

        this.put(Velocity.class, new Velocity(player));
        // this.put(DebugOffsetA.class, new DebugOffsetA(player));
        this.put(PredictionA.class, new PredictionA(player));
        this.put(InputA.class, new InputA(player));
    }
}
