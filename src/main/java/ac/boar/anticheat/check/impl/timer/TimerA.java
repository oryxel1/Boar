package ac.boar.anticheat.check.impl.timer;

import ac.boar.anticheat.check.api.CheckInfo;
import ac.boar.anticheat.check.api.impl.PacketCheck;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.protocol.event.CloudburstPacketEvent;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;

@CheckInfo(name = "Timer", type = "A")
public final class TimerA extends PacketCheck {
    public TimerA(final BoarPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceived(final CloudburstPacketEvent event) {
        if (!(event.getPacket() instanceof PlayerAuthInputPacket)) {
            return;
        }

        player.sendTransaction(true);
    }
}
