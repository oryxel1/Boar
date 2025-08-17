package ac.boar.anticheat.check.api.impl;

import ac.boar.anticheat.check.api.Check;
import ac.boar.anticheat.player.BoarPlayer;

public class PingBasedCheck extends Check {
    public PingBasedCheck(BoarPlayer player) {
        super(player);
    }

    public void onLatencySend(long id) {}
    public void onLatencyAccepted(long id, long time) {}
}
