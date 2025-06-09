package ac.boar.anticheat;

import ac.boar.anticheat.config.Config;
import ac.boar.anticheat.config.ConfigLoader;
import ac.boar.anticheat.packets.input.AuthInputPackets;
import ac.boar.anticheat.packets.input.PostAuthInputPackets;
import ac.boar.anticheat.packets.other.PacketCheckRunner;
import ac.boar.anticheat.packets.other.VehiclePackets;
import ac.boar.anticheat.packets.player.*;
import ac.boar.anticheat.packets.world.*;
import ac.boar.geyser.GeyserBoar;
import ac.boar.mappings.BedrockMappings;
import lombok.Getter;

import ac.boar.anticheat.packets.other.NetworkLatencyPackets;

import ac.boar.anticheat.player.manager.BoarPlayerManager;
import ac.boar.protocol.PacketEvents;

import java.util.List;

@Getter
public class Boar {
    @Getter
    private final static Boar instance = new Boar();
    @Getter
    private static Config config;
    private Boar() {}

    private BoarPlayerManager playerManager;

    public void init(GeyserBoar instance) {
        config = ConfigLoader.load(instance, GeyserBoar.class, Config.class, new Config(
                20, 1.0E-4F, List.of(), false));
        System.out.println("Load config: " + config);

        BedrockMappings.load();

        this.playerManager = new BoarPlayerManager();

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

    public void terminate(GeyserBoar instance) {
        PacketEvents.getApi().terminate();
        this.playerManager.clear();

        ConfigLoader.save(instance, GeyserBoar.class, config);
    }
}
