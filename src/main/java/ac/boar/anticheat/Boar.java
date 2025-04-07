package ac.boar.anticheat;

import ac.boar.anticheat.packets.input.PostAuthInputPacket;
import ac.boar.anticheat.packets.other.PacketCheckRunner;
import ac.boar.anticheat.packets.player.*;
import ac.boar.anticheat.packets.world.*;
import lombok.Getter;

import ac.boar.anticheat.packets.other.NetworkLatencyPacket;
import ac.boar.anticheat.packets.input.AuthInputPacket;

import ac.boar.anticheat.player.manager.BoarPlayerManager;
import ac.boar.geyser.GeyserSessionJoinEvent;
import ac.boar.protocol.PacketEvents;

@Getter
public class Boar {
    @Getter
    private final static Boar instance = new Boar();
    private Boar() {}

    private BoarPlayerManager playerManager;

    public void init() {
        this.playerManager = new BoarPlayerManager();
        new GeyserSessionJoinEvent();

        PacketEvents.getApi().register(new NetworkLatencyPacket());
        PacketEvents.getApi().register(new ChunkWorldPackets());
        PacketEvents.getApi().register(new EntitySimulationPacket());
        PacketEvents.getApi().register(new DataSimulationPacket());
        PacketEvents.getApi().register(new PlayerEffectPacket());
        PacketEvents.getApi().register(new PlayerTeleportPacket());
        PacketEvents.getApi().register(new PlayerVelocityPacket());
        PacketEvents.getApi().register(new InventorySimulationPacket());
        PacketEvents.getApi().register(new AuthInputPacket());
        PacketEvents.getApi().register(new PacketCheckRunner());
        PacketEvents.getApi().register(new PostAuthInputPacket());
    }

    public void terminate() {
        PacketEvents.getApi().terminate();
        this.playerManager.clear();
    }
}
