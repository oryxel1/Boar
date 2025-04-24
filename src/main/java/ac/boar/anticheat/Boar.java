package ac.boar.anticheat;

import ac.boar.anticheat.packets.input.AuthInputPackets;
import ac.boar.anticheat.packets.input.PostAuthInputPackets;
import ac.boar.anticheat.packets.other.PacketCheckRunner;
import ac.boar.anticheat.packets.other.VehiclePackets;
import ac.boar.anticheat.packets.player.*;
import ac.boar.anticheat.packets.world.*;
import lombok.Getter;

import ac.boar.anticheat.packets.other.NetworkLatencyPackets;

import ac.boar.anticheat.player.manager.BoarPlayerManager;
import ac.boar.geyser.GeyserEvents;
import ac.boar.protocol.PacketEvents;

@Getter
public class Boar {
    @Getter
    private final static Boar instance = new Boar();
    private Boar() {}

    private BoarPlayerManager playerManager;

    public void init() {
        this.playerManager = new BoarPlayerManager();
        new GeyserEvents();

        PacketEvents.getApi().register(new NetworkLatencyPackets());
        PacketEvents.getApi().register(new ChunkWorldPackets());
        PacketEvents.getApi().register(new EntityWorldPackets());
        PacketEvents.getApi().register(new PlayerDataPackets());
        PacketEvents.getApi().register(new PlayerEffectPackets());
        PacketEvents.getApi().register(new PlayerVelocityPackets());
        PacketEvents.getApi().register(new PlayerInventoryPackets());
        PacketEvents.getApi().register(new VehiclePackets());
        PacketEvents.getApi().register(new PacketCheckRunner());
        PacketEvents.getApi().register(new AuthInputPackets());
        PacketEvents.getApi().register(new PostAuthInputPackets());
    }

    public void terminate() {
        PacketEvents.getApi().terminate();
        this.playerManager.clear();
    }
}
