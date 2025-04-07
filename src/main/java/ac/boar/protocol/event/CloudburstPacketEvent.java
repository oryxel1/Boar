package ac.boar.protocol.event;

import lombok.Data;

import ac.boar.anticheat.player.BoarPlayer;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;

import java.util.ArrayList;
import java.util.List;

@Data
public class CloudburstPacketEvent {
    private final BoarPlayer player;
    private final BedrockPacket packet;
    private boolean cancelled;

    private final List<Runnable> postTasks = new ArrayList<>();
}