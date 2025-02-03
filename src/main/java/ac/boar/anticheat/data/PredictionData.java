package ac.boar.anticheat.data;

import ac.boar.anticheat.prediction.engine.data.Vector;
import ac.boar.anticheat.util.math.Vec3f;

public record PredictionData(Vector vector, Vec3f beforeCollision, Vec3f afterCollision) {
}
