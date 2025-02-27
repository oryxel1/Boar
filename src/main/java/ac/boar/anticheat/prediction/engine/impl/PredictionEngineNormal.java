package ac.boar.anticheat.prediction.engine.impl;

import ac.boar.anticheat.data.StatusEffect;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.engine.base.PredictionEngine;
import ac.boar.anticheat.util.BlockUtil;
import ac.boar.anticheat.util.math.Vec3;
import org.cloudburstmc.math.TrigMath;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;

public class PredictionEngineNormal extends PredictionEngine {
    public PredictionEngineNormal(final BoarPlayer player) {
        super(player);
    }

    @Override
    protected Vec3 travel(Vec3 vec3) {
        final Vector3i lv = player.getVelocityAffectingPos();
        float f = player.onGround ? BlockUtil.getBlockSlipperiness(player.compensatedWorld.getBlockState(lv)) : 1.0F;
        return this.applyMovementInput(vec3, f);
    }

    private Vec3 applyMovementInput(Vec3 vec3, final float slipperiness) {
        vec3 = this.updateVelocity(vec3, player.getMovementSpeed(slipperiness));
        vec3 = this.applyClimbingSpeed(vec3);
        return vec3;
    }

    @Override
    public Vec3 applyEndOfTick(Vec3 lv) {
        final Vector3i lv2 = player.getVelocityAffectingPos();
        float f = player.wasGround ? BlockUtil.getBlockSlipperiness(player.compensatedWorld.getBlockState(lv2)) : 1.0F;
        float d = lv.y;
        final StatusEffect effect = player.statusEffects.get(Effect.LEVITATION);
        if (effect != null) {
            d += (0.05f * (effect.getAmplifier() + 1) - lv.y) * 0.2f;
        } else if (player.compensatedWorld.isChunkLoaded((int) player.position.x, (int) player.position.z)) {
            d -= player.getEffectiveGravity(lv);
        } else {
            // Seems to be 0 all the times, not -0.1 depends on your y, or well I don't know?
            d = 0;
        }

        final float g = f * 0.91F;
        lv = new Vec3(lv.x * g, d * 0.98F, lv.z * g);

        return lv;
    }

    @Override
    protected Vec3 jump(Vec3 vec3) {
        float f = player.getJumpPower();
        if (!(f <= 1.0E-5F)) {
            vec3 = new Vec3(vec3.x, Math.max(f, vec3.y), vec3.z);
            if (player.sprinting) {
                float g = player.yaw * (float) (TrigMath.PI / 180.0);
                vec3 = vec3.add(-TrigMath.sin(g) * 0.2f, 0, TrigMath.cos(g) * 0.2f);
            }
        }

        return vec3;
    }

    @Override
    protected boolean shouldJump() {
        return player.getInputData().contains(PlayerAuthInputData.START_JUMPING) && player.onGround;
    }
}
