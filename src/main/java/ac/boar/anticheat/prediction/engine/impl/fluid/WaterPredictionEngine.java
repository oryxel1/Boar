package ac.boar.anticheat.prediction.engine.impl.fluid;

import ac.boar.anticheat.data.FluidState;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.engine.base.PredictionEngine;
import ac.boar.anticheat.util.MathUtil;
import ac.boar.anticheat.util.math.Vec3;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.geysermc.geyser.level.block.Fluid;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;

public class WaterPredictionEngine extends PredictionEngine {
    public WaterPredictionEngine(BoarPlayer player) {
        super(player);
    }

    @Override
    public Vec3 travel(Vec3 vec3) {
//        if (player.swimming) {
//            float d = MathUtil.getRotationVector(player.pitch, player.yaw).y;
//            float e = d < -0.2 ? 0.085F : 0.06F;
//            final FluidState state = player.compensatedWorld.getFluidState(player.position.add(0, 1.0F - 0.1F, 0).toVector3i());
//            if (d <= 0.0 || player.getInputData().contains(PlayerAuthInputData.WANT_UP) || state.fluid() != Fluid.EMPTY) {
//                vec3 = vec3.add(0, (d - vec3.y) * e, 0);
//            }
//        }

        return this.moveRelative(vec3, 0.02F);
    }

    @Override
    public void finalizeMovement() {
        float f = (player.sprinting || player.swimming) ? 0.9F : 0.8F;
        player.velocity = player.velocity.multiply(f, 0.8F, f);
        player.velocity = this.applyFluidMovingSpeed(player.getEffectiveGravity(), player.velocity);
    }

    private Vec3 applyFluidMovingSpeed(float gravity, Vec3 motion) {
        if (player.hasEffect(Effect.LEVITATION)) {
            float y = motion.y + (((player.getEffect(Effect.LEVITATION).getAmplifier() + 1) * 0.05F) - motion.y) * 0.2F;
            return new Vec3(motion.x, y, motion.z);
        }

        if (gravity != 0.0 && !player.swimming) {
            return new Vec3(motion.x, motion.y - (gravity / 16.0F), motion.z);
        }

        return motion;
    }
}