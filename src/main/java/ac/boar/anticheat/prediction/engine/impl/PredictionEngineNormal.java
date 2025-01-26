package ac.boar.anticheat.prediction.engine.impl;

import ac.boar.anticheat.data.StatusEffect;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.engine.base.PredictionEngine;
import ac.boar.anticheat.util.BlockUtil;
import ac.boar.anticheat.util.math.Vec3f;
import org.cloudburstmc.math.TrigMath;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;

public class PredictionEngineNormal extends PredictionEngine {
    public PredictionEngineNormal(final BoarPlayer player) {
        super(player);
    }

    @Override
    protected Vec3f travel(Vec3f vec3f) {
        final Vector3i lv = player.getVelocityAffectingPos();
        float f = player.onGround ? BlockUtil.getBlockSlipperiness(player.compensatedWorld.getBlockState(lv)) : 1.0F;
        return this.applyMovementInput(vec3f, f);
    }

    private Vec3f applyMovementInput(Vec3f vec3f, final float slipperiness) {
        vec3f = this.updateVelocity(vec3f, player.getMovementSpeed(slipperiness));
        // this.setVelocity(this.applyClimbingSpeed(this.getVelocity()));
        return vec3f;
    }

    @Override
    public Vec3f applyEndOfTick(Vec3f vec3f) {
        final Vector3i lv = player.getVelocityAffectingPos();
        float f = player.wasGround ? BlockUtil.getBlockSlipperiness(player.compensatedWorld.getBlockState(lv)) : 1.0F;
        float d = vec3f.y;
        final StatusEffect effect = player.statusEffects.get(Effect.LEVITATION);
        if (effect != null) {
            d += (0.05f * (effect.getAmplifier() + 1) - vec3f.y) * 0.2f;
        } else if (player.compensatedWorld.isChunkLoaded((int) player.x, (int) player.z)) {
            d -= player.getEffectiveGravity(vec3f);
        } else {
            // Seems to be 0 all the times, not -0.1 depends on your y, or well I don't know?
            d = 0;
        }

        float g = f * 0.91F;
        return new Vec3f(vec3f.x * g, d * 0.98F, vec3f.z * g);
    }

    @Override
    protected Vec3f jump(Vec3f vec3f) {
        float f = player.getJumpVelocity();
        if (!(f <= 1.0E-5F)) {
            vec3f = new Vec3f(vec3f.x, Math.max(f, vec3f.y), vec3f.z);
            if (player.sprinting) {
                float g = player.yaw * (float) (TrigMath.PI / 180.0);
                vec3f = vec3f.add(-TrigMath.sin(g) * 0.2f, 0, TrigMath.cos(g) * 0.2f);
            }
        }

        return vec3f;
    }

    @Override
    protected boolean shouldJump() {
        return player.getInputData().contains(PlayerAuthInputData.START_JUMPING) && player.onGround;
    }
}
