package ac.boar.anticheat.packets.world;

import ac.boar.anticheat.compensated.cache.BoarEntity;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.math.Vec3f;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.CloudburstPacketListener;
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
            final BoarEntity entity = player.compensatedWorld.addToCache(packet.getRuntimeEntityId(), packet.getUniqueEntityId());
            if (entity == null) {
                return;
            }

            entity.setServerPosition(packet.getPosition());
            entity.setPosition(packet.getPosition());
            entity.setBoundingBox(entity.getDimensions().getBoxAt(new Vec3f(packet.getPosition())));
        }

        if (event.getPacket() instanceof AddPlayerPacket packet) {
            final BoarEntity entity = player.compensatedWorld.addToCache(packet.getRuntimeEntityId(), packet.getUniqueEntityId());
            if (entity == null) {
                return;
            }

            entity.setServerPosition(packet.getPosition());
            entity.setPosition(packet.getPosition());
            entity.setBoundingBox(entity.getDimensions().getBoxAt(new Vec3f(packet.getPosition())));
        }


        if (event.getPacket() instanceof RemoveEntityPacket packet) {
            player.compensatedWorld.removeEntity(packet.getUniqueEntityId());
        }

        if (event.getPacket() instanceof MoveEntityDeltaPacket packet) {
            final BoarEntity entity = player.compensatedWorld.getEntity(packet.getRuntimeEntityId());
            if (entity == null) {
                return;
            }

            final Set<MoveEntityDeltaPacket.Flag> flags = packet.getFlags();

            final boolean useless = !flags.contains(MoveEntityDeltaPacket.Flag.HAS_X) && !flags.contains(MoveEntityDeltaPacket.Flag.HAS_Y) && !flags.contains(MoveEntityDeltaPacket.Flag.HAS_Z);
            if (useless) {
                return;
            }

            float x = packet.getX(), y = packet.getY(), z = packet.getZ();
            if (!flags.contains(MoveEntityDeltaPacket.Flag.HAS_X)) {
                x = entity.getServerPosition().getX();
            }
            if (!flags.contains(MoveEntityDeltaPacket.Flag.HAS_Y)) {
                y = entity.getServerPosition().getY();
            }
            if (!flags.contains(MoveEntityDeltaPacket.Flag.HAS_Z)) {
                z = entity.getServerPosition().getZ();
            }

            this.queuePositionUpdate(event, entity, Vector3f.from(x, y, z));
        }

        if (event.getPacket() instanceof MoveEntityAbsolutePacket packet) {
            final BoarEntity entity = player.compensatedWorld.getEntity(packet.getRuntimeEntityId());
            if (entity == null) {
                return;
            }

            this.queuePositionUpdate(event, entity, packet.getPosition());
        }

        if (event.getPacket() instanceof MovePlayerPacket packet) {
            if (packet.getRuntimeEntityId() == player.runtimeEntityId) {
                return;
            }

            final BoarEntity entity = player.compensatedWorld.getEntity(packet.getRuntimeEntityId());
            if (entity == null) {
                return;
            }

            this.queuePositionUpdate(event, entity, packet.getPosition());
        }
    }

    private void queuePositionUpdate(final CloudburstPacketEvent event, final BoarEntity entity, final Vector3f raw) {
        final BoarPlayer player = event.getPlayer();
        final Vector3f position = raw.sub(0, entity.getType() == EntityType.PLAYER ? EntityDefinitions.PLAYER.offset() : 0, 0);

        if (position.distance(entity.getServerPosition()) < 1.0E-7) {
            return;
        }

        entity.setServerPosition(position);

        // We need 2 transaction to check, if player receive the first transaction they could already have received the packet
        // Or they could lag right before they receive the actual update position packet so we can't be sure
        // But if player respond to the transaction AFTER the position packet they 100% already receive the packet.
        player.sendTransaction();
        player.latencyUtil.addTransactionToQueue(player.lastSentId, () -> {
            entity.setPosition(position);
            entity.setPrevBoundingBox(entity.getBoundingBox());
            entity.setBoundingBox(entity.getDimensions().getBoxAt(position.getX(), position.getY(), position.getZ()));
        });

        player.latencyUtil.addTransactionToQueue(player.lastSentId + 1, () -> entity.setPrevBoundingBox(null));
        event.getPostTasks().add(player::sendTransaction);
    }
}