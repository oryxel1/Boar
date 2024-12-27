package ac.boar.anticheat.prediction.engine.base;

import ac.boar.util.MathUtil;
import lombok.RequiredArgsConstructor;

import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.math.Vec3f;

import ac.boar.anticheat.prediction.engine.data.Vector;
import ac.boar.anticheat.prediction.engine.data.VectorType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public abstract class PredictionEngine {
    protected final BoarPlayer player;

    protected abstract Vec3f travel(Vec3f vec3f);
    protected abstract Vec3f applyEndOfTick(Vec3f vec3f);
    protected abstract Vec3f jump(Vec3f vec3f);
    protected abstract boolean shouldJump();

    public final List<Vector> gatherAllPossibilities() {
        List<Vector> vectors = new ArrayList<>();
        vectors.add(new Vector(player.eotVelocity, VectorType.NORMAL));
        addVelocityToPossibilities(vectors);
        addJumpingToPossibilities(vectors);

        applyTravelToPossibilities(vectors);
        return vectors;
    }

    protected void applyTravelToPossibilities(List<Vector> vectors) {
    }

    private void addJumpingToPossibilities(List<Vector> vectors) {
        if (!shouldJump()) {
            return;
        }

        for (Vector vector : vectors) {
            vector.setVelocity(jump(vector.getVelocity()));
            vector.setJumping(true);
        }
    }

    private void addVelocityToPossibilities(final List<Vector> vectors) {
        for (final Map.Entry<Long, Vec3f> entry : player.queuedVelocities.entrySet()) {
            final Vector vector = new Vector(entry.getValue(), VectorType.VELOCITY, entry.getKey());
            vectors.add(vector);
        }
    }

    // Other
    protected final Vec3f updateVelocity(final float speed) {
        return MathUtil.movementInputToVelocity(player.movementInput, speed, player.yaw);
    }
}
