package ac.boar.anticheat.prediction.engine.base;

import ac.boar.anticheat.data.VelocityData;
import ac.boar.util.MathUtil;
import lombok.RequiredArgsConstructor;

import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.math.Vec3f;

import ac.boar.anticheat.prediction.engine.data.Vector;
import ac.boar.anticheat.prediction.engine.data.VectorType;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.geyser.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public abstract class PredictionEngine {
    protected final BoarPlayer player;

    protected abstract Vec3f travel(Vec3f vec3f);
    protected abstract Vec3f jump(Vec3f vec3f);
    protected abstract boolean shouldJump();

    public Vec3f applyEndOfTick(Vec3f vec3f) {
        return vec3f;
    }

    public final List<Vector> gatherAllPossibilities() {
        List<Vector> vectors = new ArrayList<>();
        vectors.add(new Vector(player.eotVelocity, VectorType.NORMAL));
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
            final Vector vector = new Vector(entry.getValue().velocity(), VectorType.VELOCITY, entry.getKey());
            vectors.add(vector);
        }
    }

    // Other
    protected final Vec3f updateVelocity(final Vec3f vec3f, final float speed) {
        return vec3f.add(MathUtil.movementInputToVelocity(player.movementInput, speed, player.yaw));
    }

    protected final Vec3f applyClimbingSpeed(final Vec3f motion) {
        if (player.isClimbing(true)) {
            float g = Math.max(motion.y, -0.2F);
            if (g < 0.0 && !player.compensatedWorld.getBlockState(Vector3i.from(player.x, player.y, player.z)).is(Blocks.SCAFFOLDING) && player.sneaking) {
                g = 0;
            }

            return new Vec3f(motion.x, g, motion.z);
        }

        return motion;
    }
}
