package ac.boar.anticheat.packets.server;

import ac.boar.protocol.api.CloudburstPacketEvent;
import ac.boar.protocol.api.PacketListener;
import org.cloudburstmc.protocol.bedrock.packet.MobEffectPacket;
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateAttributesPacket;

public final class ServerRewindPackets implements PacketListener {
    @Override
    public void onPacketSend(final CloudburstPacketEvent event) {
        if (event.getPacket() instanceof MobEffectPacket packet) {
            packet.setTick(0L);
        } else if (event.getPacket() instanceof MovePlayerPacket packet) {
            packet.setTick(0L);
        } else if (event.getPacket() instanceof SetEntityDataPacket packet) {
            packet.setTick(0L);
        } else if (event.getPacket() instanceof SetEntityMotionPacket packet) {
            packet.setTick(0L);
        } else if (event.getPacket() instanceof UpdateAttributesPacket packet) {
            packet.setTick(0L);
        }
    }
}
