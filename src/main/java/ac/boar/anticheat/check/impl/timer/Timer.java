package ac.boar.anticheat.check.impl.timer;

import ac.boar.anticheat.check.api.annotations.CheckInfo;
import ac.boar.anticheat.check.api.annotations.Experimental;
import ac.boar.anticheat.check.api.impl.PacketCheck;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.protocol.event.CloudburstPacketEvent;
import org.cloudburstmc.protocol.bedrock.packet.NetworkStackLatencyPacket;

import static ac.boar.anticheat.packets.other.NetworkLatencyPackets.*;

@Experimental
@CheckInfo(name = "Timer")
public final class Timer extends PacketCheck {
    private static final long AVERAGE_DISTANCE = (long) 5e+7;

    private long lastNS, balance, prevTick;
    private boolean sentBeforeInput = true;

    private long cachedLatencyId = -1, lastLatencyTime;

    public Timer(final BoarPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceived(final CloudburstPacketEvent event) {
        // If this doesn't get cancelled, it's not ours. (NetworkLatencyPackets#onPacketReceived)
        // Also don't check for packet that is not latency packet (obviously).
        if (!event.isCancelled() || !(event.getPacket() instanceof NetworkStackLatencyPacket packet)) {
            return;
        }

        long id = Math.abs(packet.getTimestamp() / LATENCY_MAGNITUDE);
        if (id >= this.cachedLatencyId) {
            this.cachedLatencyId = -1;
            this.lastNS = Math.max(this.lastNS, player.getLatencyUtil().getLastSentTime());
        }
    }

    @Override
    public void onPacketSend(final CloudburstPacketEvent event, boolean immediate) {
        if (!(event.getPacket() instanceof NetworkStackLatencyPacket packet)) {
            return;
        }

        long absTimestamp = Math.abs(packet.getTimestamp());
        // Make sure we're the one send this...
        if (packet.getTimestamp() > 0 || !player.getLatencyUtil().hasId(absTimestamp) || !packet.isFromServer()) {
            return;
        }

        if (this.sentBeforeInput) {
            this.cachedLatencyId = absTimestamp;
            this.sentBeforeInput = false;
        }
    }

    public boolean isInvalid() {
        if (this.lastNS == 0 || player.inLoadingScreen || player.sinceLoadingScreen < 20) {
            this.lastNS = System.nanoTime();
            this.prevTick = player.tick;
            this.balance = 0;

            this.sentBeforeInput = true;
            return false;
        }

        this.sentBeforeInput = true;

        boolean valid = true;

        long distance = System.nanoTime() - this.lastNS;
        long neededDistance = (player.tick - this.prevTick) * AVERAGE_DISTANCE;

        if (this.balance > AVERAGE_DISTANCE + 1e+7 + 3e+6) {
            this.fail("balance=" + this.balance + ", player is ahead!");
            player.getTeleportUtil().teleportTo(player.getTeleportUtil().getLastKnowValid());
            this.balance -= AVERAGE_DISTANCE;
            valid = false;
        }

        this.balance -= distance - neededDistance;
        this.lastNS = System.nanoTime();
        this.prevTick = player.tick;
        return !valid;
    }
}
