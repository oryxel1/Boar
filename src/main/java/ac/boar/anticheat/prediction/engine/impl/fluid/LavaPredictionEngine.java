package ac.boar.anticheat.prediction.engine.impl.fluid;

import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.engine.base.PredictionEngine;
import ac.boar.anticheat.util.math.Vec3;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.level.block.Fluid;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;

public class LavaPredictionEngine extends PredictionEngine {
    public LavaPredictionEngine(BoarPlayer player) {
        super(player);
    }

    @Override
    public Vec3 travel(Vec3 vec3) {
        return this.moveRelative(vec3, 0.02F);
    }

    @Override
    public void finalizeMovement() {
        float gravity = player.getEffectiveGravity();
        if (player.fluidHeight.getOrDefault(Fluid.LAVA, 0F) <= player.getFluidJumpThreshold()) {
            player.velocity = player.velocity.multiply(0.5F, 0.8F, 0.5F);
            player.velocity = this.getFluidFallingAdjustedMovement(gravity, player.velocity);
        } else {
            player.velocity = player.velocity.multiply(0.5F);
        }

        if (gravity != 0.0) {
            player.velocity = player.velocity.add(0, -gravity / 4.0F, 0);
        }
    }

    private Vec3 getFluidFallingAdjustedMovement(float gravity, Vec3 motion) {
        if (player.hasEffect(Effect.LEVITATION)) {
            float y = motion.y + (((player.getEffect(Effect.LEVITATION).getAmplifier() + 1) * 0.05F) - motion.y) * 0.2F;
            return new Vec3(motion.x, y, motion.z);
        }

        if (gravity != 0.0) {
            return new Vec3(motion.x, motion.y - (gravity / 16.0F), motion.z);
        }

        return motion;
    }
}