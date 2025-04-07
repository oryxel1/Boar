package ac.boar.anticheat.prediction.engine.base;

import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.MathUtil;
import ac.boar.anticheat.util.block.specific.PowderSnowBlock;
import ac.boar.anticheat.util.math.Vec3;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.geysermc.geyser.level.block.Blocks;

@RequiredArgsConstructor
public abstract class PredictionEngine {
    protected final BoarPlayer player;

    public abstract Vec3 travel(Vec3 vec3);
    public abstract void finalizeMovement();

    protected final Vec3 moveRelative(Vec3 delta, float f) {
        Vec3 vec3 = delta.add(MathUtil.getInputVector(player.input, f, player.yaw));;

        final boolean collidedOrJumping = player.horizontalCollision ||
                player.getInputData().contains(PlayerAuthInputData.START_JUMPING) || player.getInputData().contains(PlayerAuthInputData.JUMPING);
        if (collidedOrJumping && (player.onClimbable() || player.getInBlockState().is(Blocks.POWDER_SNOW) && PowderSnowBlock.canEntityWalkOnPowderSnow(player))) {
            vec3.y = 0.2F;
        }

        return this.applyClimbingSpeed(vec3);
    }

    protected final Vec3 applyClimbingSpeed(final Vec3 motion) {
        if (player.onClimbable()) {
            float g = Math.max(motion.y, -0.2F);
            if (g < 0.0 && !player.compensatedWorld.getBlockState(player.position.toVector3i(), 0).getState().is(Blocks.SCAFFOLDING) && player.sneaking) {
                g = 0;
            }

            return new Vec3(motion.x, g, motion.z);
        }

        return motion;
    }
}