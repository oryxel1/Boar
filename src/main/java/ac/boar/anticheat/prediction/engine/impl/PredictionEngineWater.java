package ac.boar.anticheat.prediction.engine.impl;

import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.engine.base.PredictionEngine;
import ac.boar.anticheat.util.math.Vec3f;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.geysermc.geyser.level.block.Fluid;

public class PredictionEngineWater extends PredictionEngine {
    public PredictionEngineWater(BoarPlayer player) {
        super(player);
    }

    @Override
    protected Vec3f travel(Vec3f vec3f) {
        return this.updateVelocity(vec3f, 0.02F);
    }

    @Override
    public Vec3f applyEndOfTick(Vec3f lv) {
        float f = player.sprinting ? 0.9F : 0.8F;
        lv = lv.multiply(f, 0.8F, f);
        return this.applyFluidMovingSpeed(player.getEffectiveGravity(lv), lv);
    }

    @Override
    protected Vec3f jump(Vec3f vec3f) {
        return vec3f.add(0, 0.04F, 0);
    }

    @Override
    protected boolean shouldJump() {
        float g = player.fluidHeight.getOrDefault(Fluid.WATER, 0F);
        boolean bl = (player.touchingWater && g > 0.0) && !(player.onGround && !(g > (player.dimensions.eyeHeight() < 0.4 ? 0.0 : 0.4)));
        return bl && player.getInputData().contains(PlayerAuthInputData.WANT_UP);
    }

    private Vec3f applyFluidMovingSpeed(float gravity, Vec3f motion) {
        if (gravity != 0.0 && !player.swimming) {
            return new Vec3f(motion.x, motion.y - (gravity / 16.0F), motion.z);
        }

        return motion;
    }
}
