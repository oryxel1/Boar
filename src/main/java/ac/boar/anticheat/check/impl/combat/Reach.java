package ac.boar.anticheat.check.impl.combat;

import ac.boar.anticheat.GlobalSetting;
import ac.boar.anticheat.check.api.annotations.CheckInfo;
import ac.boar.anticheat.check.api.annotations.Experimental;
import ac.boar.anticheat.check.api.impl.PacketCheck;
import ac.boar.anticheat.compensated.cache.EntityCache;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.ChatUtil;
import ac.boar.anticheat.util.math.Box;
import ac.boar.anticheat.util.math.Vec3;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.util.MathUtil;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType;
import org.cloudburstmc.protocol.bedrock.packet.InteractPacket;
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;

import java.util.Optional;

@Experimental
@CheckInfo(name = "Reach", type = "A")
public final class Reach extends PacketCheck {
    public Reach(BoarPlayer player) {
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
        if (entity == null) {
            return;
        }

        // Not yet...
        if (entity.getType() != EntityType.PLAYER) {
            return;
        }

        final Vec3 rotationVec = MathUtil.getRotationVector(player.interactRotation.getX(), player.interactRotation.getY());
        final Vec3 min = player.unvalidatedPosition.add(0, player.dimensions.eyeHeight(), 0);
        final Vec3 max = min.add(rotationVec.multiply(6));

        final Vec3 hitResult = getEntityHitResult(entity.getBoundingBox(), min, max, MathUtil.square(6));
        final Vec3 prevHitResult = entity.getPrevBoundingBox() != null ? getEntityHitResult(entity.getPrevBoundingBox(), min, max, MathUtil.square(6)) : null;
        double distance = Double.MAX_VALUE;

        if (hitResult != null) {
            distance = hitResult.squaredDistanceTo(min);
        }

        if (prevHitResult != null) {
            distance = Math.min(distance, prevHitResult.squaredDistanceTo(min));
        }

        if (distance != Double.MAX_VALUE) {
            distance = Math.sqrt(distance);
        }

        if (distance > GlobalSetting.MAX_REACH_DISTANCE) {
            fail(distance == Double.MAX_VALUE ? "entity not in sight!" : "d=" + distance);
        }

        ChatUtil.alert("d=" + distance);
    }

    private Vec3 getEntityHitResult(final Box box, final Vec3 min, final Vec3 max, final double maxDistance) {
        Vec3 lv3 = null;

        Box lv5 = box.expand(/*(double)lv4.getTargetingMargin()*/ 0.1F);
        Optional<Vec3> optional = lv5.clip(min, max);
        if (lv5.contains(min)) {
            lv3 = optional.orElse(min);
        } else if (optional.isPresent()) {
            Vec3 lv6 = optional.get();
            if (min.squaredDistanceTo(lv6) < maxDistance) {
                lv3 = lv6;
            }
        }

        return lv3;
    }
}
