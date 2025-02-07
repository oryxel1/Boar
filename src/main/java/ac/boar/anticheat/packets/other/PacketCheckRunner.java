package ac.boar.anticheat.packets.other;

import ac.boar.anticheat.check.api.Check;
import ac.boar.anticheat.check.api.impl.PacketCheck;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.event.MCPLPacketEvent;
import ac.boar.protocol.listener.CloudburstPacketListener;
import ac.boar.protocol.listener.MCPLPacketListener;

public class PacketCheckRunner implements CloudburstPacketListener, MCPLPacketListener {
    @Override
    public void onPacketSend(final CloudburstPacketEvent event, final boolean immediate) {
        for (final Check check : event.getPlayer().checkHolder.values()) {
            if (!(check instanceof PacketCheck packetCheck)) {
                continue;
            }

            packetCheck.onPacketSend(event, immediate);
        }
    }

    @Override
    public void onPacketSend(final MCPLPacketEvent event) {
        for (final Check check : event.getPlayer().checkHolder.values()) {
            if (!(check instanceof PacketCheck packetCheck)) {
                continue;
            }

            packetCheck.onPacketSend(event);
        }
    }

    @Override
    public void onPacketReceived(final CloudburstPacketEvent event) {
        for (final Check check : event.getPlayer().checkHolder.values()) {
            if (!(check instanceof PacketCheck packetCheck)) {
                continue;
            }

            packetCheck.onPacketReceived(event);
        }
    }
}
