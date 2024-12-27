package ac.boar.anticheat;

import lombok.Getter;

import ac.boar.anticheat.packets.other.NetworkLatencyPacket;
import ac.boar.anticheat.packets.MovementCheckRunner;

import ac.boar.anticheat.player.manager.BoarPlayerManager;
import ac.boar.geyser.GeyserSessionJoinEvent;
import ac.boar.protocol.PacketEvents;

@Getter
public class Boar {
    @Getter
    private static final Boar instance = new Boar();
    private Boar() {}

    private BoarPlayerManager playerManager;

    public void init() {
        this.playerManager = new BoarPlayerManager();
        new GeyserSessionJoinEvent();

        PacketEvents.getApi().getCloudburst().register(new NetworkLatencyPacket());
        PacketEvents.getApi().getCloudburst().register(new MovementCheckRunner());
    }

    public void terminate() {
        PacketEvents.getApi().terminate();
        this.playerManager.clear();
    }
}
