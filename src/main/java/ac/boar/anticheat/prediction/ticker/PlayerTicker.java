package ac.boar.anticheat.prediction.ticker;

import ac.boar.anticheat.collision.Collision;
import ac.boar.anticheat.data.EntityDimensions;
import ac.boar.anticheat.data.EntityPose;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.ticker.base.LivingTicker;

public class PlayerTicker extends LivingTicker {
    public PlayerTicker(BoarPlayer player) {
        super(player);
    }

    @Override
    public void tickMovement() {
        if ((player.wasSneaking || player.sneaking) && !player.gliding && !player.isInLava() && !player.touchingWater) {
            player.movementInput = player.movementInput.multiply(0.3F);
        }

        super.tickMovement();
    }

    @Override
    public void tick() {
        super.tick();
        updatePose();
    }

    private void updatePose() {
        if (this.canChangeIntoPose(EntityPose.SWIMMING)) {
            EntityPose lv = getEntityPose();

            EntityPose lv2;
            if (/* this.isSpectator() || this.hasVehicle() || */ this.canChangeIntoPose(lv)) {
                lv2 = lv;
            } else if (this.canChangeIntoPose(EntityPose.CROUCHING)) {
                lv2 = EntityPose.CROUCHING;
            } else {
                lv2 = EntityPose.SWIMMING;
            }

            player.pose = lv2;
        }
    }

    private EntityPose getEntityPose() {
        EntityPose lv;
        if (player.gliding) {
            lv = EntityPose.GLIDING;
        } else if (player.getSession().getPlayerEntity().getBedPosition() != null) {
            lv = EntityPose.SLEEPING;
        } else if (player.swimming) {
            lv = EntityPose.SWIMMING;
        } else if (/* this.isUsingRiptide() */ false) {
            lv = EntityPose.SPIN_ATTACK;
        } else if ((player.sneaking || player.wasSneaking)) {
            lv = EntityPose.CROUCHING;
        } else {
            lv = EntityPose.STANDING;
        }
        return lv;
    }

    private boolean canChangeIntoPose(final EntityPose pose) {
        return Collision.isSpaceEmpty(player, EntityDimensions.POSE_DIMENSIONS.get(pose).getBoxAt(player.x, player.y, player.z).contract(1.0E-7F));
    }
}
