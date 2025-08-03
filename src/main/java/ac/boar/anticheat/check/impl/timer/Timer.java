package ac.boar.anticheat.check.impl.timer;

import ac.boar.anticheat.check.api.annotations.CheckInfo;
import ac.boar.anticheat.check.api.annotations.Experimental;
import ac.boar.anticheat.check.api.impl.PacketCheck;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.protocol.event.CloudburstPacketEvent;
import org.cloudburstmc.protocol.bedrock.packet.NetworkStackLatencyPacket;

@Experimental
@CheckInfo(name = "Timer")
public final class Timer extends PacketCheck {
    private static final long AVERAGE_DISTANCE = (long) 5e+7;

    private long lastNS, balance, prevTick;

    public Timer(final BoarPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceived(CloudburstPacketEvent event) {
        if (event.getPacket() instanceof NetworkStackLatencyPacket) {
            // TODO: Fix this...
            // this.lastNS = Math.max(this.lastNS, player.getLatencyUtil().getLastSentTime());
        }
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

        if (this.balance > AVERAGE_DISTANCE + 1e+7) {
            this.fail("balance=" + this.balance + ", player is ahead!");
            player.getTeleportUtil().teleportTo(player.getTeleportUtil().getLastKnowValid());
            this.balance -= AVERAGE_DISTANCE;
            valid = false;
        }

        this.balance -= distance - neededDistance;
        this.lastNS = Math.max(this.lastNS, System.nanoTime());
        this.prevTick = player.tick;
        return !valid;
    }
}
