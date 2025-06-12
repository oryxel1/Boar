package ac.boar.anticheat.check.impl.reach;

import ac.boar.anticheat.Boar;
import ac.boar.anticheat.check.api.annotations.CheckInfo;
import ac.boar.anticheat.check.api.annotations.Experimental;
import ac.boar.anticheat.check.api.impl.PacketCheck;
import ac.boar.anticheat.compensated.cache.entity.EntityCache;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.MathUtil;
import ac.boar.anticheat.util.math.Box;
import ac.boar.anticheat.util.math.Vec3;
import ac.boar.protocol.event.CloudburstPacketEvent;

import org.cloudburstmc.protocol.bedrock.data.GameType;
import org.cloudburstmc.protocol.bedrock.data.InputMode;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType;
import org.cloudburstmc.protocol.bedrock.packet.InteractPacket;
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket;

import java.util.Optional;

@Experimental
@CheckInfo(name = "Reach")
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

        // Not an attack packet.
        if (!(event.getPacket() instanceof InventoryTransactionPacket packet) || packet.getActionType() != 1 || packet.getTransactionType() != InventoryTransactionType.ITEM_USE_ON_ENTITY) {
            return;
        }

        final EntityCache entity = player.compensatedWorld.getEntity(packet.getRuntimeEntityId());
        if (entity == null) {
            return;
        }

        if (player.gameType == GameType.CREATIVE || player.gameType == GameType.SPECTATOR) { // Exempted.
            return;
        }

        final ReachResult result = calculateReach(entity);

        double distance = result.distance();
        if (distance > Boar.getConfig().toleranceReach()) {
            if (distance != Double.MAX_VALUE) {
                fail("d=" + distance);
            } else {
                // This seems to be falsing from time to time and ehmmmm, this sucks lol.
                // fail("hitboxes!" + ", deltaTicks=" + result.deltaTicks());
            }

            event.setCancelled(true);
        } else {
            // Boar.getInstance().getAlertManager().alert("Distance=" + distance + "," + result.deltaTicks());
        }

        if (player.inputMode == InputMode.TOUCH) {
            // Don't let player spoof this and hit out of 110 FOV range.
            if (MathUtil.wrapDegrees(Math.abs(player.yaw - player.interactRotation.getY())) > 110) {
                event.setCancelled(true);
            }
        }

        // ChatUtil.alert(player,"d=" + distance);
    }

    public ReachResult calculateReach(final EntityCache entity) {
        double distance = Double.MAX_VALUE;

        float deltaTicks;
        for (deltaTicks = 0F; deltaTicks <= 1; deltaTicks += 0.1F) {
            final Vec3 rotationVec = getRotationVector(player, deltaTicks);
            final Vec3 min = getEyePosition(player, deltaTicks);
            final Vec3 max = min.add(rotationVec.multiply(7F));

            final Vec3 hitResult = getEntityHitResult(entity.getCurrent().getBoundingBox(deltaTicks), min, max);
            if (hitResult != null) {
                distance = Math.min(distance, hitResult.squaredDistanceTo(min));
            }

            if (entity.getPast() != null) {
                final Vec3 prevHitResult = getEntityHitResult(entity.getPast().getBoundingBox(deltaTicks), min, max);
                if (prevHitResult != null) {
                    distance = Math.min(distance, prevHitResult.squaredDistanceTo(min));
                }
            }

            if (distance <= 3.005 * 3.005) {
                break;
            }
        }

        return new ReachResult(deltaTicks, distance == Double.MAX_VALUE ? distance : Math.sqrt(distance));
    }

    private Vec3 getEntityHitResult(final Box box, final Vec3 min, final Vec3 max) {
        Box lv5 = box.expand(0.1F);
        Optional<Vec3> vec3 = lv5.clip(min, max);
        if (lv5.contains(min)) {
            return min;
        }

        return vec3.orElse(null);
    }

    private Vec3 getRotationVector(BoarPlayer player, float f) {
        float lerpedX = player.inputMode == InputMode.TOUCH ? player.interactRotation.getX() : MathUtil.lerp(f, player.prevPitch, player.pitch);
        float lerpedY = player.inputMode == InputMode.TOUCH ? player.interactRotation.getY() : MathUtil.lerp(f, player.prevYaw, player.yaw);

        return MathUtil.getRotationVector(lerpedX, lerpedY);
    }

    private Vec3 getEyePosition(BoarPlayer player, float f) {
        float d = MathUtil.lerp(f, player.prevPosition.x, player.position.x);
        float e = MathUtil.lerp(f, player.prevPosition.y, player.position.y) + player.dimensions.eyeHeight();
        float g = MathUtil.lerp(f, player.prevPosition.z, player.position.z);
        return new Vec3(d, e, g);
    }

    public record ReachResult(float deltaTicks, double distance) {}
}