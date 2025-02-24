package ac.boar.anticheat.check.impl.combat;

import ac.boar.anticheat.check.api.CheckInfo;
import ac.boar.anticheat.check.api.impl.PacketCheck;
import ac.boar.anticheat.compensated.cache.EntityCache;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.math.Box;
import ac.boar.anticheat.util.math.Vec3;
import ac.boar.protocol.event.CloudburstPacketEvent;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType;
import org.cloudburstmc.protocol.bedrock.packet.InteractPacket;
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket;

@CheckInfo(name = "Direction", type = "A")
public class DirectionA extends PacketCheck {
    public DirectionA(BoarPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceived(final CloudburstPacketEvent event) {
        // I doubt that minecraft still use this packet....
        if (event.getPacket() instanceof InteractPacket packet && packet.getAction() == InteractPacket.Action.DAMAGE) {
            event.setCancelled(true);
        }

        if (!(event.getPacket() instanceof InventoryTransactionPacket packet)) {
            return;
        }

        // Not an attack packet.
        if (packet.getActionType() != 1 || packet.getTransactionType() != InventoryTransactionType.ITEM_USE_ON_ENTITY) {
            return;
        }

        final EntityCache entity = player.compensatedWorld.getEntity(packet.getRuntimeEntityId());
        // Some simple validation.
        final boolean tooFarOrInvalid = entity == null || entity.getTransactionId() > player.lastReceivedId ||
                entity.getServerPosition().distance(player.position.toVector3f()) > 6.0 || entity.getPosition().distance(player.position.toVector3f()) > 6.0;
        if (tooFarOrInvalid) {
            event.setCancelled(true);
            return;
        }

        // TODO: prevent spoofing cameraOrientation
        final Vec3 viewVector = new Vec3(player.cameraOrientation);
        final Vec3 playerEye = player.position.add(0, player.dimensions.eyeHeight(), 0);
        final Vec3 endPos = playerEye.add(viewVector.multiply(12)).add(viewVector.multiply(12));

        final Box box = entity.getBoundingBox().expand(1);
        final Box prevBox = entity.getPrevBoundingBox();

        // Valid
        if (box.intersects(playerEye, endPos) || prevBox != null && prevBox.expand(1).intersects(playerEye, endPos)) {
            return;
        }

//        fail();
//        event.setCancelled(true);
    }
}
