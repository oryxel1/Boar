package ac.boar.anticheat.check.api.impl;

import ac.boar.anticheat.check.api.Check;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.protocol.listener.CloudburstPacketListener;
import ac.boar.protocol.listener.MCPLPacketListener;

public class PacketCheck extends Check implements CloudburstPacketListener, MCPLPacketListener {
    public PacketCheck(BoarPlayer player) {
        super(player);
    }
}
