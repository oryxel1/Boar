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
        if (entity == null || entity.isInVehicle()) { // TODO: Impl reach check inside vehicle properly!
            return;
        }

        if (player.gameType == GameType.CREATIVE || player.gameType == GameType.SPECTATOR) { // Exempted.
            return;
        }

        final ReachResult result = calculateReach(entity);

        double distance = result.distance();
        if (distance > Boar.getConfig().toleranceReach()) {
            // Don't actually alert since this could be "java-1.8" or "java-1.9" config.... or maybe just falses
            // as long as it's not noticeable, it's fine, we can always handle the reach silently.

            if (distance != Double.MAX_VALUE) {
                Boar.debug("Cancelled hit by player " + player.getSession().getPlayerEntity().getDisplayName() + " with distance " + distance + ", mode=" + Boar.getConfig().reachJavaParityMode(), Boar.DebugMessage.WARNING);
            } else {
                Boar.debug("Cancelled hit by player " + player.getSession().getPlayerEntity().getDisplayName() + " (failed to find entity in sight)" + ", mode=" + Boar.getConfig().reachJavaParityMode(), Boar.DebugMessage.WARNING);
            }

            event.setCancelled(true);
        }

        if (player.inputMode == InputMode.TOUCH) {
            // Don't let player spoof this and hit out of 110 FOV range.
            if (MathUtil.wrapDegrees(Math.abs(player.yaw - player.interactRotation.getY())) > 110) {
                event.setCancelled(true);
            }
        }
    }

    public ReachResult calculateReach(final EntityCache entity) {
        double distance = Double.MAX_VALUE;

        // Handle this like how JE handle it.
        if (!Boar.getConfig().reachJavaParityMode().equalsIgnoreCase("bedrock")) {
            final Vec3 rotationVec = getRotationVector(player, 1);
            final Vec3 min = getEyePosition(player, 1);
            final Vec3 max = min.add(rotationVec.multiply(6F));

            final Vec3 hitResult = getEntityHitResult(entity.getCurrent().getBoundingBox(1), min, max);
            if (hitResult != null) {
                distance = Math.min(distance, hitResult.squaredDistanceTo(min));
            }

            if (entity.getPast() != null) {
                final Vec3 prevHitResult = getEntityHitResult(entity.getPast().getBoundingBox(1), min, max);
                if (prevHitResult != null) {
                    distance = Math.min(distance, prevHitResult.squaredDistanceTo(min));
                }
            }

            return new ReachResult(1, distance == Double.MAX_VALUE ? distance : Math.sqrt(distance));
        }

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
        Box lv5 = Boar.getConfig().reachJavaParityMode().equalsIgnoreCase("java-1.9") ? box.clone() : box.expand(0.1F);
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