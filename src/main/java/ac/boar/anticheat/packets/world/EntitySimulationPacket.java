package ac.boar.anticheat.packets.world;

import ac.boar.anticheat.compensated.cache.EntityCache;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.math.Vec3;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.CloudburstPacketListener;
import org.bukkit.Bukkit;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;

import java.util.Set;

public class EntitySimulationPacket implements CloudburstPacketListener {
    @Override
    public void onPacketSend(final CloudburstPacketEvent event, final boolean immediate) {
        final BoarPlayer player = event.getPlayer();
        if (event.getPacket() instanceof AddEntityPacket packet) {
            final EntityCache entity = player.compensatedWorld.addToCache(packet.getRuntimeEntityId(), packet.getUniqueEntityId());
            if (entity == null) {
                return;
            }

//            entity.setServerPosition(new Vec3(packet.getPosition()));
//            entity.setPosition(new Vec3(packet.getPosition()));
//            entity.init();
        }

        if (event.getPacket() instanceof AddPlayerPacket packet) {
            final EntityCache entity = player.compensatedWorld.addToCache(packet.getRuntimeEntityId(), packet.getUniqueEntityId());
            if (entity == null) {
                return;
            }

//            entity.setServerPosition(new Vec3(packet.getPosition()));
//            entity.setPosition(new Vec3(packet.getPosition()));
//            entity.init();
        }

        if (event.getPacket() instanceof RemoveEntityPacket packet) {
            player.compensatedWorld.removeEntity(packet.getUniqueEntityId());
        }

        if (event.getPacket() instanceof MoveEntityDeltaPacket packet) {
            final EntityCache entity = player.compensatedWorld.getEntity(packet.getRuntimeEntityId());
            if (entity == null) {
                return;
            }

            final Set<MoveEntityDeltaPacket.Flag> flags = packet.getFlags();

            final boolean useless = !flags.contains(MoveEntityDeltaPacket.Flag.HAS_X) && !flags.contains(MoveEntityDeltaPacket.Flag.HAS_Y) && !flags.contains(MoveEntityDeltaPacket.Flag.HAS_Z);
            if (useless) {
                return;
            }

//            float x = packet.getX(), y = packet.getY(), z = packet.getZ();
//            if (!flags.contains(MoveEntityDeltaPacket.Flag.HAS_X)) {
//                x = entity.getServerPosition().getX();
//            }
//            if (!flags.contains(MoveEntityDeltaPacket.Flag.HAS_Y)) {
//                y = entity.getServerPosition().getY();
//            }
//            if (!flags.contains(MoveEntityDeltaPacket.Flag.HAS_Z)) {
//                z = entity.getServerPosition().getZ();
//            }

            // this.queuePositionUpdate(event, entity, Vector3f.from(x, y, z), true);
        }

        if (event.getPacket() instanceof MoveEntityAbsolutePacket packet) {
            final EntityCache entity = player.compensatedWorld.getEntity(packet.getRuntimeEntityId());
            if (entity == null) {
                return;
            }

            this.queuePositionUpdate(event, entity, packet.getPosition(), false);
        }

        if (event.getPacket() instanceof MovePlayerPacket packet) {
            if (packet.getRuntimeEntityId() == player.runtimeEntityId) {
                return;
            }

            final EntityCache entity = player.compensatedWorld.getEntity(packet.getRuntimeEntityId());
            if (entity == null || packet.getMode() == MovePlayerPacket.Mode.HEAD_ROTATION) {
                return;
            }

            this.queuePositionUpdate(event, entity, packet.getPosition(), packet.getMode() == MovePlayerPacket.Mode.NORMAL);
        }
    }

    private void queuePositionUpdate(final CloudburstPacketEvent event, final EntityCache entity, final Vector3f raw, final boolean lerp) {
    }
}