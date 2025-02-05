package ac.boar.anticheat.prediction.engine.impl;

import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.engine.base.PredictionEngine;
import ac.boar.anticheat.util.math.Vec3f;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.geysermc.geyser.level.block.Fluid;

public class PredictionEngineLava extends PredictionEngine {
    public PredictionEngineLava(BoarPlayer player) {
        super(player);
    }

    @Override
    protected Vec3f travel(Vec3f vec3f) {
        return this.updateVelocity(vec3f, 0.02F);
    }

    @Override
    public Vec3f applyEndOfTick(Vec3f lv) {
        final float y = lv.y;

        float gravity = player.getEffectiveGravity(lv);
        if (player.fluidHeight.getOrDefault(Fluid.LAVA, 0F) <= player.getSwimHeight()) {
            lv = lv.multiply(0.5F, 0.8F, 0.5F);
            lv = this.applyFluidMovingSpeed(gravity, lv);
        } else {
            lv = lv.multiply(0.5F);
        }

        if (gravity != 0.0) {
            lv = lv.add(0, -gravity / 4.0F, 0);
        }

        if (player.horizontalCollision && player.doesNotCollide(lv.x, y + 0.6F - player.y + player.prevY, lv.z)) {
            lv.y = 0.3F;
        }

        return lv;
    }

    @Override
    protected Vec3f jump(Vec3f vec3f) {
        return vec3f.add(0, 0.04F, 0);
    }

    @Override
    protected boolean shouldJump() {
        float g = player.fluidHeight.getOrDefault(Fluid.LAVA, 0F);
        boolean bl = (player.touchingWater && g > 0.0) && !(player.onGround && !(g > player.getSwimHeight()));
        boolean canJumpOnGround = !player.isInLava() || player.onGround && !(g > player.getSwimHeight());
        return !bl && !canJumpOnGround && player.getInputData().contains(PlayerAuthInputData.WANT_UP);
    }

    private Vec3f applyFluidMovingSpeed(float gravity, Vec3f motion) {
        if (gravity != 0.0 && !player.swimming) {
            return new Vec3f(motion.x, motion.y - (gravity / 16.0F), motion.z);
        }

        return motion;
    }
}
