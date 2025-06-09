package ac.boar.anticheat.check.api.holder;

import ac.boar.anticheat.Boar;
import ac.boar.anticheat.check.api.Check;
import ac.boar.anticheat.check.api.annotations.CheckInfo;
import ac.boar.anticheat.check.impl.badpackets.BadPacketA;
import ac.boar.anticheat.check.impl.place.AirPlace;
import ac.boar.anticheat.check.impl.prediction.DebugOffsetA;
import ac.boar.anticheat.check.impl.prediction.PredictionA;
import ac.boar.anticheat.check.impl.reach.Reach;
import ac.boar.anticheat.check.impl.timer.Timer;
import ac.boar.anticheat.check.impl.velocity.Velocity;
import ac.boar.anticheat.player.BoarPlayer;

import java.util.HashMap;
import java.util.List;

public class CheckHolder extends HashMap<Class<?>, Check> {
    public CheckHolder(final BoarPlayer player) {
        this.put(Timer.class, new Timer(player));

        this.put(Reach.class, new Reach(player));

        this.put(Velocity.class, new Velocity(player));
        this.put(DebugOffsetA.class, new DebugOffsetA(player));
        this.put(PredictionA.class, new PredictionA(player));

        this.put(AirPlace.class, new AirPlace(player));

        this.put(BadPacketA.class, new BadPacketA(player));
    }

    @Override
    public Check put(Class<?> key, Check value) {
        String name = key.getDeclaredAnnotation(CheckInfo.class).name(), type = key.getDeclaredAnnotation(CheckInfo.class).type();
        List<String> disabledChecks = Boar.getInstance().getConfig().disabledChecks();
        if (type.isEmpty() ? disabledChecks.contains(name) : disabledChecks.contains(name + "-" + type)) {
            return null;
        }

        return super.put(key, value);
    }
}
