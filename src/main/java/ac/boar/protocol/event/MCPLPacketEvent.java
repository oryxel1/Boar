package ac.boar.protocol.event;

import lombok.Data;

import ac.boar.anticheat.player.BoarPlayer;
import org.geysermc.mcprotocollib.network.packet.Packet;

import java.util.ArrayList;
import java.util.List;

@Data
public class MCPLPacketEvent {
    private final BoarPlayer player;
    private final Packet packet;
    private boolean cancelled;

    private final List<Runnable> postTasks = new ArrayList<>();
}
