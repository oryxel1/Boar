package ac.boar.anticheat.prediction.engine.impl;

import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.engine.base.PredictionEngine;
import ac.boar.anticheat.util.math.Vec3;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.geysermc.geyser.level.block.Fluid;

public class PredictionEngineLava extends PredictionEngine {
    public PredictionEngineLava(BoarPlayer player) {
        super(player);
    }

    @Override
    protected Vec3 travel(Vec3 vec3) {
        return this.updateVelocity(vec3, 0.02F);
    }

    @Override
    public void finalizeMovement() {
        final float y = player.velocity.y;

        float gravity = player.getEffectiveGravity();
        if (player.fluidHeight.getOrDefault(Fluid.LAVA, 0F) <= player.getSwimHeight()) {
            player.velocity = player.velocity.multiply(0.5F, 0.8F, 0.5F);
            player.velocity = this.applyFluidMovingSpeed(gravity, player.velocity);
        } else {
            player.velocity = player.velocity.multiply(0.5F);
        }

        if (gravity != 0.0) {
            player.velocity = player.velocity.add(0, -gravity / 4.0F, 0);
        }

//        if (player.horizontalCollision && player.doesNotCollide(lv.x, y + 0.6F - player.position.y + player.prevPosition.y, lv.z)) {
//            lv.y = 0.3F;
//        }
    }

    @Override
    protected Vec3 jump(Vec3 vec3) {
        return vec3.add(0, 0.04F, 0);
    }

    @Override
    protected boolean shouldJump() {
        float g = player.fluidHeight.getOrDefault(Fluid.LAVA, 0F);
        boolean bl = (player.touchingWater && g > 0.0) && !(player.groundCollision && !(g > player.getSwimHeight()));
        boolean canJumpOnGround = !player.isInLava() || player.groundCollision && !(g > player.getSwimHeight());
        return !bl && !canJumpOnGround && player.getInputData().contains(PlayerAuthInputData.WANT_UP);
    }

    private Vec3 applyFluidMovingSpeed(float gravity, Vec3 motion) {
        if (gravity != 0.0 && !player.swimming) {
            return new Vec3(motion.x, motion.y - (gravity / 16.0F), motion.z);
        }

        return motion;
    }
}
