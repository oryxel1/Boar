package ac.boar.anticheat.prediction.engine.impl;

import ac.boar.anticheat.data.FluidState;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.engine.base.PredictionEngine;
import ac.boar.anticheat.util.math.Vec3;
import ac.boar.util.MathUtil;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.geysermc.geyser.level.block.Fluid;

public class PredictionEngineWater extends PredictionEngine {
    public PredictionEngineWater(BoarPlayer player) {
        super(player);
    }

    @Override
    protected Vec3 travel(Vec3 vec3) {
        if (player.getInputData().contains(PlayerAuthInputData.WANT_DOWN)) {
            vec3 = vec3.add(0, -0.04F, 0);
        }

        if (player.swimming) {
            float d = MathUtil.getRotationVector(player.pitch, player.yaw).y;
            float e = d < -0.2 ? 0.085F : 0.06F;
            final FluidState state = player.compensatedWorld.getFluidState(player.position.add(0, 1.0F - 0.1F, 0).toVector3i());
            if (d <= 0.0 || player.getInputData().contains(PlayerAuthInputData.WANT_UP) || state.fluid() != Fluid.EMPTY) {
                vec3 = vec3.add(0, (d - vec3.y) * e, 0);
            }
        }

        return this.updateVelocity(vec3, 0.02F);
    }

    @Override
    public void finalizeMovement() {
        final float y = player.velocity.y;

        float f = (player.sprinting || player.swimming) ? 0.9F : 0.8F;
        player.velocity = player.velocity.multiply(f, 0.8F, f);
        player.velocity = this.applyFluidMovingSpeed(player.getEffectiveGravity(), player.velocity);

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
        float g = player.fluidHeight.getOrDefault(Fluid.WATER, 0F);
        boolean bl = (player.touchingWater && g > 0.0) && !(player.groundCollision && !(g > player.getSwimHeight()));
        return bl && player.getInputData().contains(PlayerAuthInputData.WANT_UP);
    }

    private Vec3 applyFluidMovingSpeed(float gravity, Vec3 motion) {
        if (gravity != 0.0 && !player.swimming) {
            return new Vec3(motion.x, motion.y - (gravity / 16.0F), motion.z);
        }

        return motion;
    }
}
