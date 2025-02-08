package ac.boar.anticheat;

import ac.boar.anticheat.packets.other.FinalPacketListener;
import ac.boar.anticheat.packets.other.PacketCheckRunner;
import ac.boar.anticheat.packets.player.*;
import ac.boar.anticheat.packets.world.*;
import lombok.Getter;

import ac.boar.anticheat.packets.other.NetworkLatencyPacket;
import ac.boar.anticheat.packets.MovementCheckRunner;

import ac.boar.anticheat.player.manager.BoarPlayerManager;
import ac.boar.geyser.GeyserSessionJoinEvent;
import ac.boar.protocol.PacketEvents;

@Getter
public class Boar {
    @Getter
    private final static Boar instance = new Boar();
    private Boar() {}

    public final static boolean IS_IN_DEBUGGING = true;

    private BoarPlayerManager playerManager;

    public void init() {
        this.playerManager = new BoarPlayerManager();
        new GeyserSessionJoinEvent();

        PacketEvents.getApi().getCloudburst().register(new NetworkLatencyPacket());
        PacketEvents.getApi().getCloudburst().register(new ChunkSimulationPacket());
        PacketEvents.getApi().getCloudburst().register(new EntitySimulationPacket());
        PacketEvents.getApi().getCloudburst().register(new DataSimulationPacket());
        PacketEvents.getApi().getCloudburst().register(new PlayerEffectPacket());
        PacketEvents.getApi().getCloudburst().register(new PlayerTeleportPacket());
        PacketEvents.getApi().getCloudburst().register(new PlayerVelocityPacket());
        PacketEvents.getApi().getCloudburst().register(new MovementCheckRunner());
        PacketEvents.getApi().getCloudburst().register(new PacketCheckRunner());
        PacketEvents.getApi().getCloudburst().register(new FinalPacketListener());

        PacketEvents.getApi().getMcpl().register(new ChunkSimulationPacket());
        PacketEvents.getApi().getMcpl().register(new DataSimulationPacket());
        PacketEvents.getApi().getMcpl().register(new PacketCheckRunner());
    }

    public void terminate() {
        PacketEvents.getApi().terminate();
        this.playerManager.clear();
    }
}
