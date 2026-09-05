package ac.boar.anticheat.packets.other;

import ac.boar.anticheat.ack.types.VehicleLinkAck;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.protocol.api.CloudburstPacketEvent;
import ac.boar.protocol.api.PacketListener;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityLinkData;
import org.cloudburstmc.protocol.bedrock.packet.InteractPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityLinkPacket;

public class VehiclePackets implements PacketListener {
    @Override
    public void onPacketReceived(CloudburstPacketEvent event) {
        final BoarPlayer player = event.getPlayer();

        if (event.getPacket() instanceof InteractPacket packet) {
            if (packet.getRuntimeEntityId() != player.runtimeEntityId) {
                return;
            }

            if (packet.getAction() == InteractPacket.Action.LEAVE_VEHICLE) {
                player.vehicle = null;
            }
        }
    }

    @Override
    public void onPacketSend(CloudburstPacketEvent event) {
        final BoarPlayer player = event.getPlayer();
        if (event.getPacket() instanceof SetEntityLinkPacket packet) {
            final EntityLinkData link = packet.getEntityLink();
            if (link == null) {
                return;
            }

            System.out.println("queue~!");
            player.queueAcknowledgment(new VehicleLinkAck(link));
        }
    }
}
