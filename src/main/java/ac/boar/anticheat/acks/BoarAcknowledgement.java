package ac.boar.anticheat.acks;

import ac.boar.anticheat.Boar;
import ac.boar.anticheat.player.BoarPlayer;
import org.cloudburstmc.netty.channel.raknet.RakChildChannel;
import org.cloudburstmc.netty.channel.raknet.packet.RakDatagramPacket;
import org.cloudburstmc.netty.handler.codec.raknet.common.RakSessionCodec;

public class BoarAcknowledgement {
    public static void handle(final RakSessionCodec codec, final RakDatagramPacket datagram) {
        BoarPlayer player = null;
        for (BoarPlayer bPlayer : Boar.getInstance().getPlayerManager().values()) {
            RakSessionCodec rakSessionCodec = ((RakChildChannel) bPlayer.getSession().getUpstream().getSession().
                    getPeer().getChannel()).rakPipeline().get(RakSessionCodec.class);
            if (rakSessionCodec == codec) {
                player = bPlayer;
            }
        }

        if (player == null) {
            return;
        }

        if (player.receivedStackId.get() == player.sentStackId.get()) {
            return;
        }

        long lastLatency = player.getLatencyUtil().getLastSentTime();
        // System.out.println("acks=" + datagram.getSendTime() + ", lastLatency=" + lastLatency);

        long distance = datagram.getSendTime() - lastLatency;
        if (distance <= 1000L || lastLatency == -1 || player.inLoadingScreen || player.sinceLoadingScreen < 5) {
            return;
        }

        player.getLatencyUtil().confirmByTime(datagram.getSendTime());
    }
}
