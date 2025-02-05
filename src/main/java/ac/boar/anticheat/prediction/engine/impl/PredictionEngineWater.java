package ac.boar.anticheat.prediction.engine.impl;

import ac.boar.anticheat.data.FluidState;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.engine.base.PredictionEngine;
import ac.boar.anticheat.util.math.Vec3f;
import ac.boar.util.MathUtil;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.geysermc.geyser.level.block.Fluid;

public class PredictionEngineWater extends PredictionEngine {
    public PredictionEngineWater(BoarPlayer player) {
        super(player);
    }

    @Override
    protected Vec3f travel(Vec3f vec3f) {
        if (player.getInputData().contains(PlayerAuthInputData.WANT_DOWN)) {
            vec3f = vec3f.add(0, -0.04F, 0);
        }

        if (player.swimming) {
            float d = MathUtil.getRotationVector(player.pitch, player.yaw).y;
            float e = d < -0.2 ? 0.085F : 0.06F;
            final FluidState state = player.compensatedWorld.getFluidState(GenericMath.floor(player.prevX), GenericMath.floor(player.prevY + 1.0 - 0.1), GenericMath.floor(player.prevZ));
            if (d <= 0.0 || player.getInputData().contains(PlayerAuthInputData.WANT_UP) || state.fluid() != Fluid.EMPTY) {
                vec3f = vec3f.add(0, (d - vec3f.y) * e, 0);
            }
        }

        return this.updateVelocity(vec3f, 0.02F);
    }

    @Override
    public Vec3f applyEndOfTick(Vec3f lv) {
        final float y = lv.y;

        float f = (player.sprinting || player.swimming) ? 0.9F : 0.8F;
        lv = lv.multiply(f, 0.8F, f);
        lv = this.applyFluidMovingSpeed(player.getEffectiveGravity(lv), lv);

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
