package ac.boar.anticheat.check.api.holder;

import ac.boar.anticheat.check.api.Check;
import ac.boar.anticheat.check.impl.combat.DirectionA;
import ac.boar.anticheat.check.impl.prediction.DebugOffsetA;
import ac.boar.anticheat.check.impl.prediction.PredictionA;
import ac.boar.anticheat.check.impl.timer.TimerA;
import ac.boar.anticheat.check.impl.velocity.VelocityA;
import ac.boar.anticheat.player.BoarPlayer;

import java.util.HashMap;

public class CheckHolder extends HashMap<Class<?>, Check> {
    public CheckHolder(final BoarPlayer player) {
        this.put(TimerA.class, new TimerA(player));

        this.put(VelocityA.class, new VelocityA(player));
        this.put(DebugOffsetA.class, new DebugOffsetA(player));
        this.put(PredictionA.class, new PredictionA(player));

        this.put(DirectionA.class, new DirectionA(player));
    }
}
