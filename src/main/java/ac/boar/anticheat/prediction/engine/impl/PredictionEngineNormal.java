package ac.boar.anticheat.prediction.engine.impl;

import ac.boar.anticheat.data.StatusEffect;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.engine.base.PredictionEngine;
import ac.boar.anticheat.util.BlockUtil;
import ac.boar.anticheat.util.math.Vec3f;
import org.cloudburstmc.math.TrigMath;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.geysermc.geyser.level.block.Blocks;
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
        vec3f = this.applyClimbingSpeed(vec3f);
        return vec3f;
    }

    @Override
    public Vec3f applyEndOfTick(Vec3f lv) {
        final Vector3i lv2 = player.getVelocityAffectingPos();
        float f = player.wasGround ? BlockUtil.getBlockSlipperiness(player.compensatedWorld.getBlockState(lv2)) : 1.0F;
        float d = lv.y;
        final StatusEffect effect = player.statusEffects.get(Effect.LEVITATION);
        if (effect != null) {
            d += (0.05f * (effect.getAmplifier() + 1) - lv.y) * 0.2f;
        } else if (player.compensatedWorld.isChunkLoaded((int) player.x, (int) player.z)) {
            d -= player.getEffectiveGravity(lv);
        } else {
            // Seems to be 0 all the times, not -0.1 depends on your y, or well I don't know?
            d = 0;
        }

        final float g = f * 0.91F;
        lv = new Vec3f(lv.x * g, d * 0.98F, lv.z * g);

        // This got applied before instead of after like on Java Edition - or at-least seems to be the case.
        if ((player.horizontalCollision || player.getInputData().contains(PlayerAuthInputData.JUMPING)) &&
                (player.isClimbing(false) /* ||
                        player.compensatedWorld.getBlockState(Vector3i.from(player.x, player.y, player.z)).is(Blocks.POWDER_SNOW) &&
                                PowderSnowBlock.canWalkOnPowderSnow(this) */)) {
            lv = new Vec3f(lv.x, 0.2F, lv.z);
        }

        return lv;
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
