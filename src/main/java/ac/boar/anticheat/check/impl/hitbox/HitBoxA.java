package ac.boar.anticheat.check.impl.hitbox;

import ac.boar.anticheat.check.api.impl.PacketCheck;
import ac.boar.anticheat.compensated.cache.BoarEntity;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.protocol.event.CloudburstPacketEvent;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType;
import org.cloudburstmc.protocol.bedrock.packet.InteractPacket;
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket;

public class HitBoxA extends PacketCheck {
    public HitBoxA(BoarPlayer player) {
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
        if (packet.getActionType() != 0 || packet.getTransactionType() != InventoryTransactionType.ITEM_USE_ON_ENTITY) {
            return;
        }

        final BoarEntity entity = player.compensatedWorld.getEntity(packet.getRuntimeEntityId());
        // Some simple validation.
        final boolean tooFarOrInvalid = entity == null || entity.getTransactionId() > player.lastReceivedId ||
                entity.getServerPosition().distance(player.x, player.y, player.z) > 6.0 || entity.getPosition().distance(player.x, player.y, player.z) > 6.0;
        if (tooFarOrInvalid) {
            event.setCancelled(true);
            return;
        }
    }
}
