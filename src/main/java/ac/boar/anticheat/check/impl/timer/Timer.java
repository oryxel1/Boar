package ac.boar.anticheat.check.impl.timer;

import ac.boar.anticheat.check.api.annotations.CheckInfo;
import ac.boar.anticheat.check.api.annotations.Experimental;
import ac.boar.anticheat.check.api.impl.PingBasedCheck;
import ac.boar.anticheat.player.BoarPlayer;

@Experimental
@CheckInfo(name = "Timer")
public final class Timer extends PingBasedCheck {
    private static final long AVERAGE_DISTANCE = (long) 5e+7;

    private long lastNS, balance, prevTick;

    public Timer(final BoarPlayer player) {
        super(player);
    }

    @Override
    public void onLatencyAccepted(long id, long time) {
    }

    public boolean isInvalid() {
        if (this.lastNS == 0 || player.inLoadingScreen || player.sinceLoadingScreen < 20) {
            this.lastNS = System.nanoTime();
            this.prevTick = player.tick;
            this.balance = 0;
            return false;
        }

        boolean valid = true;

        long distance = System.nanoTime() - this.lastNS;
        long neededDistance = (player.tick - this.prevTick) * AVERAGE_DISTANCE;

        if (this.balance > AVERAGE_DISTANCE + 1e+7 + 3e+6) {
            this.fail("balance=" + this.balance + ", player is ahead!");
            // Send a teleport that we won't cache just so the player know they're getting flagged (assuming they also get flagged next tick).
            // Assume that they don't get flagged next tick however, rewind handle this. The flag will be way more violent this way...
            // Side note: Due to how rewind works, it might *look* like they're gaining advantage but the *should* still be moving at the same speed as legit
            // maybe even slower due to the violent setback.
            player.getTeleportUtil().hardResyncWithoutAcknowledgement();
            this.balance -= AVERAGE_DISTANCE;
            valid = false;
        }

        this.balance -= distance - neededDistance;
        this.lastNS = Math.max(this.lastNS, System.nanoTime());
        this.prevTick = player.tick;
        return !valid;
    }
}