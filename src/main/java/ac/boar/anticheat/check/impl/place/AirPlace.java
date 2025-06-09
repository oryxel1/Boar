package ac.boar.anticheat.check.impl.place;

import ac.boar.anticheat.check.api.Check;
import ac.boar.anticheat.check.api.annotations.CheckInfo;
import ac.boar.anticheat.player.BoarPlayer;

@CheckInfo(name = "Air Place")
public class AirPlace extends Check {
    public AirPlace(BoarPlayer player) {
        super(player);
    }
}
