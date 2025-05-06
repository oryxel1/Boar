package ac.boar.anticheat.prediction.engine.impl.fluid;

import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.engine.base.PredictionEngine;
import ac.boar.anticheat.util.math.Vec3;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;

public class WaterPredictionEngine extends PredictionEngine {
    public WaterPredictionEngine(BoarPlayer player) {
        super(player);
    }

    @Override
    public Vec3 travel(Vec3 vec3) {
        return this.moveRelative(vec3, 0.02F);
    }

    @Override
    public void finalizeMovement() {
        // There is a bug on uhh version below 1.21.80 that makes player able to move faster in water without swimming.
        boolean fasterTickEnd = GameProtocol.is1_21_80orHigher(player.getSession()) ? player.getFlagTracker().has(EntityFlag.SWIMMING) : player.getFlagTracker().has(EntityFlag.SPRINTING);

        float f = fasterTickEnd ? 0.9F : 0.8F;
        player.velocity = player.velocity.multiply(f, 0.8F, f);
        player.velocity = this.getFluidFallingAdjustedMovement(player.getEffectiveGravity(), player.velocity);
    }

    private Vec3 getFluidFallingAdjustedMovement(float gravity, Vec3 motion) {
        if (player.hasEffect(Effect.LEVITATION)) {
            float y = motion.y + (((player.getEffect(Effect.LEVITATION).getAmplifier() + 1) * 0.05F) - motion.y) * 0.2F;
            return new Vec3(motion.x, y, motion.z);
        }

        if (gravity != 0.0 && !player.getFlagTracker().has(EntityFlag.SWIMMING)) {
            return new Vec3(motion.x, motion.y - (gravity / 16.0F), motion.z);
        }

        return motion;
    }
}