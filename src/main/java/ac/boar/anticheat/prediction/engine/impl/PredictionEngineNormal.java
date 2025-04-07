package ac.boar.anticheat.prediction.engine.impl;

import ac.boar.anticheat.data.StatusEffect;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.engine.base.PredictionEngine;
import ac.boar.anticheat.util.block.BlockUtil;
import ac.boar.anticheat.util.block.specific.PowderSnowBlock;
import ac.boar.anticheat.util.math.Vec3;
import ac.boar.anticheat.util.MathUtil;
import org.cloudburstmc.math.TrigMath;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;

public class PredictionEngineNormal extends PredictionEngine {
    private float prevSlipperiness = 0.6F;

    public PredictionEngineNormal(final BoarPlayer player) {
        super(player);
    }

    @Override
    protected Vec3 travel(Vec3 vec3) {
        final Vector3i lv = player.getBlockPosBelowThatAffectsMyMovement();
        this.prevSlipperiness = player.groundCollision ? BlockUtil.getBlockSlipperiness(player.compensatedWorld.getBlockState(lv)) : 1.0F;
        return this.applyMovementInput(vec3, this.prevSlipperiness);
    }

    private Vec3 applyMovementInput(Vec3 vec3, final float slipperiness) {
        vec3 = this.updateVelocity(vec3, player.getMovementSpeed(slipperiness));

        final boolean collidedOrJumping = player.horizontalCollision ||
                player.getInputData().contains(PlayerAuthInputData.START_JUMPING) || player.getInputData().contains(PlayerAuthInputData.JUMPING);
        if (collidedOrJumping && (player.onClimbable() || player.getInBlockState().is(Blocks.POWDER_SNOW) && PowderSnowBlock.canEntityWalkOnPowderSnow(player))) {
            vec3.y = 0.2F;
        }

        vec3 = this.applyClimbingSpeed(vec3);
        return vec3;
    }

    @Override
    public void finalizeMovement() {
        boolean climbable = false;
        // Seems to be the case here!
        if (player.horizontalCollision && (player.onClimbable() ||
                player.getInBlockState().is(Blocks.POWDER_SNOW) && PowderSnowBlock.canEntityWalkOnPowderSnow(player))) {
            player.velocity.y = 0.2F;
            climbable = true;
        }

        final StatusEffect effect = player.getEffect(Effect.LEVITATION);
        if (!climbable) {
            if (effect != null) {
                player.velocity.y += (0.05f * (effect.getAmplifier() + 1) - player.velocity.y) * 0.2f;
            } else if (player.compensatedWorld.isChunkLoaded((int) player.position.x, (int) player.position.z)) {
                player.velocity.y -= player.getEffectiveGravity();
            } else {
                // Seems to be 0 all the times, not -0.1 depends on your y, or well I don't know?
                player.velocity.y = 0;
            }

            player.velocity.y *= 0.98F;
        }

        final float g = this.prevSlipperiness * 0.91F;
        player.velocity = player.velocity.multiply(g, 1, g);
    }

    @Override
    protected Vec3 jump(Vec3 vec3) {
        float f = player.getJumpPower();
        if (!(f <= 1.0E-5F)) {
            vec3 = new Vec3(vec3.x, Math.max(f, vec3.y), vec3.z);
            if (player.sprinting) {
                float g = player.yaw * MathUtil.DEGREE_TO_RAG;
                vec3 = vec3.add(-TrigMath.sin(g) * 0.2f, 0, TrigMath.cos(g) * 0.2f);
            }
        }

        return vec3;
    }

    @Override
    protected boolean shouldJump() {
        return player.getInputData().contains(PlayerAuthInputData.START_JUMPING) && player.groundCollision;
    }
}
