package ac.boar.anticheat.prediction.engine.base;

import ac.boar.anticheat.data.VelocityData;
import ac.boar.anticheat.util.MathUtil;
import lombok.RequiredArgsConstructor;

import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.math.Vec3;

import ac.boar.anticheat.prediction.engine.data.Vector;
import ac.boar.anticheat.prediction.engine.data.VectorType;
import org.geysermc.geyser.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public abstract class PredictionEngine {
    protected final BoarPlayer player;

    protected abstract Vec3 travel(Vec3 vec3);
    protected abstract Vec3 jump(Vec3 vec3);
    protected abstract boolean shouldJump();

    public void finalizeMovement() {
    }

    public final List<Vector> gatherAllPossibilities() {
        List<Vector> vectors = new ArrayList<>();
//        final VelocityData supposedVelocity = player.getSupposedVelocity();
//        if (supposedVelocity == null) {
//            vectors.add(new Vector(player.velocity, VectorType.NORMAL));
//        } else {
//            vectors.add(new Vector(supposedVelocity.velocity(), VectorType.VELOCITY, supposedVelocity.transactionId()));
//        }

        addVelocityToPossibilities(vectors);

        applyJumpingToPossibilities(vectors);
        applyTravelToPossibilities(vectors);
        return vectors;
    }

    protected void applyTravelToPossibilities(List<Vector> vectors) {
        for (final Vector vector : vectors) {
            vector.setVelocity(this.travel(vector.getVelocity()));
        }
    }

    private void applyJumpingToPossibilities(List<Vector> vectors) {
        if (!shouldJump()) {
            return;
        }

        for (Vector vector : vectors) {
            vector.setJumping(true);
            vector.setVelocity(jump(vector.getVelocity()));
        }
    }

    private void addVelocityToPossibilities(final List<Vector> vectors) {
        for (final Map.Entry<Long, VelocityData> entry : player.queuedVelocities.entrySet()) {
            if (entry.getKey() - 1 > player.receivedStackId.get()) {
                continue;
            }

            final Vector vector = new Vector(entry.getValue().velocity(), VectorType.VELOCITY, entry.getKey());
            vectors.add(vector);
        }
    }

    // Other
    protected final Vec3 updateVelocity(final Vec3 vec3, final float speed) {
        return vec3.add(MathUtil.movementInputToVelocity(player.input, speed, player.yaw));
    }

    protected final Vec3 applyClimbingSpeed(final Vec3 motion) {
        if (player.onClimbable()) {
            float g = Math.max(motion.y, -0.2F);
            if (g < 0.0 && !player.compensatedWorld.getBlockState(player.position.toVector3i()).is(Blocks.SCAFFOLDING) && player.sneaking) {
                g = 0;
            }

            return new Vec3(motion.x, g, motion.z);
        }

        return motion;
    }
}
